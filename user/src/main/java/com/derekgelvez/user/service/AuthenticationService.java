package com.derekgelvez.user.service;


import com.derekgelvez.user.model.User;
import com.derekgelvez.user.model.dto.LoginUserDto;
import com.derekgelvez.user.model.dto.RegisterUserDto;
import com.derekgelvez.user.model.dto.VerifyUserDto;
import com.derekgelvez.user.model.dto.ForgotPasswordDto;
import com.derekgelvez.user.model.dto.ResetPasswordDto;
import com.derekgelvez.user.exception.ApiException;
import com.derekgelvez.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account lifecycle rules: sign-up, email verification, login and password reset.
 *
 * <p>Three habits run through this class: emails are normalized (trimmed, lower-cased) before
 * every lookup; where possible, responses don't reveal whether an email is registered; and secrets
 * are compared in constant time with {@link MessageDigest#isEqual} so timing can't leak them.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    /** Wrong codes allowed before the code is thrown away and a new one must be requested. */
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Failed verification attempts per email (in memory; resets on restart). */
    private final Map<String, Integer> failedVerifyAttempts = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            LoginAttemptService loginAttemptService
    ){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Creates an unverified account and emails its verification code (valid 15 minutes).
     * The byte-length check backs up the DTO's {@code @Size(max = 72)}, which counts characters:
     * a password with multi-byte characters can pass that and still exceed BCrypt's 72-byte limit.
     * If the email cannot be sent, the account is still saved and a 503 is returned; the user can
     * then get a new code through {@code /auth/resend}.
     */
    public User signup(RegisterUserDto input){
        String email = normalizeEmail(input.getEmail());
        if (input.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Password must be at most 72 bytes");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists");
        }
        if (userRepository.existsByUsername(input.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "This username is already taken");
        }
        User user = new User(input.getUsername(), email, passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        User savedUser = userRepository.save(user);
        sendVerificationEmail(savedUser);
        return savedUser;
    }

    /**
     * Checks the email and password. Unknown email and wrong password give the same error, so the
     * response never reveals which emails are registered. "Not verified" is only reported once the
     * password is correct.
     */
    public User authenticate(LoginUserDto input) {
        String email = normalizeEmail(input.getEmail());
        if (loginAttemptService.isBlocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Too many failed login attempts. Try again later");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            input.getPassword()
                    )
            );
            loginAttemptService.recordSuccess(email);
            return (User) authentication.getPrincipal();
        } catch (DisabledException e) {
            // The password was correct, so this isn't a guessing attempt: clear the failure count.
            loginAttemptService.recordSuccess(email);
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIED",
                    "Account not verified. Please verify your account");
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
    }

    /**
     * Activates the account when the submitted code matches and hasn't expired. An unknown email,
     * an already-verified account and a wrong code all produce the same error. After
     * {@link #MAX_VERIFY_ATTEMPTS} wrong codes the code is discarded and a new one must be requested.
     */
    public void verifyUser(VerifyUserDto input){
        String email = normalizeEmail(input.getEmail());
        User user = userRepository.findByEmail(email).orElseThrow(AuthenticationService::invalidCode);
        if (user.isEnabled() || user.getVerificationCode() == null || user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalidCode();
        }

        boolean matches = MessageDigest.isEqual(
                user.getVerificationCode().getBytes(StandardCharsets.UTF_8),
                input.getVerificationCode().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            int attempts = failedVerifyAttempts.merge(email, 1, Integer::sum);
            if (attempts >= MAX_VERIFY_ATTEMPTS) {
                // Too many guesses: throw the code away so it can't be brute-forced
                failedVerifyAttempts.remove(email);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
                throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_ATTEMPTS",
                        "Too many incorrect codes. Request a new verification code");
            }
            throw invalidCode();
        }

        failedVerifyAttempts.remove(email);
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    /**
     * Sends a new code if the email belongs to an unverified account. Does nothing otherwise, and the
     * caller answers the same way in every case, so this can't be used to discover accounts.
     */
    public void resendVerificationCode(String email){
        userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> !user.isEnabled())
                .ifPresent(user -> {
                    failedVerifyAttempts.remove(user.getEmail());
                    user.setVerificationCode(generateVerificationCode());
                    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
                    userRepository.save(user);
                    sendVerificationEmail(user);
                });
    }

    /**
     * Starts a password reset: generates a random token, stores only its SHA-256 hash (so a leaked
     * database can't be used to reset passwords) and emails the raw token, valid for 15 minutes.
     * Unknown emails are silently ignored so the response can't reveal which accounts exist.
     */
    public void requestPasswordReset(ForgotPasswordDto input) {
        userRepository.findByEmail(normalizeEmail(input.getEmail())).ifPresent(user -> {
            String rawToken = generateResetToken();
            user.setPasswordResetTokenHash(hashToken(rawToken));
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            try {
                emailService.sendPasswordResetEmail(user.getEmail(),
                        "Password reset", buildPasswordResetMessage(rawToken));
            } catch (MessagingException | MailException e) {
                // Keep the response generic so delivery failures cannot enumerate accounts.
                log.error("Password reset email delivery failed for {}", user.getEmail(), e);
            }
        });
    }

    /**
     * Completes a password reset. The token is single-use (cleared once the password changes), and
     * any failed-login lockout on the account is lifted. JWTs issued before the reset are not
     * revoked; they stay valid until they expire.
     */
    public void resetPassword(ResetPasswordDto input) {
        User user = userRepository.findByEmail(normalizeEmail(input.getEmail()))
                .orElseThrow(AuthenticationService::invalidResetToken);
        String storedHash = user.getPasswordResetTokenHash();
        LocalDateTime expiresAt = user.getPasswordResetExpiresAt();
        if (storedHash == null || expiresAt == null || expiresAt.isBefore(LocalDateTime.now())
                || !MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                hashToken(input.getToken()).getBytes(StandardCharsets.UTF_8))) {
            throw invalidResetToken();
        }
        if (input.getNewPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Password must be at most 72 bytes");
        }
        user.setPassword(passwordEncoder.encode(input.getNewPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        loginAttemptService.recordSuccess(user.getEmail());
    }

    /** Emails the account's current code. A delivery failure becomes a 503 so the client can ask the user to retry. */
    public void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to OurPlan!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException | MailException e) {
            log.error("Verification email delivery failed for {}", user.getEmail(), e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "VERIFICATION_EMAIL_UNAVAILABLE",
                    "Verification email could not be sent");
        }
    }

    private String buildPasswordResetMessage(String rawToken) {
        return "<html><body><p>Use this password reset token in the app:</p><p><b>"
                + rawToken + "</b></p><p>This token expires in 15 minutes.</p></body></html>";
    }

    /** 256 random bits, Base64 URL-safe so the token can be pasted or placed in a link. */
    private static String generateResetToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** A fast hash is enough here (no BCrypt) because the token is long and random, not user-chosen. */
    private static String hashToken(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** Always six digits (100000-999999), matching the pattern VerifyUserDto accepts. */
    private String generateVerificationCode() {
        int code = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "Invalid verification code");
    }

    private static ApiException invalidResetToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Invalid or expired password reset token");
    }
}
