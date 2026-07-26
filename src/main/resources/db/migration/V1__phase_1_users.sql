CREATE TABLE app_user (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    locale VARCHAR(16) NOT NULL DEFAULT 'en-IN',
    theme_preference VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    week_start VARCHAR(16) NOT NULL DEFAULT 'MONDAY',
    onboarding_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_theme_preference
        CHECK (theme_preference IN ('SYSTEM', 'LIGHT', 'DARK')),
    CONSTRAINT chk_week_start
        CHECK (week_start IN ('MONDAY', 'SUNDAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_starter_template (
    user_id BINARY(16) NOT NULL,
    template_key VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, template_key),
    CONSTRAINT fk_user_starter_template_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_starter_template_user
    ON user_starter_template(user_id);
