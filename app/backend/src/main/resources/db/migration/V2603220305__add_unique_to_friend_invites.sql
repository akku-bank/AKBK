-- 친구 초대 링크 테이블의 user_id에 유니크 제약 조건 추가 (중복 발급 방지)
ALTER TABLE friend_invites ADD CONSTRAINT uq_friend_invites_user_id UNIQUE (user_id);
