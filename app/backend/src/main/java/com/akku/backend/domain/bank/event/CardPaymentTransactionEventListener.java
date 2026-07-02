package com.akku.backend.domain.bank.event;

import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.entity.Merchant;
import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.MerchantRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentTransactionEventListener {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCardPayment(CardPaymentEvent event) {
        log.info("📢 CardPaymentEvent 수신 성공! — userId: {}, uniqueNo: {}, merchant: {}", 
                event.userId(), event.transactionUniqueNo(), event.merchantName());

        // 중복 저장 방지
        // withdrawalUniqueNo(금융망 이체 ID)가 있으면 그것을 우선 사용, 없으면 Card API ID 사용
        String uniqueNo = (event.withdrawalUniqueNo() != null) ? event.withdrawalUniqueNo() : event.transactionUniqueNo();
        
        if (transactionRepository.existsByTransactionUniqueNo(uniqueNo)) {
            log.warn("⚠️ 이미 저장된 거래 내역입니다. (중복 방지) uniqueNo: {}", uniqueNo);
            return;
        }

        // 계좌 정보 조회 (bankCode 999 고정 - 싸피은행)
        // 계좌 번호 형식 차이 방지를 위해 정규화 수행
        String normalizedAccountNo = event.withdrawalAccountNo().replace("-", "");
        Account account = accountRepository.findByAccountNumberAndBankCode(normalizedAccountNo, "999")
                .orElseGet(() -> {
                    // 원본으로도 한 번 더 시도
                    return accountRepository.findByAccountNumberAndBankCode(event.withdrawalAccountNo(), "999")
                            .orElse(null);
                });

        if (account == null) {
            log.error("❌ 계좌를 찾을 수 없습니다. (조회 시도: {}, {}) DB의 accounts 테이블을 확인해주세요.", 
                    normalizedAccountNo, event.withdrawalAccountNo());
            return;
        }
        log.info("✅ 계좌 조회 성공: accountId={}", account.getId());

        // 가맹점 정보 조회 (subCategoryId 매핑용)
        Optional<Merchant> merchantOpt = merchantRepository.findById(Long.parseLong(event.merchantId()));

        // 트랜잭션 엔티티 생성 및 저장
        Transaction tx = Transaction.builder()
                .accountId(account.getId())
                .transactionUniqueNo(uniqueNo)
                .amount(event.paymentBalance())
                .balanceAfter(event.balanceAfter()) // 잔액 정보 추가
                .transactionType("2") // 2: 출금 (결제)
                .merchantId(merchantOpt.map(Merchant::getMerchantId).orElse(null))
                .subCategoryId(merchantOpt.map(Merchant::getSubCategoryId).orElse(null))
                .subCategoryName(merchantOpt.map(Merchant::getSubCategoryName).orElse(event.subCategoryName()))
                .merchantName(event.merchantName())
                .date(event.transactionDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + event.transactionTime())
                .isHidden(false)
                .build();

        transactionRepository.save(tx);
        log.info("카드 결제 내역 실시간 반영 완료 — txId: {}, uniqueNo: {}", tx.getId(), uniqueNo);

        // Kafka 이벤트 발행 (실시간 소비 동기화)
        transactionService.publishTransactionEvent(tx, event.userId(), account);
    }
}
