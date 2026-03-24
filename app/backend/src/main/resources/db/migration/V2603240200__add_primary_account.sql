-- accounts 테이블에 주계좌 여부 컬럼 추가
ALTER TABLE accounts ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT false;

-- 사용자당 주계좌는 최대 1개 (PostgreSQL partial unique index)
CREATE UNIQUE INDEX uq_user_primary_account
    ON accounts(user_id)
    WHERE is_primary = true;
