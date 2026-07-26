CREATE TABLE life_area (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    parent_id BINARY(16) NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NULL,
    color_hex CHAR(7) NOT NULL,
    icon_key VARCHAR(40) NOT NULL,
    background_key VARCHAR(40) NOT NULL,
    background_image_url VARCHAR(1000) NULL,
    desired_importance TINYINT NOT NULL DEFAULT 3,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    position_index INT NOT NULL DEFAULT 0,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_life_area_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_life_area_parent
        FOREIGN KEY (parent_id) REFERENCES life_area(id) ON DELETE CASCADE,
    CONSTRAINT chk_life_area_importance
        CHECK (desired_importance BETWEEN 1 AND 5),
    CONSTRAINT chk_life_area_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_life_area_color
        CHECK (color_hex REGEXP '^#[0-9A-Fa-f]{6}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_life_area_user_status_position
    ON life_area(user_id, status, position_index);
CREATE INDEX idx_life_area_parent
    ON life_area(parent_id);

CREATE TABLE growth_habit (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    life_area_id BINARY(16) NOT NULL,
    kind VARCHAR(20) NOT NULL DEFAULT 'GROWTH_HABIT',
    name VARCHAR(120) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    tracking_method VARCHAR(20) NOT NULL,
    target_value DECIMAL(19,4) NULL,
    target_unit VARCHAR(40) NULL,
    cue_note VARCHAR(300) NOT NULL,
    two_minute_starter VARCHAR(300) NOT NULL,
    preferred_time TIME NULL,
    preferred_place VARCHAR(160) NULL,
    preceding_activity VARCHAR(200) NULL,
    situation VARCHAR(200) NULL,
    fallback_plan VARCHAR(400) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    position_index INT NOT NULL DEFAULT 0,
    paused_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_growth_habit_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_growth_habit_life_area
        FOREIGN KEY (life_area_id) REFERENCES life_area(id) ON DELETE RESTRICT,
    CONSTRAINT chk_growth_habit_kind
        CHECK (kind IN ('GROWTH_HABIT', 'MILESTONE')),
    CONSTRAINT chk_growth_habit_tracking
        CHECK (tracking_method IN
            ('CHECKBOX', 'DURATION', 'QUANTITY', 'RATING', 'VALUE', 'MILESTONE')),
    CONSTRAINT chk_growth_habit_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_growth_habit_user_status
    ON growth_habit(user_id, status, position_index);
CREATE INDEX idx_growth_habit_life_area
    ON growth_habit(life_area_id);

CREATE TABLE habit_schedule (
    id BINARY(16) PRIMARY KEY,
    habit_id BINARY(16) NOT NULL UNIQUE,
    schedule_type VARCHAR(28) NOT NULL,
    start_date DATE NOT NULL,
    weekdays VARCHAR(40) NULL,
    interval_days INT NULL,
    target_count INT NULL,
    due_date DATE NULL,
    custom_description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_habit_schedule_habit
        FOREIGN KEY (habit_id) REFERENCES growth_habit(id) ON DELETE CASCADE,
    CONSTRAINT chk_habit_schedule_type
        CHECK (schedule_type IN
            ('DAILY', 'SELECTED_WEEKDAYS', 'ALTERNATE_DAYS', 'EVERY_N_DAYS',
             'TIMES_PER_WEEK', 'TIMES_PER_MONTH', 'ROTATION', 'ONE_TIME', 'CUSTOM')),
    CONSTRAINT chk_habit_schedule_interval
        CHECK (interval_days IS NULL OR interval_days BETWEEN 2 AND 365),
    CONSTRAINT chk_habit_schedule_target
        CHECK (target_count IS NULL OR target_count BETWEEN 1 AND 31)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
