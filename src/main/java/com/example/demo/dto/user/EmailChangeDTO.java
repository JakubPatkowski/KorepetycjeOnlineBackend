package com.example.demo.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeDTO(
        @NotBlank(message = "Code cannot be blank")
        String code,

        @NotBlank(message = "New email cannot be blank")
        @Email(message = "Invalid email format")
        String newEmail
) {}
