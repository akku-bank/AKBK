package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.global.finance.dto.FinanceAccountCreateResponse;
import com.akku.backend.global.finance.dto.FinanceAccountListResponse;
import com.akku.backend.global.finance.dto.FinanceTransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final FamilyProfileRepository familyProfileRepository;
    private final AccountRepository accountRepository;
    private final SsafyFinanceService ssafyFinanceService;

    /**
     * 계좌 생성 (부모가 자녀의 계좌를 생성)
     */
    @Transactional
    public AccountCreateResponse createAccount(UUID parentId, AccountCreateRequest request) {
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        
        if (!"PARENT".equals(parent.getRole())) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        User child = userRepository.findById(request.childId())
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 부모와 자녀가 같은 가족 그룹에 속해 있는지 검증
        if (parent.getFamilyId() == null || !parent.getFamilyId().equals(child.getFamilyId())) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        // 금융망 호출 전에 우리 DB에 이미 계좌가 있는지 중복 검증 (자녀 계좌가 이미 개설되었는지 여부 확인)
        boolean hasAccount = accountRepository.existsByUserId(child.getId());
        if (hasAccount) {
            throw new ApiException(BankErrorCode.ALREADY_EXISTS_ACCOUNT);
        }

        // 금융망 API 호출하여 계좌 생성
        FinanceAccountCreateResponse.Rec rec = ssafyFinanceService.createAccount(child.getUserKey(), request.accountType());

        // 우리 DB에 계좌 정보 저장
        Account account = Account.builder()
                .userId(child.getId())
                .accountNumber(rec.accountNo())
                .bankCode(rec.bankCode())
                .type(request.accountType())
                .balance(0L)
                .build();
        
        Account savedAccount = accountRepository.save(account);

        return new AccountCreateResponse(savedAccount.getId(), savedAccount.getBalance());
    }

    /**
     * 내 계좌 목록 조회
     */
    public AccountListResponse getMyAccounts(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        List<FinanceAccountListResponse.AccountDetails> finAccounts = ssafyFinanceService.getAccounts(user.getUserKey());
        
        List<AccountInfo> accounts = finAccounts.stream()
                .map(acc -> new AccountInfo(
                        acc.bankCode(),
                        acc.bankName(),
                        acc.accountNo(),
                        acc.accountName(),
                        acc.accountBalance()
                ))
                .collect(Collectors.toList());
        
        return new AccountListResponse(accounts);
    }

    /**
     * 타행 계좌 연동
     */
    @Transactional
    public void linkExternalAccount(UUID userId, AccountLinkRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 금융망 연동 호출 전 우리 DB 중복 검사
        boolean isAlreadyLinked = accountRepository.existsByAccountNumberAndBankCode(request.accountNumber(), request.bankCode());
        if (isAlreadyLinked) {
             throw new ApiException(BankErrorCode.ALREADY_EXISTS_ACCOUNT);
        }

        // 금융망 API 호출하여 계좌 연동 처리
        ssafyFinanceService.linkAccount(user.getUserKey(), request.bankCode(), request.accountNumber());

        // 우리 DB에도 연동된 계좌 정보 저장
        Account account = Account.builder()
                .userId(user.getId())
                .accountNumber(request.accountNumber())
                .bankCode(request.bankCode())
                .type("EXTERNAL") // 타행 계좌
                .balance(0L) // 실제 잔액은 추후
                .build();

        accountRepository.save(account);
    }

    /**
     * 계좌 이체
     */
    @Transactional
    public TransferResponse transfer(UUID userId, TransferRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // PIN 검증
        if (user.getPinPassword() == null || !user.getPinPassword().equals(request.pin())) {
            throw new ApiException(AuthErrorCode.PIN_MISMATCH);
        }

        // 출금 계좌 조회
        Account withdrawalAccount = accountRepository.findById(UUID.fromString(request.withdrawalAccountId()))
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
        
        if (!withdrawalAccount.getUserId().equals(userId)) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        // 입금 계좌 조회
        Account depositAccount = accountRepository.findById(UUID.fromString(request.targetAccountId()))
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));

        // 잔액 확인
        if (withdrawalAccount.getBalance() < request.amount()) {
            throw new ApiException(BankErrorCode.INSUFFICIENT_BALANCE);
        }

        // 금융망 API 호출
        FinanceTransferResponse.Rec finRec = ssafyFinanceService.transfer(
                user.getUserKey(),
                withdrawalAccount.getBankCode(),
                withdrawalAccount.getAccountNumber(),
                depositAccount.getBankCode(),
                depositAccount.getAccountNumber(),
                request.amount()
        );

        return new TransferResponse(finRec.transactionUniqueNo(), withdrawalAccount.getBalance() - request.amount());
    }
}
