package com.arohan.user;

import com.arohan.lifearea.StarterProvisioningService;
import com.arohan.shared.ApiException;
import com.arohan.user.UserDtos.UpdatePreferencesRequest;
import com.arohan.user.UserDtos.UserResponse;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final StarterProvisioningService starterProvisioning;

    public UserService(UserRepository users, StarterProvisioningService starterProvisioning) {
        this.users = users;
        this.starterProvisioning = starterProvisioning;
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return UserResponse.from(require(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdatePreferencesRequest request) {
        try {
            ZoneId.of(request.timeZone());
        } catch (DateTimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid IANA time zone.");
        }
        User user = require(userId);
        boolean completingOnboarding =
            request.onboardingComplete() && !user.isOnboardingComplete();
        user.updatePreferences(request.displayName().trim(), request.timeZone(), request.locale(),
            request.themePreference(), request.weekStart(), request.dateFormat(),
            request.timeFormat(), request.reducedMotion(), request.enhancedContrast(),
            request.onboardingComplete(),
            request.starterTemplateKeys());
        if (completingOnboarding) {
            starterProvisioning.provision(
                userId,
                request.starterTemplateKeys(),
                LocalDate.now(ZoneId.of(request.timeZone()))
            );
        }
        return UserResponse.from(user);
    }

    private User require(UUID id) {
        return users.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User was not found."));
    }
}
