package com.akku.backend.domain.report.batch;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 자녀 소비 레벨 및 점수를 업데이트하는 Spring Batch 설정
 */
@EnableBatchProcessing
@Configuration
@RequiredArgsConstructor
public class WeeklyLevelBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final WeeklyLevelProcessor weeklyLevelProcessor;
    private final Clock clock;

    @Bean
    public Job weeklyLevelUpdateJob() {
        return new JobBuilder("weeklyLevelUpdateJob", jobRepository)
                .start(weeklyLevelUpdateStep())
                .build();
    }

    @Bean
    public Step weeklyLevelUpdateStep() {
        return new StepBuilder("weeklyLevelUpdateStep", jobRepository)
                .<User, User>chunk(100, transactionManager)
                .reader(userReader())
                .processor(weeklyLevelProcessor)
                .writer(userWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userReader() {
        LocalDateTime lastMonday = LocalDate.now(clock).minusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay();
        return new JpaPagingItemReaderBuilder<User>()
                .name("userReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u WHERE u.role = 'CHILD' AND u.isActive = true AND u.createdAt < :lastMonday")
                .parameterValues(Map.of("lastMonday", lastMonday))
                .pageSize(100)
                .build();
    }

    @Bean
    public JpaItemWriter<User> userWriter() {
        return new JpaItemWriterBuilder<User>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
