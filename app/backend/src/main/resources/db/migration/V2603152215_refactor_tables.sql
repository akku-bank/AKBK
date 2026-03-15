-- 1. spending_challenges 테이블 컬럼명 및 외래키 수정
ALTER TABLE spending_challenges DROP CONSTRAINT fk_sc_child;
ALTER TABLE spending_challenges RENAME COLUMN child_id TO user_id;
ALTER TABLE spending_challenges RENAME COLUMN category TO sub_category_name;
ALTER TABLE spending_challenges ADD CONSTRAINT fk_sc_child FOREIGN KEY (user_id) REFERENCES users(id);

-- 2. friend_invites 테이블 컬럼명 및 외래키 수정, 만료일 삭제
ALTER TABLE friend_invites DROP CONSTRAINT fk_invites_inviter;
ALTER TABLE friend_invites RENAME COLUMN inviter_id TO user_id;
ALTER TABLE friend_invites DROP COLUMN expires_at;
ALTER TABLE friend_invites ADD CONSTRAINT fk_invites_inviter FOREIGN KEY (user_id) REFERENCES users(id);

-- 3. quizzes 테이블 정답 컬럼명 수정
ALTER TABLE quizzes RENAME COLUMN correct_choice_no TO correct_answer;

-- 4. users 테이블 불필요해진 컬럼 삭제 (이전 버전에 있던 것들 정리)
ALTER TABLE users DROP COLUMN IF EXISTS email;
ALTER TABLE users DROP COLUMN IF EXISTS score;
ALTER TABLE users DROP COLUMN IF EXISTS has_level_changed;
ALTER TABLE users DROP COLUMN IF EXISTS custom_tickets;