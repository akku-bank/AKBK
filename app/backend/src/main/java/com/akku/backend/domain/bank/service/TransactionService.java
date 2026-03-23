package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.TransactionHistoryResponse;
import com.akku.backend.domain.bank.dto.TransactionVisibilityRequest;
import com.akku.backend.domain.bank.dto.TransactionVisibilityResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.event.DepositEvent;
import com.akku.backend.domain.bank.event.PaymentEvent;
import com.akku.backend.domain.bank.event.TransferEvent;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
=======
import java.util.*;
>>>>>>> 8c55485b64c2b396adb77a380256a88eb0b827c4
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SsafyFinanceService ssafyFinanceService;
    private final TransactionRepository transactionRepository;

    // ── Kafka 파이프라인용 저장 메서드 ──────────────────────────────────────────

    /**
     * PAYMENT 이벤트를 DB에 저장한다.
     *
     * @return 저장된 Transaction. 이미 처리된 eventId인 경우 {@link Optional#empty()} 반환.
     */
    @Transactional
    public Optional<Transaction> savePayment(PaymentEvent event) {
        if (transactionRepository.existsByEventId(event.eventId())) {
            log.warn("PAYMENT 이벤트 중복 수신, skip - eventId: {}", event.eventId());
            return Optional.empty();
        }
        Transaction saved = transactionRepository.save(Transaction.fromPayment(event));
        log.info("PAYMENT 거래 저장 완료 - eventId: {}, userId: {}, amount: {}",
                event.eventId(), event.userId(), event.amount());
        return Optional.of(saved);
    }

    /**
     * TRANSFER 이벤트를 DB에 저장한다.
     *
     * @return 저장된 Transaction. 이미 처리된 eventId인 경우 {@link Optional#empty()} 반환.
     */
    @Transactional
    public Optional<Transaction> saveTransfer(TransferEvent event) {
        if (transactionRepository.existsByEventId(event.eventId())) {
            log.warn("TRANSFER 이벤트 중복 수신, skip - eventId: {}", event.eventId());
            return Optional.empty();
        }
        Transaction saved = transactionRepository.save(Transaction.fromTransfer(event));
        log.info("TRANSFER 거래 저장 완료 - eventId: {}, userId: {}, amount: {}",
                event.eventId(), event.userId(), event.amount());
        return Optional.of(saved);
    }

    /**
     * DEPOSIT 이벤트를 DB에 저장한다.
     *
     * @return 저장된 Transaction. 이미 처리된 eventId인 경우 {@link Optional#empty()} 반환.
     */
    @Transactional
    public Optional<Transaction> saveDeposit(DepositEvent event) {
        if (transactionRepository.existsByEventId(event.eventId())) {
            log.warn("DEPOSIT 이벤트 중복 수신, skip - eventId: {}", event.eventId());
            return Optional.empty();
        }
        Transaction saved = transactionRepository.save(Transaction.fromDeposit(event));
        log.info("DEPOSIT 거래 저장 완료 - eventId: {}, userId: {}, amount: {}",
                event.eventId(), event.userId(), event.amount());
        return Optional.of(saved);
    }

    // ── 조회 메서드 ───────────────────────────────────────────────────────────

    public TransactionHistoryResponse getTransactionHistory(UUID userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        List<Account> accounts = accountRepository.findAllByUserId(userId);
        
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        
        String startDate = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<TransactionHistoryResponse.TransactionInfo> allTransactions = new ArrayList<>();

        for (Account account : accounts) {
            List<FinanceTransactionHistoryResponse.TransactionDetails> finHistory = 
                    ssafyFinanceService.getTransactionHistory(user.getUserKey(), account.getAccountNumber(), startDate, endDate);
            
            allTransactions.addAll(finHistory.stream()
                    .map(h -> new TransactionHistoryResponse.TransactionInfo(
                            h.transactionUniqueNo(),
                            h.transactionDate() + h.transactionTime(),
                            h.transactionSummary(),
                            h.transactionType().equals("1") ? Long.parseLong(h.transactionBalance()) : -Long.parseLong(h.transactionBalance()),
                            user.getIsHidden() // 모든 내역에 대해 유저의 글로벌 설정 적용
                    ))
                    .collect(Collectors.toList()));
        }

        allTransactions.sort(Comparator.comparing(TransactionHistoryResponse.TransactionInfo::date).reversed());

        return new TransactionHistoryResponse(allTransactions);
    }

    /**
     * 자녀 세부 소비 내역 조회 (부모용)
     * - 자녀의 글로벌 설정(isHidden)이 true인 경우 모든 가맹점명 마스킹
     */
    public TransactionHistoryResponse getChildTransactionHistory(UUID parentId, UUID childId, int year, int month) {
        // 가족 관계 검증
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        if (parent.getFamilyId() == null || !parent.getFamilyId().equals(child.getFamilyId())
                || !"PARENT".equals(parent.getRole()) || !"CHILD".equals(child.getRole())) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }
        
        // 자녀의 전체 내역 조회
        TransactionHistoryResponse fullHistory = getTransactionHistory(childId, year, month);

        // 자녀가 비공개 모드인 경우 모든 가맹점명 마스킹
        if (Boolean.TRUE.equals(child.getIsHidden())) {
            List<TransactionHistoryResponse.TransactionInfo> maskedTransactions = fullHistory.transactions().stream()
                    .map(t -> new TransactionHistoryResponse.TransactionInfo(
                            t.id(),
                            t.date(),
                            "비공개 내역", // 전체 마스킹
                            t.amount(),
                            true
                    ))
                    .collect(Collectors.toList());
            return new TransactionHistoryResponse(maskedTransactions);
        }

        return fullHistory;
    }

    @Transactional
    public TransactionVisibilityResponse updateGlobalVisibility(UUID userId, TransactionVisibilityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        user.updateIsHidden(request.isHidden());
        userRepository.save(user);

        return new TransactionVisibilityResponse(user.getIsHidden());
    }
}


