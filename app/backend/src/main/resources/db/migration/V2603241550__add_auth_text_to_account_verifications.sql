-- ==========================================
-- account_verifications 테이블에 auth_text 컬럼 추가
-- ==========================================

ALTER TABLE account_verifications 
    ADD COLUMN auth_text VARCHAR(255) DEFAULT 'SSAFY' NOT NULL;
