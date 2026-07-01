package com.akku.backend.domain.bank.event;

import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.bank.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardPaymentTransactionEventListenerTest {

    @InjectMocks
    private CardPaymentTransactionEventListener listener;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private com.akku.backend.domain.bank.repository.MerchantRepository merchantRepository;

    @Mock
    private TransactionService transactionService;

    @Test
    @DisplayName("CardPaymentEvent 수신 시 트랜잭션 정상 저장 여부 확인")
    void onCardPayment_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String accountNo = "TEST-ACC-123";
        Account account = mock(Account.class);
        given(account.getId()).willReturn(UUID.randomUUID());

        given(accountRepository.findByAccountNumberAndBankCode(anyString(), eq("999")))
                .willReturn(Optional.of(account));
        given(transactionRepository.existsByTransactionUniqueNo(anyString())).willReturn(false);
        given(merchantRepository.findById(any())).willReturn(Optional.empty());

        CardPaymentEvent event = new CardPaymentEvent(
                userId,
                accountNo,
                "111111",
                "withdraw-123",
                "편의점",
                5000L,
                95000L,
                "1",
                "CU",
                LocalDate.now(),
                "123456",
                false
        );

        // when
        listener.onCardPayment(event);

        // then
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionService).publishTransactionEvent(any(), any(), any());
    }
}
