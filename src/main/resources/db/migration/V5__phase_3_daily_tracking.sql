ALTER TABLE growth_habit
    ADD COLUMN tracking_enabled_from DATE NULL AFTER position_index;

UPDATE growth_habit
SET tracking_enabled_from = CURRENT_DATE
WHERE tracking_enabled_from IS NULL;

ALTER TABLE growth_habit
    MODIFY COLUMN tracking_enabled_from DATE NOT NULL;

CREATE TABLE habit_tracking_entry (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    habit_id BINARY(16) NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(16) NULL,
    actual_value DECIMAL(19,4) NULL,
    quality_rating TINYINT NULL,
    reflection VARCHAR(1000) NULL,
    friction_note VARCHAR(300) NULL,
    cue_started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_tracking_entry_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_tracking_entry_habit
        FOREIGN KEY (habit_id) REFERENCES growth_habit(id) ON DELETE CASCADE,
    CONSTRAINT uq_tracking_entry_habit_date
        UNIQUE (habit_id, scheduled_date),
    CONSTRAINT chk_tracking_entry_status
        CHECK (status IS NULL OR status IN ('COMPLETED', 'PARTIAL', 'SKIPPED')),
    CONSTRAINT chk_tracking_entry_quality
        CHECK (quality_rating IS NULL OR quality_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_tracking_entry_actual
        CHECK (actual_value IS NULL OR actual_value >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_tracking_entry_user_date
    ON habit_tracking_entry(user_id, scheduled_date);
CREATE INDEX idx_tracking_entry_habit_date
    ON habit_tracking_entry(habit_id, scheduled_date);
