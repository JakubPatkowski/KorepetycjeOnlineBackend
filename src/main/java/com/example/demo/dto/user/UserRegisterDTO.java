package com.example.demo.dto.user;

import com.example.demo.validators.ValidPassword;
import jakarta.validation.constraints.*;


public record UserRegisterDTO(

        @NotNull(message = "Name and surname cannot be null")
        @NotBlank(message = "Name and surname cannot be null")
        @NotEmpty(message = "Name and surname cannot be null")
        String fullName,
        @NotNull(message = "Email cannot be null")
        @NotBlank(message = "Email cannot be blank")
        @NotEmpty(message = "Email cannot be empty")
        @Email(message = "Enter a valid email")
        String email,
        @NotNull
        @NotNull(message = "Password cannot be null")
        @NotBlank(message = "Password cannot be blank")
        @NotEmpty(message = "Password cannot be empty")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @ValidPassword
        String password
) {

}