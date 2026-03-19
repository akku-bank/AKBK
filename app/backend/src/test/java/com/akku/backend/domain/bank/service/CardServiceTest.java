package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.CardCreateRequest;
import com.akku.backend.domain.bank.dto.CardProductResponse;
import com.akku.backend.domain.bank.dto.CardResponse;
import com.akku.backend.domain.bank.entity.Card;
import com.akku.backend.domain.bank.entity.CardProduct;
import com.akku.backend.domain.bank.repository.CardProductRepository;
import com.akku.backend.domain.bank.repository.CardRepository;
import com.akku.backend.global.finance.dto.FinanceCardCreateResponse;
import com.akku.backend.global.finance.dto.FinanceCardProductListResponse;
import com.akku.backend.global.finance.dto.FinanceUserCardListResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
