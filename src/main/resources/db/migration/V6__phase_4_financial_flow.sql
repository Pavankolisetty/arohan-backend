CREATE TABLE finance_profile (
    user_id BINARY(16) PRIMARY KEY,
    currency_code CHAR(3) NOT NULL DEFAULT 'INR',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_profile_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE finance_bucket (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    system_key VARCHAR(24) NOT NULL,
    name VARCHAR(60) NOT NULL,
    color_hex CHAR(7) NOT NULL,
    icon_key VARCHAR(40) NOT NULL,
    position_index INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_bucket_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_finance_bucket_key UNIQUE (user_id, system_key),
    CONSTRAINT chk_finance_bucket_key
        CHECK (system_key IN ('NEEDS', 'WANTS', 'EXPERIENCES', 'UNEXPECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE finance_category (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    bucket_id BINARY(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    position_index INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_category_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_finance_category_bucket
        FOREIGN KEY (bucket_id) REFERENCES finance_bucket(id),
    CONSTRAINT uq_finance_category_name UNIQUE (user_id, bucket_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE finance_transaction (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    type VARCHAR(24) NOT NULL,
    bucket_id BINARY(16) NULL,
    category_id BINARY(16) NULL,
    title VARCHAR(120) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    occurred_on DATE NOT NULL,
    payment_mode VARCHAR(24) NOT NULL,
    transfer_direction VARCHAR(16) NULL,
    income_source VARCHAR(120) NULL,
    note VARCHAR(500) NULL,
    recurring_frequency VARCHAR(16) NULL,
    recurring_until DATE NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_transaction_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_finance_transaction_bucket
        FOREIGN KEY (bucket_id) REFERENCES finance_bucket(id),
    CONSTRAINT fk_finance_transaction_category
        FOREIGN KEY (category_id) REFERENCES finance_category(id),
    CONSTRAINT chk_finance_transaction_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'SAVINGS', 'REFUND', 'TRANSFER')),
    CONSTRAINT chk_finance_transaction_payment
        CHECK (payment_mode IN ('CASH', 'BANK', 'CARD', 'UPI', 'DIGITAL_WALLET', 'OTHER')),
    CONSTRAINT chk_finance_transaction_transfer
        CHECK (transfer_direction IS NULL OR transfer_direction IN ('CASH_IN', 'CASH_OUT')),
    CONSTRAINT chk_finance_transaction_recurrence
        CHECK (recurring_frequency IS NULL OR recurring_frequency IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT chk_finance_transaction_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_finance_transaction_user_date
    ON finance_transaction(user_id, occurred_on);
CREATE INDEX idx_finance_transaction_user_type
    ON finance_transaction(user_id, type);

CREATE TABLE finance_cash_adjustment (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    adjusted_on DATE NOT NULL,
    reason VARCHAR(240) NOT NULL,
    adjustment_kind VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_cash_adjustment_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT chk_finance_cash_adjustment_amount CHECK (amount <> 0),
    CONSTRAINT chk_finance_cash_adjustment_kind
        CHECK (adjustment_kind IN ('OPENING', 'CORRECTION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_finance_cash_adjustment_user_date
    ON finance_cash_adjustment(user_id, adjusted_on);

CREATE TABLE finance_month_plan (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    month_start DATE NOT NULL,
    expected_income DECIMAL(19,2) NOT NULL DEFAULT 0,
    savings_target DECIMAL(19,2) NOT NULL DEFAULT 0,
    intention VARCHAR(500) NULL,
    went_well VARCHAR(700) NULL,
    learned VARCHAR(700) NULL,
    next_month_change VARCHAR(700) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_month_plan_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_finance_month_plan UNIQUE (user_id, month_start),
    CONSTRAINT chk_finance_month_plan_expected CHECK (expected_income >= 0),
    CONSTRAINT chk_finance_month_plan_savings CHECK (savings_target >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE finance_bucket_budget (
    id BINARY(16) PRIMARY KEY,
    plan_id BINARY(16) NOT NULL,
    bucket_id BINARY(16) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_finance_bucket_budget_plan
        FOREIGN KEY (plan_id) REFERENCES finance_month_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_finance_bucket_budget_bucket
        FOREIGN KEY (bucket_id) REFERENCES finance_bucket(id),
    CONSTRAINT uq_finance_bucket_budget UNIQUE (plan_id, bucket_id),
    CONSTRAINT chk_finance_bucket_budget_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
