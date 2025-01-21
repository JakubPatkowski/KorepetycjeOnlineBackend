package com.example.ekorki.dto.user;

import com.example.ekorki.validators.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PasswordChangeDTO(
        @NotNull(message = "Password cannot be null")
        @NotBlank(message = "Password cannot be blank")
        @NotEmpty(message = "Password cannot be empty")
        String code,

        @NotNull
        @NotNull(message = "Password cannot be null")
        @NotBlank(message = "Password cannot be blank")
        @NotEmpty(message = "Password cannot be empty")
        @Size(min = 12, message = "Password must be at least 12 characters long")
        String newPassword
) {}
