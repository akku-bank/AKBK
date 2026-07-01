package com.akku.backend.domain.bank.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransferRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("이체 금액이 양수일 때 - 검증 통과")
    void amount_Positive_Success() {
        TransferRequest request = new TransferRequest("from", "001", "12345", "to", 1000L, "123456");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("이체 금액이 0일 때 - 검증 실패")
    void amount_Zero_Fail() {
        TransferRequest request = new TransferRequest("from", "001", "12345", "to", 0L, "123456");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("이체 금액은 0보다 커야 합니다.")));
    }

    @Test
    @DisplayName("이체 금액이 음수일 때 - 검증 실패")
    void amount_Negative_Fail() {
        TransferRequest request = new TransferRequest("from", "001", "12345", "to", -1000L, "123456");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("이체 금액은 0보다 커야 합니다.")));
    }
}
