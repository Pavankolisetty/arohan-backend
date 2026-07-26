ALTER TABLE growth_habit
    ADD COLUMN client_request_id BINARY(16) NULL AFTER life_area_id,
    ADD CONSTRAINT uq_growth_habit_creation
        UNIQUE (user_id, client_request_id);
