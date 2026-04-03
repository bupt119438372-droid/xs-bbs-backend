package com.xs.bbs.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min = 4, max = 32) String username,
        @NotBlank @Size(min = 6, max = 32) String password
) {
}
