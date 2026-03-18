package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.TransactionHistoryResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SsafyFinanceService ssafyFinanceService;

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
                            h.transactionSummary().contains("비밀") // 임시
                    ))
                    .collect(Collectors.toList()));
        }

        allTransactions.sort(Comparator.comparing(TransactionHistoryResponse.TransactionInfo::date).reversed());

        return new TransactionHistoryResponse(allTransactions);
    }

    /**
     * 자녀 세부 소비 내역 조회 (부모용)
     * - 숨김 처리된 내역은 "비공개 내역"으로 마스킹
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

        // 숨김 처리된 내역 마스킹
        List<TransactionHistoryResponse.TransactionInfo> maskedTransactions = fullHistory.transactions().stream()
                .map(t -> {
                    if (t.isHidden()) {
                        return new TransactionHistoryResponse.TransactionInfo(
                                t.id(),
                                t.date(),
                                "비공개 내역", // 가맹점명 마스킹
                                t.amount(),   // 금액은 유지
                                true
                        );
                    }
                    return t;
                })
                .collect(Collectors.toList());

        return new TransactionHistoryResponse(maskedTransactions);
    }
}
