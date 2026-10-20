package com.derekgelvez.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Second step of a password reset: the token from the email plus the new password. */
@Getter
@Setter
public class ResetPasswordDto {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 128)
    private String token;

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
