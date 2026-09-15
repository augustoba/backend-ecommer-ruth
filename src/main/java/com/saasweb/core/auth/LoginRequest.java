package com.saasweb.core.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String dni,
        @NotBlank String password
) {}
