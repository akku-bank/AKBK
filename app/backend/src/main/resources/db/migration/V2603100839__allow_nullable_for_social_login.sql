-- 소셜 로그인 시 아직 알 수 없는 필드들을 NULL 허용으로 변경
ALTER TABLE users ALTER COLUMN birth_date DROP NOT NULL;
ALTER TABLE users ALTER COLUMN pin_password DROP NOT NULL;
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
