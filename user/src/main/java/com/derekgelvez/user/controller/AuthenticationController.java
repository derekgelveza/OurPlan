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

@RequestMapping("/auth")
@RestController
public class AuthenticationController {

    private final JwtService jwtService;

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService){
        this.jwtService =  jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserDto registerUserDto){
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(UserResponse.from(registeredUser));
    }

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

    @PostMapping("/resend")
    public ResponseEntity<MessageResponse> resentVerificationCode(@RequestParam @NotBlank String email) {
        authenticationService.resendVerificationCode(email);
        // Same answer whether or not the account exists, so this can't be used to discover emails
        return ResponseEntity.ok(new MessageResponse("If an unverified account exists for this email, a new code has been sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordDto input) {
        authenticationService.requestPasswordReset(input);
        return ResponseEntity.ok(new MessageResponse(
                "If an account exists for this email, a password reset message has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordDto input) {
        authenticationService.resetPassword(input);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }
}
