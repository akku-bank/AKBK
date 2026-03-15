-- accounts 테이블에 금융 연동 관련 컬럼 추가
ALTER TABLE accounts
    ADD COLUMN account_number VARCHAR(100),
    ADD COLUMN bank_code VARCHAR(3),
    ADD COLUMN type VARCHAR(20) DEFAULT 'CASH';

-- 별도 migration에서 기존 rows의 account_number를 backfill 한 뒤 적용
-- ALTER TABLE accounts
--     ALTER COLUMN account_number SET NOT NULL;
-- ALTER TABLE accounts
--     ADD CONSTRAINT uq_account_number UNIQUE (account_number);

-- type 필드는 기본값이 적용되므로 NOT NULL 설정이 안전함.
ALTER TABLE accounts
    ALTER COLUMN type SET NOT NULL;
