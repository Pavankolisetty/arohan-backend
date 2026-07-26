CREATE TABLE journal_tag (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    name VARCHAR(40) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_journal_tag_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_journal_tag_user_name UNIQUE (user_id, name)
);

CREATE TABLE journal_entry (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    entry_type VARCHAR(24) NOT NULL,
    title VARCHAR(120) NULL,
    content VARCHAR(4000) NULL,
    entry_date DATE NOT NULL,
    life_area_id BINARY(16) NULL,
    habit_id BINARY(16) NULL,
    mood_score TINYINT NULL,
    energy_score TINYINT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    period_start DATE NULL,
    period_end DATE NULL,
    wins VARCHAR(1000) NULL,
    friction VARCHAR(1000) NULL,
    next_adjustment VARCHAR(700) NULL,
    small_commitment VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_journal_entry_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_entry_life_area FOREIGN KEY (life_area_id) REFERENCES life_area(id) ON DELETE SET NULL,
    CONSTRAINT fk_journal_entry_habit FOREIGN KEY (habit_id) REFERENCES growth_habit(id) ON DELETE SET NULL,
    CONSTRAINT ck_journal_entry_mood CHECK (mood_score IS NULL OR mood_score BETWEEN 1 AND 5),
    CONSTRAINT ck_journal_entry_energy CHECK (energy_score IS NULL OR energy_score BETWEEN 1 AND 5)
);

CREATE INDEX idx_journal_entry_user_date ON journal_entry(user_id, entry_date);
CREATE INDEX idx_journal_entry_user_pinned ON journal_entry(user_id, pinned);
CREATE INDEX idx_journal_entry_life_area ON journal_entry(life_area_id);
CREATE INDEX idx_journal_entry_habit ON journal_entry(habit_id);

CREATE TABLE journal_entry_tag (
    entry_id BINARY(16) NOT NULL,
    tag_id BINARY(16) NOT NULL,
    PRIMARY KEY (entry_id, tag_id),
    CONSTRAINT fk_journal_entry_tag_entry FOREIGN KEY (entry_id) REFERENCES journal_entry(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_entry_tag_tag FOREIGN KEY (tag_id) REFERENCES journal_tag(id) ON DELETE CASCADE
);

CREATE INDEX idx_journal_entry_tag_tag ON journal_entry_tag(tag_id);
