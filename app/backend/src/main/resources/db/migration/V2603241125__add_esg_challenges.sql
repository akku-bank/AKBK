CREATE TABLE esg_challenges
(
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,
    reward_amount BIGINT      NOT NULL DEFAULT 50,
    status        VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, SUCCESS, REWARDED
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    created_at    TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_esg_child     FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_esg_user_week UNIQUE (user_id, start_date)
);
