package com.arohan.auth;

import com.arohan.user.UserDtos.UserResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 10, max = 72) String password
    ) {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
    ) {}
}

