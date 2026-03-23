package com.akku.backend.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AESUtilTest {

    private AESUtil aesUtil;
    private final String testKey = "akku-cvc-encryption-key-32chars!";

    @BeforeEach
    void setUp() {
        aesUtil = new AESUtil(testKey);
    }

    @Test
    @DisplayName("암호화 및 복호화 성공 테스트")
    void encryptAndDecryptSuccess() {
        // given
        String originalText = "123";

        // when
        String encryptedText = aesUtil.encrypt(originalText);
        String decryptedText = aesUtil.decrypt(encryptedText);

        // then
        assertThat(encryptedText).isNotEqualTo(originalText);
        assertThat(decryptedText).isEqualTo(originalText);
    }

    @Test
    @DisplayName("서로 다른 IV로 인한 암호문 상이성 테스트")
    void differentIvProduction() {
        // given
        String originalText = "123";

        // when
        String encrypted1 = aesUtil.encrypt(originalText);
        String encrypted2 = aesUtil.encrypt(originalText);

        // then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(aesUtil.decrypt(encrypted1)).isEqualTo(originalText);
        assertThat(aesUtil.decrypt(encrypted2)).isEqualTo(originalText);
    }
}
