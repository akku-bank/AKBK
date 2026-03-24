-- weekly_report 테이블에 해당 주 시작 시점의 잔액(이월금)을 저장할 컬럼 추가
ALTER TABLE weekly_report ADD COLUMN IF NOT EXISTS start_balance BIGINT DEFAULT 0;
