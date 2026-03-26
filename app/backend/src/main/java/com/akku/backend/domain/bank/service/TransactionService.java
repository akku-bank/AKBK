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
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                    String place = (h.transactionMemo() != null && !h.transactionMemo().trim().isEmpty()) 
                            ? h.transactionMemo() : h.transactionSummary();

                    Transaction tx = Transaction.builder()
                            .accountId(account.getId())
                            .transactionUniqueNo(h.transactionUniqueNo())
                            .amount(Long.parseLong(h.transactionBalance()))
                            .balanceAfter(Long.parseLong(h.transactionAfterBalance()))
                            .transactionType(h.transactionType())
                            .merchantName(place)
                            .date(h.transactionDate() + h.transactionTime())
                            .isHidden(false) // 초기값
                            .build();
                    transactionRepository.save(tx);
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
                                "비공개 내역 🤫",
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
}


