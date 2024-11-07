package com.example.demo.dto.user;

import com.example.demo.validators.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeDTO(
        @NotBlank(message = "Code cannot be blank")
        String code,

        @NotBlank(message = "New password cannot be blank")
        @ValidPassword
        String newPassword
) {}
