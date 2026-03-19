-- =========================================================
-- Kafka 이벤트 중복 처리 방지를 위한 event_id 컬럼 추가
-- payment / transfer / deposit Consumer 가 DB 저장 시
-- 동일 eventId 로 중복 INSERT 를 방지하는 데 사용된다.
-- =========================================================

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS event_id UUID;

-- event_id 기반 중복 INSERT 방지 (nullable unique: null 은 중복 허용)
CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_event_id
    ON transactions (event_id)
    WHERE event_id IS NOT NULL;
