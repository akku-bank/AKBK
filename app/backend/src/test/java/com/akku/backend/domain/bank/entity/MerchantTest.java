package com.akku.backend.domain.bank.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MerchantTest {

    @Test
    @DisplayName("Merchant 생성 테스트 - merchantId 없이 생성 가능 여부 확인")
    void createMerchant_WithoutId_Success() {
        Merchant merchant = Merchant.builder()
                .merchantName("테스트 가맹점")
                .subCategoryName("테스트 카테고리")
                .build();

        assertNull(merchant.getMerchantId());
        assertEquals("테스트 가맹점", merchant.getMerchantName());
        assertEquals("테스트 카테고리", merchant.getSubCategoryName());
    }
}
