package com.derekgelvez.user.model.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserDto {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    /** BCrypt only uses the first 72 bytes of a password, so longer ones are rejected. */
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may only contain letters, numbers, '.', '_' and '-'")
    private String username;

}
