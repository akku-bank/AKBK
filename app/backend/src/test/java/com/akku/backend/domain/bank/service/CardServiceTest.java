package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.CardHistoryRequest;
import com.akku.backend.domain.bank.dto.CardHistoryResponse;
import com.akku.backend.domain.bank.dto.CardPaymentRequest;
import com.akku.backend.domain.bank.entity.Card;
import com.akku.backend.domain.bank.repository.CardProductRepository;
import com.akku.backend.domain.bank.repository.CardRepository;
import com.akku.backend.global.finance.dto.FinanceCardPaymentResponse;
import com.akku.backend.global.finance.dto.FinanceCardTransactionHistoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @InjectMocks
    private CardService cardService;

    @Mock
    private CardProductRepository cardProductRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private SsafyFinanceService ssafyFinanceService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("카드 결제 처리 테스트")
    void processPaymentTest() {
        // given
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("user-key").build();
        Card card = Card.builder().id(cardId).userId(userId).cardNo("1234").cvc("123").build();
        CardPaymentRequest request = new CardPaymentRequest(cardId, 1L, 10000L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // when
        cardService.processPayment(userId, request);

        // then
        verify(ssafyFinanceService).createCardTransaction(eq("user-key"), eq("1234"), eq("123"), eq(1L), eq(10000L));
    }

    @Test
    @DisplayName("카드 거래 내역 조회 테스트")
    void getCardHistoryTest() {
        // given
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("user-key").build();
        Card card = Card.builder().id(cardId).userId(userId).cardNo("1234").cvc("123").build();
        CardHistoryRequest request = new CardHistoryRequest(cardId, "20240101", "20240131");

        FinanceCardTransactionHistoryResponse.Rec rec = new FinanceCardTransactionHistoryResponse.Rec(
                "1005", "신한카드", "신한 TRAVEL", "1234", 2000000L,
                List.of(new FinanceCardTransactionHistoryResponse.TransactionDetails(
                        1L, "CG-1", "식비", 1L, "편의점", "20240115", "103000", 5000L, "승인", "N", "미결제"
                ))
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(ssafyFinanceService.getCardTransactionHistory(any(), any(), any(), any(), any())).willReturn(rec);

        // when
        CardHistoryResponse response = cardService.getCardHistory(userId, request);

        // then
        assertThat(response.estimatedBalance()).isEqualTo(2000000L);
        assertThat(response.list()).hasSize(1);
        assertThat(response.list().get(0).merchantName()).isEqualTo("편의점");
        assertThat(response.list().get(0).transactionBalance()).isEqualTo(5000L);
    }
}
