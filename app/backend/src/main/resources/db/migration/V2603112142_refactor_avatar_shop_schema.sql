-- ===========================================================================
-- [Flyway Migration] HOME/AVATAR 도메인 기획 변경에 따른 스키마 수정
-- ===========================================================================

-- 1. users 테이블: 소비 점수, 레벨 변동 플래그, 커스텀 티켓 컬럼 추가
ALTER TABLE users
    ADD COLUMN score INTEGER DEFAULT 0,
    ADD COLUMN has_level_changed BOOLEAN DEFAULT FALSE,
    ADD COLUMN custom_tickets INTEGER DEFAULT 0;

-- 2. items 테이블: '상점 구매' 기획 폐기에 따른 가격(price) 컬럼 삭제
ALTER TABLE items
    DROP COLUMN price;

-- 3. user_items (인벤토리) 테이블: 티켓 분리 및 중복 보유 무의미화에 따른 수량(quantity) 컬럼 삭제
ALTER TABLE user_items
    DROP COLUMN quantity;