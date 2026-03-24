-- user_quizzes.solved_date: 퀴즈 첫 조회(미제출) 시점에는 값이 없으므로 nullable로 변경
ALTER TABLE user_quizzes
    ALTER COLUMN solved_date DROP NOT NULL;

-- idx_user_quizzes_solved: PK(user_id, quiz_id)가 이미 유일성을 보장하고
-- solved_date가 nullable로 전환되면 NULL 비교가 제외되어 의미가 없어짐
DROP INDEX IF EXISTS idx_user_quizzes_solved;
