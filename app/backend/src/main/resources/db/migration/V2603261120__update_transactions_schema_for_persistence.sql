-- 거래 내역 테이블 고도화 (로컬 저장 및 개별 숨김 기능 지원)
-- 이미 존재하는 merchant_name 컬럼은 유지하고, 신규 필드만 추가합니다.

ALTER TABLE transactions ADD COLUMN transaction_unique_no VARCHAR(100);
ALTER TABLE transactions ADD COLUMN balance_after BIGINT;
ALTER TABLE transactions ADD COLUMN transaction_date VARCHAR(14);
ALTER TABLE transactions ADD COLUMN is_hidden BOOLEAN DEFAULT FALSE;

-- 고유 번호 제약 조건 추가 (데이터 정합성 및 중복 방지)
-- 중복 데이터가 있을 수 있으므로 실제 환경에선 정제 후 실행이 권장됩니다.
ALTER TABLE transactions ADD CONSTRAINT uk_transaction_unique_no UNIQUE (transaction_unique_no);
