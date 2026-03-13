-- 가족 구성원 프로필 테이블 생성 (빈 의자 및 연동 관리)
CREATE TABLE family_profiles
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id      UUID        NOT NULL,
    role           VARCHAR(20) NOT NULL, -- 'CHILD' or 'PARENT'
    name           VARCHAR(50) NOT NULL, -- 부모가 미리 등록해 둔 자녀의 실명
    birth_date     DATE        NOT NULL, -- 이름 매칭 시 정확도를 높이기 위한 생년월일
    linked_user_id UUID,                 -- 실제 가입한 유저와 매칭 전에는 NULL
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 외래키 제약조건 추가
ALTER TABLE family_profiles
    ADD CONSTRAINT fk_fp_family FOREIGN KEY (family_id) REFERENCES families (id);
ALTER TABLE family_profiles
    ADD CONSTRAINT fk_fp_user FOREIGN KEY (linked_user_id) REFERENCES users (id);

-- 성능 최적화를 위한 인덱스 추가 (이름 기반 매칭 시 활용)
CREATE INDEX idx_family_profiles_name_family ON family_profiles (family_id, name);