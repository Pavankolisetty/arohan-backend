package com.arohan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String timeZone,
        String locale,
        ThemePreference themePreference,
        WeekStart weekStart,
        DateFormatPreference dateFormat,
        TimeFormatPreference timeFormat,
        boolean reducedMotion,
        boolean enhancedContrast,
        boolean onboardingComplete,
        Set<String> starterTemplateKeys,
        Instant createdAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getTimeZone(), user.getLocale(), user.getThemePreference(),
                user.getWeekStart(), user.getDateFormat(), user.getTimeFormat(),
                user.isReducedMotion(), user.isEnhancedContrast(),
                user.isOnboardingComplete(),
                user.getStarterTemplateKeys(), user.getCreatedAt());
        }
    }

    public record UpdatePreferencesRequest(
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Size(max = 64) String timeZone,
        @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String locale,
        @NotNull ThemePreference themePreference,
        @NotNull WeekStart weekStart,
        @NotNull DateFormatPreference dateFormat,
        @NotNull TimeFormatPreference timeFormat,
        boolean reducedMotion,
        boolean enhancedContrast,
        boolean onboardingComplete,
        @NotNull @Size(max = 8) Set<
            @Pattern(regexp = "^(WELLBEING|MINDFULNESS|LEARNING|FINANCIAL|RELATIONSHIPS|CREATIVE)$")
            String> starterTemplateKeys
    ) {}
}
