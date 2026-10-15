package com.derekgelvez.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserDto {

    @NotBlank
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 72)
    private String password;
}
