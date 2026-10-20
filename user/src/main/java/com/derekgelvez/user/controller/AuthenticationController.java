package com.derekgelvez.user.controller;


import com.derekgelvez.user.model.User;
import com.derekgelvez.user.model.dto.LoginUserDto;
import com.derekgelvez.user.model.dto.RegisterUserDto;
import com.derekgelvez.user.model.dto.VerifyUserDto;
import com.derekgelvez.user.model.dto.ForgotPasswordDto;
import com.derekgelvez.user.model.dto.ResetPasswordDto;
import com.derekgelvez.user.responses.LoginResponse;
import com.derekgelvez.user.responses.MessageResponse;
import com.derekgelvez.user.responses.UserResponse;
import com.derekgelvez.user.service.AuthenticationService;
import com.derekgelvez.user.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public account endpoints: sign-up, email verification, login (returns a JWT) and password reset.
 * The rules live in {@link AuthenticationService}; this class only maps HTTP to service calls.
 * Failures are turned into JSON error bodies by {@code UserExceptionHandler}.
 */
@RequestMapping("/auth")
@RestController
public class AuthenticationController {

    private final JwtService jwtService;

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService){
        this.jwtService =  jwtService;
        this.authenticationService = authenticationService;
    }

    /** Creates an unverified account and emails it a 6-digit code. Login is refused until it is verified. */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserDto registerUserDto){
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(UserResponse.from(registeredUser));
    }

    /** Exchanges email + password for a signed JWT. {@code expiresIn} is in milliseconds. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginUserDto loginUserDto){
        User authenticatedUser = authenticationService.authenticate((loginUserDto));
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtService.getExpirationTime(), jwtToken);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<MessageResponse> verifyUser(@Valid @RequestBody VerifyUserDto verifyUserDto){
        // Errors (wrong/expired code, too many attempts) are turned into JSON by UserExceptionHandler
        authenticationService.verifyUser(verifyUserDto);
        return ResponseEntity.ok(new MessageResponse("Account verified successfully"));
    }

    /** Issues a fresh verification code. Takes the email as a query parameter, not a JSON body. */
    @PostMapping("/resend")
    public ResponseEntity<MessageResponse> resentVerificationCode(@RequestParam @NotBlank String email) {
        authenticationService.resendVerificationCode(email);
        // Same answer whether or not the account exists, so this can't be used to discover emails
        return ResponseEntity.ok(new MessageResponse("If an unverified account exists for this email, a new code has been sent"));
    }

    /** Emails a reset token valid for 15 minutes. Same response whether or not the account exists. */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordDto input) {
        authenticationService.requestPasswordReset(input);
        return ResponseEntity.ok(new MessageResponse(
                "If an account exists for this email, a password reset message has been sent"));
    }

    /** Sets a new password when the emailed token is valid and unexpired; the token can't be reused. */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordDto input) {
        authenticationService.resetPassword(input);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }
}
