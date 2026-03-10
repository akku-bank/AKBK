-- ==========================================
-- 1. 테이블 생성 (Tables)
-- ==========================================

-- 1. 가족 그룹 (Families)
CREATE TABLE families
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    qr_code       VARCHAR(100) UNIQUE,
    qr_expires_at TIMESTAMP,
    created_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP -- 피드백 반영: 갱신일 추가
);


-- 2. 유저 (Users: 부모/자녀 통합)
CREATE TABLE users
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id    UUID,
    role         VARCHAR(20)         NOT NULL,
    name         VARCHAR(50)         NOT NULL,
    email        VARCHAR(100) UNIQUE NOT NULL,
    birth_date   DATE                NOT NULL,
    fin_user_key VARCHAR(60) UNIQUE,
    pin_password VARCHAR(255)        NOT NULL,
    fcm_token    VARCHAR(255),
    level        INTEGER          DEFAULT 1,
    is_hidden    BOOLEAN          DEFAULT FALSE,
    is_active    BOOLEAN          DEFAULT TRUE,
    created_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 3. 통합 계좌 (Accounts)
CREATE TABLE accounts
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID   NOT NULL,
    balance    BIGINT NOT NULL  DEFAULT 0,                -- 피드백 반영: 돈은 BIGINT
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP -- 피드백 반영: 잔액 갱신일 추가
);

-- 4. 가맹점 / 카테고리 고정 데이터 (Merchant)
CREATE TABLE merchant
(
    merchant_id       BIGINT PRIMARY KEY,
    merchant_name     VARCHAR(255) NOT NULL,
    is_green          BOOLEAN DEFAULT FALSE,
    category_id       INTEGER,
    sub_category_id   INTEGER,
    sub_category_name VARCHAR(255)
);

-- 5. 거래 내역 (Transactions)
CREATE TABLE transactions
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID        NOT NULL,
    merchant_id      BIGINT,
    amount           BIGINT      NOT NULL, -- 피드백 반영: 돈은 BIGINT
    transaction_type VARCHAR(30) NOT NULL,
    category         VARCHAR(50),
    merchant_name    VARCHAR(255),
    created_at       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 6. 젤링 지갑 (Jellings)
CREATE TABLE jellings
(
    user_id    UUID PRIMARY KEY,
    balance    BIGINT NOT NULL DEFAULT 0, -- 피드백 반영: 돈은 BIGINT
    updated_at TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 7. 젤링 변동 내역 (Jelling Transactions)
CREATE TABLE jelling_transactions
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    amount      BIGINT      NOT NULL, -- 피드백 반영: 돈은 BIGINT
    type        VARCHAR(30) NOT NULL,
    description VARCHAR(100),
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 8. 상점 아이템 (Items)
CREATE TABLE items
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    category       VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    resource_url   TEXT,
    price          INTEGER      NOT NULL,
    required_level INTEGER      NOT NULL DEFAULT 1,
    created_at     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- 9. 보유 아이템/인벤토리 (User Items)
CREATE TABLE user_items
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    item_id     UUID NOT NULL,
    quantity    INTEGER          DEFAULT 1,
    is_equipped BOOLEAN          DEFAULT FALSE,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 10. 기부처 목록 (Charities)
CREATE TABLE charities
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    target_amount INTEGER      NOT NULL DEFAULT 500,
    created_at    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- 11. 현재 진행 중인 저금통 (Active Charity)
CREATE TABLE active_charities
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    charity_id     UUID NOT NULL,
    current_amount BIGINT           DEFAULT 0,                -- 피드백 반영: 돈은 BIGINT
    status         VARCHAR(20)      DEFAULT 'IN_PROGRESS',
    updated_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP -- 피드백 반영: 갱신일 추가
);
CREATE UNIQUE INDEX idx_active_charities_unique_progress ON active_charities (user_id, status) WHERE status = 'IN_PROGRESS';

-- 12. 소비 목표 챌린지 (Spending Challenges)
CREATE TABLE spending_challenges
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID        NOT NULL,
    category        VARCHAR(50) NOT NULL,
    target_spending BIGINT      NOT NULL,                      -- 피드백 반영: 돈은 BIGINT
    reward_amount   BIGINT      NOT NULL,                      -- 피드백 반영: 돈은 BIGINT
    parent_message  TEXT,
    status          VARCHAR(20)      DEFAULT 'PENDING',
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    created_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP -- 피드백 반영: 상태 갱신일 추가
);

-- 13. 친구 연결 (Friends)
CREATE TABLE friends
(
    user_id    UUID NOT NULL,
    friend_id  UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id)
);

-- 14. 친구 초대 링크 (Friend Invites)
CREATE TABLE friend_invites
(
    invite_code VARCHAR(100) PRIMARY KEY,
    inviter_id  UUID      NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 15. 주간 통계 리포트 (Weekly Report)
CREATE TABLE weekly_report
(
    user_id             UUID        NOT NULL,
    start_day           DATE        NOT NULL,
    type                VARCHAR(20) NOT NULL,
    end_day             DATE,
    mon                 BIGINT DEFAULT 0, -- 피드백 반영: 원화는 소수점 없으므로 DECIMAL 대신 BIGINT
    tue                 BIGINT DEFAULT 0,
    wed                 BIGINT DEFAULT 0,
    thu                 BIGINT DEFAULT 0,
    fri                 BIGINT DEFAULT 0,
    sat                 BIGINT DEFAULT 0,
    sun                 BIGINT DEFAULT 0,
    total_amount        BIGINT,           -- 피드백 반영
    quiz_ai_summary     TEXT,
    spending_ai_summary TEXT,
    PRIMARY KEY (user_id, start_day, type)
);

-- 16. 주간 카테고리 소비 비율 (Weekly Category Ratio)
CREATE TABLE weekly_category_ratio
(
    user_id           UUID    NOT NULL,
    start_day         DATE    NOT NULL,
    sub_category_id   INTEGER NOT NULL,
    sub_category_name VARCHAR(255),
    ratio             DECIMAL, -- 비율(0.15 등)이므로 DECIMAL 유지
    PRIMARY KEY (user_id, start_day, sub_category_id)
);

-- 17. 금융 퀴즈 문제 은행 (Quizzes)
CREATE TABLE quizzes
(
    id                UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    topic             VARCHAR(50) NOT NULL,
    difficulty        VARCHAR(10) NOT NULL,
    problem_json JSONB NOT NULL,
    correct_choice_no INTEGER     NOT NULL,
    explanation       TEXT        NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);
-- 피드백 반영: 시간을 제외한 '날짜' 기준으로 난이도당 1개씩만 허용
CREATE UNIQUE INDEX idx_quizzes_created_date_diff ON quizzes ((created_at::DATE), difficulty);

-- 18. 유저 퀴즈 풀이 이력 (User Quizzes)
CREATE TABLE user_quizzes
(
    user_id           UUID      NOT NULL,
    quiz_id           UUID      NOT NULL,
    remaining_credits INTEGER   NOT NULL DEFAULT 100,
    is_submitted      BOOLEAN   NOT NULL DEFAULT FALSE,
    is_correct        BOOLEAN,
    solved_date       DATE      NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, quiz_id)
);
CREATE UNIQUE INDEX idx_user_quizzes_solved ON user_quizzes (user_id, quiz_id, solved_date);

-- 19. AI 채팅 로그 (Chat Logs)
CREATE TABLE chat_logs
(
    user_id    UUID NOT NULL,
    quiz_id    UUID NOT NULL,
    chat_json JSONB NOT NULL,
    created_at DATE NOT NULL,
    PRIMARY KEY (user_id, quiz_id)
);

-- ==========================================
-- 2. 외래키 제약조건 (Foreign Keys)
-- ==========================================
ALTER TABLE users
    ADD CONSTRAINT fk_users_family FOREIGN KEY (family_id) REFERENCES families (id);
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES accounts (id);
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (merchant_id);
ALTER TABLE jellings
    ADD CONSTRAINT fk_jellings_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE jelling_transactions
    ADD CONSTRAINT fk_jelling_tx_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_items
    ADD CONSTRAINT fk_ui_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_items
    ADD CONSTRAINT fk_ui_item FOREIGN KEY (item_id) REFERENCES items (id);
ALTER TABLE active_charities
    ADD CONSTRAINT fk_ac_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE active_charities
    ADD CONSTRAINT fk_ac_charity FOREIGN KEY (charity_id) REFERENCES charities (id);
ALTER TABLE spending_challenges
    ADD CONSTRAINT fk_sc_child FOREIGN KEY (child_id) REFERENCES users (id);
ALTER TABLE friends
    ADD CONSTRAINT fk_friends_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE friends
    ADD CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES users (id);
ALTER TABLE friend_invites
    ADD CONSTRAINT fk_invites_inviter FOREIGN KEY (inviter_id) REFERENCES users (id);
ALTER TABLE weekly_report
    ADD CONSTRAINT fk_wr_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE weekly_category_ratio
    ADD CONSTRAINT fk_wcr_user FOREIGN KEY (user_id) REFERENCES users (id);
-- ALTER TABLE weekly_category_ratio
   --  ADD CONSTRAINT fk_wcr_merchant FOREIGN KEY (sub_category_id) REFERENCES merchant (sub_category_id);
ALTER TABLE user_quizzes
    ADD CONSTRAINT fk_uq_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_quizzes
    ADD CONSTRAINT fk_uq_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id);
ALTER TABLE chat_logs
    ADD CONSTRAINT fk_cl_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE chat_logs
    ADD CONSTRAINT fk_cl_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id);