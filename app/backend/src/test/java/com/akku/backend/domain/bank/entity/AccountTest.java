package com.akku.backend.domain.bank.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    @DisplayName("deductBalance - 음수 금액 입력 시 예외 발생 검증")
    void deductBalance_NegativeAmount_ThrowsException() {
        Account account = Account.builder()
                .balance(1000L)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            account.deductBalance(-500L);
        });

        assertEquals("Amount must be positive", exception.getMessage());
        assertEquals(1000L, account.getBalance());
    }

    @Test
    @DisplayName("deductBalance - 0원 입력 시 예외 발생 검증")
    void deductBalance_ZeroAmount_ThrowsException() {
        Account account = Account.builder()
                .balance(1000L)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            account.deductBalance(0L);
        });

        assertEquals("Amount must be positive", exception.getMessage());
        assertEquals(1000L, account.getBalance());
    }

    @Test
    @DisplayName("deductBalance - 정상적인 양수 금액 차감 검증")
    void deductBalance_PositiveAmount_Success() {
        Account account = Account.builder()
                .balance(1000L)
                .build();

        account.deductBalance(500L);

        assertEquals(500L, account.getBalance());
    }

    @Test
    @DisplayName("deductBalance - 잔액 부족 시 예외 발생 검증")
    void deductBalance_InsufficientBalance_ThrowsException() {
        Account account = Account.builder()
                .balance(100L)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            account.deductBalance(500L);
        });

        assertEquals("Insufficient balance", exception.getMessage());
        assertEquals(100L, account.getBalance());
    }
}
