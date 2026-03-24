-- ShedLock: 다중 서버 환경에서 스케줄러 중복 실행 방지용 분산 락 테이블
-- shedlock-provider-jdbc-template 라이브러리가 이 테이블을 통해 락을 관리한다.
CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL,
    lock_until  TIMESTAMP    NOT NULL,
    locked_at   TIMESTAMP    NOT NULL,
    locked_by   VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
