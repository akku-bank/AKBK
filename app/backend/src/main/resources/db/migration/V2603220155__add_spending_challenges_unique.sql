-- 소비 챌린지 중복 등록 방지 (동시성 문제 해결용 복합 유니크 제약조건)
ALTER TABLE spending_challenges
    ADD CONSTRAINT uq_spending_challenge UNIQUE (user_id, sub_category_name, start_date);