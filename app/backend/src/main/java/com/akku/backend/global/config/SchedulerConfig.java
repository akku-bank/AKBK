package com.akku.backend.global.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * 스케줄러 및 비동기 처리 설정.
 *
 * @EnableScheduling : @Scheduled 어노테이션이 동작하도록 활성화
 * @EnableAsync      : @Async 어노테이션이 동작하도록 활성화 (이벤트 리스너 비동기 처리)
 * @EnableSchedulerLock : ShedLock 활성화 — 다중 서버 환경에서 스케줄러 중복 실행 방지
 *   defaultLockAtMostFor: 잠금을 유지할 최대 시간. 서버가 비정상 종료되어도 이 시간 이후 락이 해제됨.
 */
@Configuration
@EnableScheduling
@EnableAsync
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
public class SchedulerConfig {

    /**
     * ShedLock JDBC Provider.
     * 별도 인프라(Redis 등) 없이 기존 PostgreSQL DB를 재활용한다.
     * shedlock 테이블은 Flyway 마이그레이션으로 생성됨 (V2603231200__add_shedlock_table.sql).
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // DB 서버 시간 기준으로 락 관리 (서버 간 시계 차이 무관)
                        .build()
        );
    }
}
