-- 1. 가족 그룹 (Families)
CREATE TABLE families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    qr_code VARCHAR(100) UNIQUE,
    qr_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 유저 (Users)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID REFERENCES families(id),
    role VARCHAR(20) NOT NULL, -- PARENT or CHILD
    name VARCHAR(50) NOT NULL,
    pin_password VARCHAR(255) NOT NULL,
    fcm_token VARCHAR(255),
    level INT DEFAULT 1,
    is_hidden BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 통합 계좌 (Accounts)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    balance INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 젤링 지갑
CREATE TABLE jellings (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    balance INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. 젤링 변동 내역
CREATE TABLE jelling_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. 상점 아이템 (Items)
CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    resource_url TEXT,
    price INT NOT NULL,
    required_level INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. 보유 아이템/인벤토리 (User Items)
CREATE TABLE user_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    item_id UUID NOT NULL REFERENCES items(id),
    quantity INT DEFAULT 1,
    is_equipped BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. 기부처 목록 (Charities)
CREATE TABLE charities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    target_amount INT NOT NULL DEFAULT 500,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 현재 진행 중인 저금통 (Active Charity)
CREATE TABLE active_charities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    charity_id UUID NOT NULL REFERENCES charities(id),
    current_amount INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    UNIQUE (user_id, status)
);

-- 10. 소비 목표 챌린지 (Spending Challenges)
CREATE TABLE spending_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES users(id),
    category VARCHAR(50) NOT NULL,
    target_spending INT NOT NULL,
    reward_amount INT NOT NULL,
    parent_message TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 11. 거래 내역 (Transactions)
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    amount INT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    category VARCHAR(50),
    merchant_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. 친구 연결 (Friends)
CREATE TABLE friends (
    user_id UUID NOT NULL REFERENCES users(id),
    friend_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id)
);

-- 13. 친구 초대 링크 (Friend Invites)
CREATE TABLE friend_invites (
    invite_code VARCHAR(100) PRIMARY KEY,
    inviter_id UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 14. 주간 리포트
CREATE TABLE weekly_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    total_spending INT NOT NULL,
    ai_spending_summary TEXT,
    ai_quiz_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 15. 금융 퀴즈 문제 은행 (Quizzes)
CREATE TABLE quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    explanation TEXT NOT NULL,
    reward_amount INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 16. 유저 퀴즈 풀이 이력 (User Quizzes)
CREATE TABLE user_quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    quiz_id UUID NOT NULL REFERENCES quizzes(id),
    solved_date DATE NOT NULL,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, solved_date)
);
