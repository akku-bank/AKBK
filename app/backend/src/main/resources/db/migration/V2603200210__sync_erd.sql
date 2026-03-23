ALTER TABLE users ADD COLUMN IF NOT EXISTS score INTEGER DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_provider') THEN
        ALTER TABLE users ADD CONSTRAINT uq_provider UNIQUE (provider, provider_id);
    END IF;
END $$;

-- 2. accounts 테이블: 계좌번호 필수 및 유니크 제약 설정
ALTER TABLE accounts ALTER COLUMN account_number SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_account_number') THEN
        ALTER TABLE accounts ADD CONSTRAINT uq_account_number UNIQUE (account_number);
    END IF;
END $$;

-- 3. account_verifications 테이블 생성 (계좌 점유 인증)
CREATE TABLE IF NOT EXISTS account_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    bank_code VARCHAR(3) NOT NULL,
    account_number VARCHAR(100) NOT NULL,
    auth_code VARCHAR(4) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 4. charities & active_charities 구조 변경 (목표 금액 이동)
-- 기존 charities에서 target_amount 제거 (데이터 유실 주의 - 필요시 백업 logic 추가 가능하나 명세 우선)
ALTER TABLE charities DROP COLUMN IF EXISTS target_amount;
ALTER TABLE charities DROP COLUMN IF EXISTS created_at;

-- active_charities에 target_amount 및 created_at 추가
ALTER TABLE active_charities ADD COLUMN IF NOT EXISTS target_amount INTEGER NOT NULL DEFAULT 500;
ALTER TABLE active_charities ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 5. transactions 테이블 컬럼명 변경 (category -> sub_category_name)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'transactions' AND column_name = 'category') THEN
        ALTER TABLE transactions RENAME COLUMN category TO sub_category_name;
    END IF;
END $$;


-- 7. user_items 테이블 수량(quantity) 컬럼 복구 (티켓 보유용)
ALTER TABLE user_items ADD COLUMN IF NOT EXISTS quantity INTEGER DEFAULT 1;

-- 8. weekly_category_ratio 테이블 지출 금액 컬럼 추가
ALTER TABLE weekly_category_ratio ADD COLUMN IF NOT EXISTS spending_amount BIGINT NOT NULL DEFAULT 0;

