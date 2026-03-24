-- 2026-03-24: accounts 테이블 백필 및 제약 조건 재강화 (V2603200210 보완)
-- 이미 NOT NULL 제약 조건이 존재하더라도 안전하게 백필을 수행하기 위해 DROP 후 다시 SET 합니다.

DO $$
BEGIN
    -- 1. 백필: NULL 데이터를 id(UUID)값으로 채움
    UPDATE accounts SET account_number = id::text WHERE account_number IS NULL;

    -- 2. 제약 조건 강화 (V2603200210에서 이미 적용되었더라도 정합성 확인 차원)
    ALTER TABLE accounts ALTER COLUMN account_number SET NOT NULL;
    
    -- 3. 유니크 제약 조건 (만약 V2603200210에서 실패했다면 여기서 재시도)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_account_number') THEN
        ALTER TABLE accounts ADD CONSTRAINT uq_account_number UNIQUE (account_number);
    END IF;
END $$;
