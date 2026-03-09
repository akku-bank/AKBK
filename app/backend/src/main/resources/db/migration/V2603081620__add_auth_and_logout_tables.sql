-- users 테이블에 소셜 로그인 및 금융망 연동 컬럼 추가
ALTER TABLE users
    ADD COLUMN email VARCHAR(255) UNIQUE,
    ADD COLUMN provider VARCHAR(20),
    ADD COLUMN provider_id VARCHAR(100),
    ADD COLUMN user_key VARCHAR(255),
    ALTER COLUMN pin_password DROP NOT NULL;

-- 동일 소셜 계정 중복 방지
ALTER TABLE users
    ADD CONSTRAINT uq_provider UNIQUE (provider, provider_id);

-- 로그아웃된 토큰 블랙리스트 (서버 측 JWT 무효화)
CREATE TABLE logout_tokens (
    token VARCHAR(512) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    expired_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
