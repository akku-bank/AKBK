-- accounts 테이블에 금융 연동 관련 컬럼 추가
ALTER TABLE accounts
    ADD COLUMN account_number VARCHAR(100),
    ADD COLUMN bank_code VARCHAR(3),
    ADD COLUMN type VARCHAR(20) DEFAULT 'CASH';

-- 제약 조건 및 정합성 설정
ALTER TABLE accounts
    ALTER COLUMN account_number SET NOT NULL,
    ALTER COLUMN type SET NOT NULL;

ALTER TABLE accounts
    ADD CONSTRAINT uq_account_number UNIQUE (account_number);
