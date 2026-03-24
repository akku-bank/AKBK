package com.akku.backend.domain.report.batch;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 자녀 소비 레벨 및 점수를 업데이트하는 Spring Batch 설정
 */
@Configuration
@RequiredArgsConstructor
public class WeeklyLevelBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final WeeklyLevelProcessor weeklyLevelProcessor;

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
        return new JpaPagingItemReaderBuilder<User>()
                .name("userReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u WHERE u.role = 'CHILD' AND u.isActive = true") // 활성 자녀만 대상
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
