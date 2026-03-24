-- ==========================================
-- 오프라인 결제 토큰 테이블 생성 (Offline Payment Tokens)
-- ==========================================

CREATE TABLE offline_payment_tokens
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    is_used    BOOLEAN      NOT NULL DEFAULT FALSE,
    expired_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- 외래키 제약조건 (Users 테이블 참조)
ALTER TABLE offline_payment_tokens
    ADD CONSTRAINT fk_opt_user FOREIGN KEY (user_id) REFERENCES users (id);
