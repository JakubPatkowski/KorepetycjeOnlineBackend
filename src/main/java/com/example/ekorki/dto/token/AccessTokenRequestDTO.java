package com.example.ekorki.dto.token;

import jakarta.validation.constraints.NotBlank;

public record AccessTokenRequestDTO(
		@NotBlank(message = "Token cannot be blank")
		String token
) {}