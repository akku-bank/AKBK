package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.TransactionHistoryResponse;
import com.akku.backend.domain.bank.dto.TransactionVisibilityRequest;
import com.akku.backend.domain.bank.dto.TransactionVisibilityResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.entity.Merchant;
import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.MerchantRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.akku.backend.domain.bank.event.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SsafyFinanceService ssafyFinanceService;
    private final AccountService accountService;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final MerchantRepository merchantRepository;

    public TransactionHistoryResponse getTransactionHistory(UUID userId, Integer year, Integer month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        List<Account> accounts = accountRepository.findAllByUserId(userId);
        if (accounts.isEmpty()) {
            accounts = accountService.discoverAndRegisterAccounts(user);
        }

        LocalDate startStr = LocalDate.of(year, month, 1);
        LocalDate endStr = startStr.withDayOfMonth(startStr.lengthOfMonth());
        String startDate = startStr.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = endStr.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<TransactionHistoryResponse.TransactionInfo> allTxInfos = new ArrayList<>();

        for (Account account : accounts) {
            // 금융망에서 최신 내역 가져오기
            List<FinanceTransactionHistoryResponse.TransactionDetails> finHistory = 
                    ssafyFinanceService.getTransactionHistory(user.getUserKey(), account.getAccountNumber(), startDate, endDate);

            // 새로운 내역 DB에 저장 (동기화)
            for (FinanceTransactionHistoryResponse.TransactionDetails h : finHistory) {
                if (!transactionRepository.existsByTransactionUniqueNo(h.transactionUniqueNo())) {
                    // 수신/발신자 이름 처리 로직 개선
                    String place = (h.transactionMemo() != null && !h.transactionMemo().trim().isEmpty()) 
                            ? h.transactionMemo().trim() : h.transactionSummary().trim();
                    
                    // 만약 이체 내역인데 적요가 비어있거나 제네릭한 경우, 계좌 번호로 실명 조회 시도
                    if (("송금".equals(place) || "이체".equals(place) || "입금".equals(place) || "출금".equals(place) || "출금(이체)".equals(place) || "입금(이체)".equals(place))
                            && h.transactionAccountNo() != null && !h.transactionAccountNo().isEmpty()) {
                        try {
                            // 상대방 계좌 번호로 실명 조회
                            String holderName = accountService.getAccountHolderName("999", h.transactionAccountNo());
                            if (!"알 수 없음".equals(holderName) && !"외부 계좌 사용자".equals(holderName)) {
                                place = holderName;
                            }
                        } catch (Exception e) {
                            log.warn("실명 조회 패스: {}", e.getMessage());
                        }
                    }

                    Optional<Merchant> merchantOpt = merchantRepository.findByMerchantName(place);

                    Transaction tx = Transaction.builder()
                            .accountId(account.getId())
                            .transactionUniqueNo(h.transactionUniqueNo())
                            .amount(Long.parseLong(h.transactionBalance()))
                            .balanceAfter(Long.parseLong(h.transactionAfterBalance()))
                            .transactionType(h.transactionType())
                            .merchantId(merchantOpt.map(Merchant::getMerchantId).orElse(null))
                            .subCategoryId(merchantOpt.map(Merchant::getSubCategoryId).orElse(null))
                            .subCategoryName(merchantOpt.map(Merchant::getSubCategoryName).orElse(place))
                            .merchantName(place)
                            .date(h.transactionDate() + h.transactionTime())
                            .isHidden(false) // 초기값
                            .build();
                    transactionRepository.save(tx);
                    publishTransactionEvent(tx, userId, account);
                } else {
                    // 이미 존재하는 내역이라도 merchantName이 비어있거나 "결제 내역"인 경우 업데이트 시도
                    Transaction existingTx = transactionRepository.findByTransactionUniqueNo(h.transactionUniqueNo()).orElse(null);
                    if (existingTx != null && (existingTx.getMerchantName() == null || existingTx.getMerchantName().trim().isEmpty() 
                            || "결제 내역".equals(existingTx.getMerchantName()) || "입금 내역".equals(existingTx.getMerchantName()))) {
                            
                            String place = (h.transactionMemo() != null && !h.transactionMemo().trim().isEmpty()) 
                                    ? h.transactionMemo().trim() : h.transactionSummary().trim();
                            
                            // 이체/송금 관련 키워드 포함 시 실명 조회 시도
                            boolean isTransfer = place.contains("송금") || place.contains("이체") || place.contains("입금") || place.contains("출금");
                            
                            if (isTransfer && h.transactionAccountNo() != null && !h.transactionAccountNo().isEmpty()) {
                                try {
                                    String holderName = accountService.getAccountHolderName("999", h.transactionAccountNo());
                                    if (!"알 수 없음".equals(holderName) && !"외부 계좌 사용자".equals(holderName)) {
                                        place = holderName;
                                    }
                                } catch (Exception e) {}
                            }

                            if (!place.equals(existingTx.getMerchantName())) {
                                existingTx.updateMerchantName(place);
                                if (existingTx.getSubCategoryName() == null || existingTx.getSubCategoryName().trim().isEmpty() || "결제 내역".equals(existingTx.getSubCategoryName())) {
                                    existingTx.updateSubCategoryName(place);
                                }
                                transactionRepository.save(existingTx);
                            }
                        }
                    }
            }

            // 우리 DB에서 해당 기간 내역 조회
            List<Transaction> localTxs = transactionRepository.findAllByAccountIdAndDateBetween(
                    account.getId(), 
                    startDate + "000000", 
                    endDate + "235959",
                    Sort.by(Sort.Direction.DESC, "date")
            );

            allTxInfos.addAll(localTxs.stream()
                    .map(t -> new TransactionHistoryResponse.TransactionInfo(
                        t.getTransactionUniqueNo(),
                        t.getDate(),
                        t.getPlace(),
                        t.getAmount(),
                        t.getTransactionType().equals("1"), // 1: 입금
                        t.getBalanceAfter(),
                        t.getIsHidden(), // 개별 숨김 상태 반영
                        t.getMemo()
                    ))
                    .collect(Collectors.toList()));
        }

        allTxInfos.sort(Comparator.comparing(TransactionHistoryResponse.TransactionInfo::date).reversed());
        long balance = accountService.getPrimaryAccountBalance(userId);

        return new TransactionHistoryResponse(balance, allTxInfos);
    }

    /**
     * 자녀 세부 소비 내역 조회 (부모용)
     * - 자녀의 글로벌 설정(isHidden)이 true인 경우 모든 가맹점명 마스킹
     */
    public TransactionHistoryResponse getChildTransactionHistory(UUID parentId, UUID childId, Integer year, Integer month) {
        // 가족 관계 검증
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        if (parent.getFamilyId() == null || !parent.getFamilyId().equals(child.getFamilyId())
                || !"PARENT".equals(parent.getRole()) || !"CHILD".equals(child.getRole())) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }
        
        // 자녀의 전체 내역 조회 (DB 동기화 포함)
        TransactionHistoryResponse fullHistory = getTransactionHistory(childId, year, month);

        // 개별 숨김 처리된 내역 마스킹
        List<TransactionHistoryResponse.TransactionInfo> processedTransactions = fullHistory.transactions().stream()
                .map(t -> {
                    if (t.isHidden()) {
                        return new TransactionHistoryResponse.TransactionInfo(
                                t.id(),
                                t.date(),
                                "********",
                                t.amount(),
                                t.isIncome(),
                                t.balanceAfter(),
                                true,
                                "" // 메모도 숨김
                        );
                    }
                    return t;
                })
                .collect(Collectors.toList());

        return new TransactionHistoryResponse(fullHistory.balance(), processedTransactions);
    }

    @Transactional
    public TransactionVisibilityResponse updateTransactionVisibility(UUID userId, String transactionUniqueNo, TransactionVisibilityRequest request) {
        Transaction transaction = transactionRepository.findByTransactionUniqueNo(transactionUniqueNo)
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND)); 

        // 계좌 소유주 확인
        Account account = accountRepository.findById(transaction.getAccountId())
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
        
        if (!account.getUserId().equals(userId)) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        transaction.updateVisibility(request.isHidden());
        transactionRepository.save(transaction);

        return new TransactionVisibilityResponse(transaction.getIsHidden());
    }

    @Transactional
    public void updateTransactionMemo(UUID userId, String transactionUniqueNo, String memo) {
        Transaction transaction = transactionRepository.findByTransactionUniqueNo(transactionUniqueNo)
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));

        Account account = accountRepository.findById(transaction.getAccountId())
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        transaction.updateMemo(memo);
        transactionRepository.save(transaction);
    }

    @Transactional
    public TransactionVisibilityResponse updateGlobalVisibility(UUID userId, TransactionVisibilityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        user.updateIsHidden(request.isHidden());
        userRepository.save(user);

        return new TransactionVisibilityResponse(user.getIsHidden());
    }

    /**
     * Kafka로 거래 이벤트 발행
     */
    public void publishTransactionEvent(Transaction tx, UUID userId, Account account) {
        try {
            String mappedTransactionType = mapTransactionType(tx.getTransactionType());

            TransactionEvent event = new TransactionEvent(
                    "TRANSACTION_COMPLETED",
                    new TransactionEvent.Data(
                            tx.getId().toString(),
                            userId.toString(),
                            account.getId().toString(),
                            tx.getMerchantId(),
                            tx.getAmount(),
                            mappedTransactionType,
                            tx.getSubCategoryId(),
                            tx.getSubCategoryName(),
                            tx.getMerchantName(),
                            tx.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    )
            );

            kafkaTemplate.send("transaction", tx.getId().toString(), event);
            log.info("✅ Kafka transaction event published: {} | Type: {} | Amount: {}",
                    tx.getId(), mappedTransactionType, tx.getAmount());

        } catch (Exception e) {
            log.error("❌ Failed to publish transaction event to Kafka", e);
        }
    }

    private String mapTransactionType(String financialType) {
        if (financialType == null) {
            return "SPEND";
        }
        if ("1".equals(financialType)) {
            return "INCOME";
        }
        return "SPEND";
    }
}


