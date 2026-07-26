ALTER TABLE app_user
    ADD COLUMN date_format VARCHAR(16) NOT NULL DEFAULT 'AUTO' AFTER week_start,
    ADD COLUMN time_format VARCHAR(24) NOT NULL DEFAULT 'SYSTEM' AFTER date_format,
    ADD COLUMN reduced_motion BOOLEAN NOT NULL DEFAULT FALSE AFTER time_format,
    ADD COLUMN enhanced_contrast BOOLEAN NOT NULL DEFAULT FALSE AFTER reduced_motion,
    ADD CONSTRAINT chk_date_format
        CHECK (date_format IN ('AUTO', 'DAY_FIRST', 'MONTH_FIRST', 'ISO')),
    ADD CONSTRAINT chk_time_format
        CHECK (time_format IN ('SYSTEM', 'TWELVE_HOUR', 'TWENTY_FOUR_HOUR'));

