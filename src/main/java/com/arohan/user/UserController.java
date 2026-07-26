package com.arohan.user;

import com.arohan.user.UserDtos.UpdatePreferencesRequest;
import com.arohan.user.UserDtos.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    UserResponse getMe(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return userService.get(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping
    UserResponse updateMe(
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdatePreferencesRequest request) {
        return userService.update(UUID.fromString(jwt.getSubject()), request);
    }
}

