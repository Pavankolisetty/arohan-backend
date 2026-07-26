package com.arohan.user;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User extends AuditableEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(nullable = false, length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false, length = 16)
    private ThemePreference themePreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_start", nullable = false, length = 16)
    private WeekStart weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_format", nullable = false, length = 16)
    private DateFormatPreference dateFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_format", nullable = false, length = 24)
    private TimeFormatPreference timeFormat;

    @Column(name = "reduced_motion", nullable = false)
    private boolean reducedMotion;

    @Column(name = "enhanced_contrast", nullable = false)
    private boolean enhancedContrast;

    @Column(name = "onboarding_complete", nullable = false)
    private boolean onboardingComplete;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_starter_template", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "template_key", nullable = false, length = 40)
    private Set<String> starterTemplateKeys = new LinkedHashSet<>();

    protected User() {}

    public User(String email, String passwordHash, String displayName) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timeZone = "UTC";
        this.locale = "en-IN";
        this.themePreference = ThemePreference.SYSTEM;
        this.weekStart = WeekStart.MONDAY;
        this.dateFormat = DateFormatPreference.AUTO;
        this.timeFormat = TimeFormatPreference.SYSTEM;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getTimeZone() { return timeZone; }
    public String getLocale() { return locale; }
    public ThemePreference getThemePreference() { return themePreference; }
    public WeekStart getWeekStart() { return weekStart; }
    public DateFormatPreference getDateFormat() { return dateFormat; }
    public TimeFormatPreference getTimeFormat() { return timeFormat; }
    public boolean isReducedMotion() { return reducedMotion; }
    public boolean isEnhancedContrast() { return enhancedContrast; }
    public boolean isOnboardingComplete() { return onboardingComplete; }
    public Set<String> getStarterTemplateKeys() { return Set.copyOf(starterTemplateKeys); }

    public void updatePreferences(String displayName, String timeZone, String locale,
                                  ThemePreference themePreference, WeekStart weekStart,
                                  DateFormatPreference dateFormat,
                                  TimeFormatPreference timeFormat,
                                  boolean reducedMotion, boolean enhancedContrast,
                                  boolean onboardingComplete, Set<String> templateKeys) {
        this.displayName = displayName;
        this.timeZone = timeZone;
        this.locale = locale;
        this.themePreference = themePreference;
        this.weekStart = weekStart;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.reducedMotion = reducedMotion;
        this.enhancedContrast = enhancedContrast;
        this.onboardingComplete = onboardingComplete;
        this.starterTemplateKeys.clear();
        this.starterTemplateKeys.addAll(templateKeys);
    }
}
