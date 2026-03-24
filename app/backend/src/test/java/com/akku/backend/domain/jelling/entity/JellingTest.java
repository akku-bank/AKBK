package com.akku.backend.domain.jelling.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JellingTest {

    @Test
    @DisplayName("deductBalance - 음수 금액 입력 시 예외 발생 검증")
    void deductBalance_NegativeAmount_ThrowsException() {
        Jelling jelling = Jelling.builder()
                .balance(1000L)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jelling.deductBalance(-500L);
        });

        assertEquals("Amount must be positive", exception.getMessage());
        assertEquals(1000L, jelling.getBalance());
    }

    @Test
    @DisplayName("addBalance - 음수 금액 입력 시 예외 발생 검증")
    void addBalance_NegativeAmount_ThrowsException() {
        Jelling jelling = Jelling.builder()
                .balance(1000L)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jelling.addBalance(-500L);
        });

        assertEquals("Amount must be positive", exception.getMessage());
        assertEquals(1000L, jelling.getBalance());
    }

    @Test
    @DisplayName("정상적인 잔액 변동 검증")
    void balance_Updates_Success() {
        Jelling jelling = Jelling.builder()
                .balance(1000L)
                .build();

        jelling.addBalance(500L);
        assertEquals(1500L, jelling.getBalance());

        jelling.deductBalance(300L);
        assertEquals(1200L, jelling.getBalance());
    }
}
