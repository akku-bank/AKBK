-- ===========================================================================
-- [Flyway Migration] ERD 최신화 동기화 및 동시성 방지 제약조건 추가
-- ===========================================================================

-- 1. [신규 테이블] 계좌 점유 인증 (Account Verifications)
CREATE TABLE account_verifications (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       user_id UUID NOT NULL,
                                       bank_code VARCHAR(3) NOT NULL,
                                       account_number VARCHAR(100) NOT NULL,
                                       auth_code VARCHAR(4) NOT NULL,
                                       status VARCHAR(20) DEFAULT 'PENDING',
                                       expires_at TIMESTAMP NOT NULL,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_av_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 2. [신규 테이블] 카드 상품 (Card Products)
CREATE TABLE card_products (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               card_unique_no VARCHAR(20) UNIQUE NOT NULL,
                               card_issuer_code VARCHAR(10),
                               card_issuer_name VARCHAR(100),
                               card_name VARCHAR(255) NOT NULL,
                               card_description VARCHAR(1000),
                               card_type_code VARCHAR(10),
                               card_type_name VARCHAR(50),
                               base_limit_performance BIGINT,
                               max_benefit_limit BIGINT,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. [신규 테이블] 보유 카드 (Cards)
CREATE TABLE cards (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       user_id UUID NOT NULL,
                       card_product_id UUID NOT NULL,
                       card_no VARCHAR(20) UNIQUE NOT NULL,
                       cvc VARCHAR(3) NOT NULL,
                       card_expiry_date VARCHAR(8) NOT NULL,
                       withdrawal_account_no VARCHAR(20),
                       withdrawal_date VARCHAR(2),
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users(id),
                       CONSTRAINT fk_cards_product FOREIGN KEY (card_product_id) REFERENCES card_products(id)
);

-- 4. [컬럼 복구] users 테이블
-- 이전 마이그레이션에서 삭제되었으나 ERD에 존재하는 score 컬럼 복구
ALTER TABLE users ADD COLUMN score INTEGER DEFAULT 0;

-- 5. [컬럼명 동기화] transactions 테이블
-- ERD 명세와 통일하여 category를 sub_category_name으로 변경
ALTER TABLE transactions RENAME COLUMN category TO sub_category_name;

-- 6. [컬럼 추가] active_charities 테이블
-- ERD에 정의된 진행 중인 저금통의 목표 금액 컬럼 추가
ALTER TABLE active_charities ADD COLUMN target_amount INTEGER NOT NULL DEFAULT 500;

-- 7. [컬럼 추가] weekly_category_ratio 테이블
-- ERD에 정의된 카테고리별 소비 금액 컬럼 추가
ALTER TABLE weekly_category_ratio ADD COLUMN spending_amount BIGINT NOT NULL DEFAULT 0;

-- 8. [제약조건 추가] spending_challenges 테이블
-- 동일 유저가 동일 주간(start_date)에 동일 카테고리(sub_category_name)를
-- 중복 등록하지 못하도록 복합 유니크 제약조건 추가 (동시성 차단)
ALTER TABLE spending_challenges
    ADD CONSTRAINT uq_spending_challenge UNIQUE (user_id, sub_category_name, start_date);