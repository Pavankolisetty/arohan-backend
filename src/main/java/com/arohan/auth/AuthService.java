package com.arohan.auth;

import com.arohan.auth.AuthDtos.AuthResponse;
import com.arohan.auth.AuthDtos.LoginRequest;
import com.arohan.auth.AuthDtos.RegisterRequest;
import com.arohan.config.JwtProperties;
import com.arohan.shared.ApiException;
import com.arohan.user.User;
import com.arohan.user.UserDtos.UserResponse;
import com.arohan.user.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       TokenService tokenService, JwtProperties jwtProperties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT,
                "An account already exists for this email. Try signing in.");
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()),
            request.displayName().trim()));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(tokenService.create(user), "Bearer",
            jwtProperties.accessTokenTtl().toSeconds(), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
    }
}

