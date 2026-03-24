-- ==========================================
-- offline_payment_tokens 테이블에 version 컬럼 추가
-- ==========================================

ALTER TABLE offline_payment_tokens 
    ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
