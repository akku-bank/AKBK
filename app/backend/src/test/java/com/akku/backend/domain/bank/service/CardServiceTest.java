package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.bank.dto.CardCreateRequest;
import com.akku.backend.domain.bank.dto.CardHistoryRequest;
import com.akku.backend.domain.bank.dto.CardHistoryResponse;
import com.akku.backend.domain.bank.dto.CardPaymentRequest;
import com.akku.backend.domain.bank.dto.CardProductResponse;
import com.akku.backend.domain.bank.dto.CardResponse;
import com.akku.backend.domain.bank.entity.Card;
import com.akku.backend.domain.bank.entity.CardProduct;
import com.akku.backend.domain.bank.repository.CardProductRepository;
import com.akku.backend.domain.bank.repository.CardRepository;
import com.akku.backend.global.finance.dto.FinanceCardCreateResponse;
import com.akku.backend.global.finance.dto.FinanceCardPaymentResponse;
import com.akku.backend.global.finance.dto.FinanceCardProductListResponse;
import com.akku.backend.global.finance.dto.FinanceCardTransactionHistoryResponse;
import com.akku.backend.global.finance.dto.FinanceUserCardListResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        FinanceCardPaymentResponse.Rec mockResult = new FinanceCardPaymentResponse.Rec(
                1L, "CAT-1", "편의점", 1L, "CU", "20240324", "123456", 10000L
        );
        given(ssafyFinanceService.createCardTransaction(anyString(), anyString(), anyString(), anyLong(), anyLong()))
                .willReturn(mockResult);

        // when
        TransactionSynchronizationManager.initSynchronization();
        try {
            cardService.processPayment(userId, request);

            // 트랜잭션 동기화 리스트에서 afterCommit을 강제로 실행하여 이벤트 발행 확인
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // then
        verify(ssafyFinanceService).createCardTransaction(eq("user-key"), eq("1234"), eq("123"), eq(1L), eq(10000L));
        verify(eventPublisher, times(1)).publishEvent(any(CardPaymentEvent.class));
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

    @Test
    @DisplayName("1. 카드 상품 목록 조회 - 성공")
    void getCardProducts_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("userKey123").build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        FinanceCardProductListResponse.CardProductDetails finProduct = new FinanceCardProductListResponse.CardProductDetails(
                "CARD-123", "001", "BankA", "Good Card", "1", "Credit", 100000L, 50000L, "Discount card", Collections.emptyList()
        );
        given(ssafyFinanceService.getCardProducts("userKey123")).willReturn(List.of(finProduct));
        given(cardProductRepository.findByCardUniqueNo("CARD-123")).willReturn(Optional.empty());

        CardProduct savedProduct = CardProduct.builder().id(UUID.randomUUID()).cardUniqueNo("CARD-123").cardName("Good Card").build();
        given(cardProductRepository.save(any(CardProduct.class))).willReturn(savedProduct);

        given(cardProductRepository.findAll()).willReturn(List.of(savedProduct));

        List<CardProductResponse> products = cardService.getCardProducts(userId);

        assertEquals(1, products.size());
        assertEquals("Good Card", products.get(0).cardName());
        verify(cardProductRepository, times(1)).save(any(CardProduct.class));
    }

    @Test
    @DisplayName("2. 카드 발급 - 성공")
    void createCard_Success() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("userKey123").build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        CardProduct product = CardProduct.builder().id(productId).cardUniqueNo("CARD-123").cardName("Good Card").build();
        given(cardProductRepository.findById(productId)).willReturn(Optional.of(product));

        CardCreateRequest request = new CardCreateRequest(productId, "ACC-123", "10");
        FinanceCardCreateResponse.Rec finRec = new FinanceCardCreateResponse.Rec("9999-9999", "123", "CARD-123", "001", "BankA", "Good Card", "1225", "ACC-123", "10");
        given(ssafyFinanceService.createCard("userKey123", "CARD-123", "ACC-123", "10")).willReturn(finRec);

        Card savedCard = Card.builder().id(UUID.randomUUID()).cardNo("9999-9999").cardProductId(productId).cardProduct(product).isActive(true).build();
        given(cardRepository.save(any(Card.class))).willReturn(savedCard);

        CardResponse response = cardService.createCard(userId, request);

        assertEquals("9999-9999", response.cardNo());
        assertEquals("Good Card", response.cardName());
        verify(ssafyFinanceService, times(1)).createCard("userKey123", "CARD-123", "ACC-123", "10");
    }

    @Test
    @DisplayName("3. 내 보유 카드 조회 - 성공")
    void getMyCards_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("userKey123").build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        FinanceUserCardListResponse.UserCardDetails finCard = new FinanceUserCardListResponse.UserCardDetails(
                "9999-9999", "123", "CARD-123", "001", "BankA", "Good Card", 100000L, 50000L, "Discount card", "1225", "ACC-123", "10"
        );
        given(ssafyFinanceService.getUserCards("userKey123")).willReturn(List.of(finCard));

        given(cardRepository.findAllByUserId(userId)).willReturn(Collections.emptyList(), List.of(
            Card.builder().id(UUID.randomUUID()).cardNo("9999-9999").build()
        ));

        CardProduct product = CardProduct.builder().id(UUID.randomUUID()).cardUniqueNo("CARD-123").build();
        given(cardProductRepository.findByCardUniqueNo("CARD-123")).willReturn(Optional.of(product));

        Card savedCard = Card.builder().id(UUID.randomUUID()).cardNo("9999-9999").build();
        given(cardRepository.save(any(Card.class))).willReturn(savedCard);

        List<CardResponse> myCards = cardService.getMyCards(userId);

        assertEquals(1, myCards.size());
        assertEquals("9999-9999", myCards.get(0).cardNo());
        verify(cardRepository, times(1)).save(any(Card.class));
    }
}
