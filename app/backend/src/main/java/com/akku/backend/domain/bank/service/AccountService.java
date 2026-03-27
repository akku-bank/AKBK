package com.akku.backend.domain.bank.service;

import java.util.*;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.entity.AccountVerification;
import com.akku.backend.domain.bank.repository.AccountVerificationRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.global.finance.dto.FinanceAccountAuthCheckResponse;
import com.akku.backend.global.finance.dto.FinanceAccountAuthResponse;
import com.akku.backend.global.finance.dto.FinanceAccountCreateResponse;
import com.akku.backend.global.finance.dto.FinanceAccountHolderNameResponse;
import com.akku.backend.global.finance.dto.FinanceAccountListResponse;
import com.akku.backend.global.finance.dto.FinanceTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final FamilyProfileRepository familyProfileRepository;
    private final AccountRepository accountRepository;
    private final AccountVerificationRepository accountVerificationRepository;
    private final SsafyFinanceService ssafyFinanceService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
 
    private static final String DEFAULT_ACCOUNT_TYPE_UNIQUE_NO = "999-1-98000e5fd8024b";

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

        // 금융망 API 호출하여 계좌 생성 (고정 상품 번호 사용)
        FinanceAccountCreateResponse.Rec rec = ssafyFinanceService.createAccount(child.getUserKey(), DEFAULT_ACCOUNT_TYPE_UNIQUE_NO);

        // 우리 DB에 계좌 정보 저장
        Account account = Account.builder()
                .userId(child.getId())
                .accountNumber(rec.accountNo())
                .bankCode(rec.bankCode())
                .type("CASH")
                .balance(0L)
                .isPrimary(true)
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

        // 금융망 계좌 목록 조회
        List<FinanceAccountListResponse.AccountDetails> finAccounts = ssafyFinanceService.getAccounts(user.getUserKey());
        
        // 우리 DB에 등록된 계좌 목록 조회
        List<Account> localAccounts = accountRepository.findAllByUserId(userId);

        // 금융망 계좌와 우리 DB 계좌 매칭하여 DTO 생성
        List<AccountInfo> accounts = finAccounts.stream()
                .map(acc -> {
                    String bankName = acc.bankName();
                    // 은행 코드가 001인 경우 싸피은행으로 표시
                    if ("001".equals(acc.bankCode())) {
                        bankName = "싸피은행";
                    }

                    // 우리 DB에서 매칭되는 계좌 및 주계좌 여부 확인 (계좌번호 매칭 우선)
                    Account matchingLocal = localAccounts.stream()
                            .filter(la -> la.getAccountNumber().equals(acc.accountNo()))
                            .findFirst()
                            .orElse(null);

                    UUID accountId = matchingLocal != null ? matchingLocal.getId() : null;
                    boolean isPrimary = matchingLocal != null && (matchingLocal.getIsPrimary() || localAccounts.size() == 1);
                    long balance = (matchingLocal != null) ? matchingLocal.getBalance() : acc.accountBalance();

                    log.info("[GetMyAccounts] Matching - AccNo: {}, FoundInLocal: {}, Balance: {}, isPrimary: {}", 
                            acc.accountNo(), matchingLocal != null, balance, isPrimary);

                    return new AccountInfo(
                        accountId,
                        acc.bankCode(),
                        bankName,
                        acc.accountNo(),
                        acc.accountName(),
                        balance,
                        isPrimary
                    );
                })
                .sorted((a, b) -> Boolean.compare(b.isPrimary(), a.isPrimary())) // 주계좌 우선 정렬
                .collect(Collectors.toList());

        log.info("[GetMyAccounts] Return {} accounts for user: {}", accounts.size(), userId);
        if (!accounts.isEmpty()) {
            log.info("[GetMyAccounts] Top Account - No: {}, Balance: {}, isPrimary: {}", 
                    accounts.get(0).accountNumber(), accounts.get(0).balance(), accounts.get(0).isPrimary());
        }
        return new AccountListResponse(accounts);
    }

    /**
     * 주계좌 객체 조회 (없으면 자동 생성/등록)
     */
    @Transactional
    public Account getPrimaryAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        return accountRepository.findByUserIdAndIsPrimaryTrue(userId)
                .orElseGet(() -> {
                    List<Account> allLocal = accountRepository.findAllByUserId(userId);
                    if (allLocal.isEmpty()) {
                        List<Account> discovered = discoverAndRegisterAccounts(user);
                        return discovered.isEmpty() ? null : discovered.get(0);
                    }
                    Account firstAcc = allLocal.get(0);
                    firstAcc.designateAsPrimary();
                    return accountRepository.save(firstAcc);
                });
    }

    /**
     * 주계좌 잔액 조회
     */
    @Transactional
    public long getPrimaryAccountBalance(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 주계좌 조회
        Account account = getPrimaryAccount(userId);

        if (account == null) {
            return 0L;
        }

        // 금융망과 잔액 동기화
        try {
            List<FinanceAccountListResponse.AccountDetails> finAccounts = ssafyFinanceService.getAccounts(user.getUserKey());
            long balance = finAccounts.stream()
                    .filter(a -> a.accountNo().equals(account.getAccountNumber()))
                    .findFirst()
                    .map(FinanceAccountListResponse.AccountDetails::accountBalance)
                    .orElse(account.getBalance());
            
            // CASH 계좌의 경우, 금융망 밸런스가 0인데 로컬은 양수인 경우 로컬을 우선적으로 신뢰한다. (Sandbox 환경 대응)
            if (account.getType().equals("CASH") && balance == 0 && account.getBalance() > 0) {
                return account.getBalance();
            }

            account.updateBalance(balance);
            return balance;
        } catch (Exception e) {
            log.warn("금융망 잔액 조회 실패 (UserId: {}, Account: {}): {}", userId, account.getAccountNumber(), e.getMessage());
            return account.getBalance();
        }
    }

    /**
     * 금융망에서 계좌를 조회하여 우리 DB에 등록되어 있지 않으면 자동 등록한다.
     */
    @Transactional
    public List<Account> discoverAndRegisterAccounts(User user) {
        try {
            List<FinanceAccountListResponse.AccountDetails> finAccounts = ssafyFinanceService.getAccounts(user.getUserKey());
            List<Account> registered = new ArrayList<>();
            
            for (FinanceAccountListResponse.AccountDetails fin : finAccounts) {
                boolean exists = accountRepository.existsByAccountNumberAndBankCode(fin.accountNo(), fin.bankCode());
                if (!exists) {
                    Account newAcc = Account.builder()
                            .userId(user.getId())
                            .accountNumber(fin.accountNo())
                            .bankCode(fin.bankCode())
                            .type("CASH")
                            .balance(fin.accountBalance())
                            .isPrimary(registered.isEmpty() && !accountRepository.existsByUserId(user.getId()))
                            .build();
                    registered.add(accountRepository.save(newAcc));
                }
            }
            return registered;
        } catch (Exception e) {
            log.warn("계좌 자동 조회/등록 실패 (UserId: {}): {}", user.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     * 내부 전용 보상 송금 (PIN 검증 없음 — Challenge 도메인에서만 호출)
     *
     * 호출 조건: 부모가 이미 인증된 상태이며, 챌린지 서비스가 REWARD_REQUESTED 상태를
     *           검증한 뒤 위임하는 신뢰된 내부 호출이다.
     *
     * @param parentId        출금 주체(부모) userId
     * @param childId         입금 대상(자녀) userId
     * @param parentAccountId 부모가 선택한 출금 계좌 ID
     * @param childAccountId  자녀의 입금 계좌 ID
     * @param amount          송금 금액
     * @param depositMemo     자녀 통장 적요 (예: "주간 소비 챌린지 보상 (카페)")
     * @param withdrawalMemo  부모 통장 적요 (예: "챌린지 보상 송금 (카페)")
     */
    @Transactional
    public void internalRewardTransfer(UUID parentId, UUID childId,
                                       UUID parentAccountId, UUID childAccountId,
                                       Long amount, String depositMemo, String withdrawalMemo) {
        // 1. 부모 유저 조회 (금융망 userKey 확보)
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 2. 부모/자녀 계좌 조회 및 비관적 락 획득 (데드락 방지를 위해 ID 순서로 락 획득 고려할 수 있으나, 여기서는 비즈니스 흐름상 부모->자녀 고정)
        // 먼저 부모 계좌 획득
        Account parentAccount = accountRepository.findByIdWithLock(parentAccountId)
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
        if (!parentAccount.getUserId().equals(parentId)) {
            throw new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 자녀 계좌 획득
        Account childAccount = accountRepository.findByIdWithLock(childAccountId)
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
        if (!childAccount.getUserId().equals(childId)) {
            throw new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 3. 로컬 잔액 검증 및 차감/증가 (동기화)
        parentAccount.deductBalance(amount);
        childAccount.addBalance(amount);

        // 4. 금융망 이체 API 호출 (실패 시 RuntimeException → @Transactional 롤백됨)
        ssafyFinanceService.transfer(
                parent.getUserKey(),
                parentAccount.getBankCode(),
                parentAccount.getAccountNumber(),
                childAccount.getBankCode(),
                childAccount.getAccountNumber(),
                amount,
                depositMemo,
                withdrawalMemo
        );
    }

    /**
     * 주계좌 지정 — 기존 주계좌를 해제하고 새 계좌를 주계좌로 설정한다.
     */
    @Transactional
    public void setPrimaryAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.getUserId().equals(userId)) {
            throw new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND);
        }

        accountRepository.findByUserIdAndIsPrimaryTrue(userId)
                .ifPresent(Account::revokePrimary);

        account.designateAsPrimary();
    }

    /**
     * 계좌 이체
     */
    @Transactional
    public TransferResponse transfer(UUID userId, TransferRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // PIN 검증
        if (user.getPinPassword() == null || !passwordEncoder.matches(request.pin(), user.getPinPassword())) {
            throw new ApiException(AuthErrorCode.PIN_MISMATCH);
        }

        // 출금 계좌 조회
        Account withdrawalAccount = accountRepository.findById(UUID.fromString(request.withdrawalAccountId()))
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
        
        if (!withdrawalAccount.getUserId().equals(userId)) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        // 잔액 확인 (금융망 실시간 잔액 조회 루틴 추가)
        long currentBalance = withdrawalAccount.getBalance();
        // 실시간 잔액 동기화 (금융망 API 호출)
        try {
            List<FinanceAccountListResponse.AccountDetails> finAccounts = ssafyFinanceService.getAccounts(user.getUserKey());
            currentBalance = finAccounts.stream()
                    .filter(a -> a.accountNo().equals(withdrawalAccount.getAccountNumber()))
                    .findFirst()
                    .map(FinanceAccountListResponse.AccountDetails::accountBalance)
                    .orElse(withdrawalAccount.getBalance());
            withdrawalAccount.updateBalance(currentBalance);
        } catch (Exception e) {
            log.warn("금융망 잔액 동기화 실패: {}", e.getMessage());
        }

        if (currentBalance < request.amount()) {
            throw new ApiException(BankErrorCode.INSUFFICIENT_BALANCE);
        }

        // 입금 계좌 여부 확인
        Account depositAccount = accountRepository.findByAccountNumberAndBankCode(request.targetAccountNumber(), request.targetBankCode())
                .orElse(null);

        // 적요 설정 (이름 기반)
        String depositMemo = user.getName(); // 입금자(나)의 이름이 상대방 통장에 찍힘
        String withdrawalMemo = request.targetName(); // 수금자(상대)의 이름이 내 통장에 찍힘

        // 금융망 API 호출
        FinanceTransferResponse.Rec finRec = ssafyFinanceService.transfer(
                user.getUserKey(),
                withdrawalAccount.getBankCode(),
                withdrawalAccount.getAccountNumber(),
                request.targetBankCode(),
                request.targetAccountNumber(),
                request.amount(),
                depositMemo,
                withdrawalMemo
        );

        // 우리 DB 잔액 업데이트
        withdrawalAccount.deductBalance(request.amount());
        if (depositAccount != null) {
            depositAccount.addBalance(request.amount());
        }

        return new TransferResponse(finRec.transactionUniqueNo(), withdrawalAccount.getBalance());
    }

    /**
     * 1원 송금 인증 요청
     */
    @Transactional
    public AccountVerifyResponse request1WonVerification(UUID userId, AccountVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 이미 연동된 계좌인지 확인
        boolean isAlreadyLinked = accountRepository.existsByAccountNumberAndBankCode(request.accountNumber(), request.bankCode());
        if (isAlreadyLinked) {
            throw new ApiException(BankErrorCode.ALREADY_EXISTS_ACCOUNT);
        }

        // 기존 동일 계좌 인증 정보 삭제 (새로운 요청으로 갱신)
        accountVerificationRepository.deleteAllByUserIdAndAccountNumberAndBankCode(user.getId(), request.accountNumber(), request.bankCode());

        // 1원 송급 요청 (기업명 "SSAFY"로 고정하여 요청)
        String authText = "SSAFY";
        ssafyFinanceService.openAccountAuth(user.getUserKey(), request.accountNumber(), authText);

        // 거래 내역 조회를 통해 금융 API가 생성한 인증 코드 추출 (폴링 로직)
        String authCode = null;
        String dateStr = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        for (int i = 0; i < 3; i++) {
            try {
                // 약간의 지연 후 거래 내역 조회 (금융망 반영 시간 고려)
                Thread.sleep(300); 
                
                List<FinanceTransactionHistoryResponse.TransactionDetails> history = 
                    ssafyFinanceService.getTransactionHistory(user.getUserKey(), request.accountNumber(), dateStr, dateStr);
                
                // 최근 거래 내역 중 기업명으로 시작하는 1원 입금 건 찾기
                authCode = history.stream()
                    .filter(tx -> "1".equals(tx.transactionBalance())) // 1원 입금
                    .filter(tx -> tx.transactionSummary().startsWith(authText)) // 기업명 일치
                    .map(tx -> {
                        // "SSAFY1234" 등에서 숫자 4자리 추출
                        Pattern pattern = Pattern.compile("\\d{4}");
                        Matcher matcher = pattern.matcher(tx.transactionSummary());
                        return matcher.find() ? matcher.group() : null;
                    })
                    .filter(code -> code != null)
                    .findFirst()
                    .orElse(null);

                if (authCode != null) break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (authCode == null) {
            throw new ApiException(BankErrorCode.INVALID_AUTH_CODE);
        }

        // 3. 인증 정보 저장 (실제 금융 API 코드로 저장)
        AccountVerification verification = AccountVerification.builder()
                .userId(user.getId())
                .accountNumber(request.accountNumber())
                .bankCode(request.bankCode())
                .authCode(authCode)
                .authText(authText)
                .status("PENDING")
                .expiresAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();
        
        accountVerificationRepository.save(verification);

        return new AccountVerifyResponse(authCode);
    }

    /**
     * 1원 송금 인증 확인 및 계좌 연동
     */
    @Transactional
    public AccountLinkResponse verifyAccountAndLink(UUID userId, AccountVerifyConfirmRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // DB에서 인증 정보 조회
        AccountVerification verification = accountVerificationRepository.findTopByUserIdAndAccountNumberAndBankCodeOrderByCreatedAtDesc(
                        user.getId(), request.accountNumber(), request.bankCode())
                .orElseThrow(() -> new ApiException(BankErrorCode.INVALID_AUTH_CODE));

        if (verification.isExpired()) {
            throw new ApiException(BankErrorCode.EXPIRED_PAYMENT_TOKEN);
        }

        // 금융망 API 호출하여 코드 검증
        FinanceAccountAuthCheckResponse.Rec verifyRec = ssafyFinanceService.checkAuthCode(
                user.getUserKey(), 
                request.accountNumber(), 
                verification.getAuthText(), 
                request.authCode()
        );

        if (!"SUCCESS".equals(verifyRec.status())) {
            throw new ApiException(BankErrorCode.INVALID_AUTH_CODE);
        }

        // 우리 DB 중복 검사
        boolean isAlreadyLinked = accountRepository.existsByAccountNumberAndBankCode(request.accountNumber(), request.bankCode());
        if (isAlreadyLinked) {
            throw new ApiException(BankErrorCode.ALREADY_EXISTS_ACCOUNT);
        }

        // 초기 잔액 조회 (금융망)
        List<FinanceAccountListResponse.AccountDetails> finAccounts = ssafyFinanceService.getAccounts(user.getUserKey());
        long initialBalance = finAccounts.stream()
                .filter(a -> a.accountNo().equals(request.accountNumber()))
                .findFirst()
                .map(FinanceAccountListResponse.AccountDetails::accountBalance)
                .orElse(0L);

        // 인증 성공 시 계좌 연동 (우리 DB 저장)
        Account account = Account.builder()
                .userId(user.getId())
                .accountNumber(request.accountNumber())
                .bankCode(request.bankCode())
                .type("EXTERNAL")
                .balance(initialBalance)
                .isPrimary(true) // 부모의 경우 첫 연동 계좌를 주계좌로 설정
                .build();

        Account savedAccount = accountRepository.save(account);

        // 사용 완료된 인증 정보 삭제
        accountVerificationRepository.deleteAllByUserIdAndAccountNumberAndBankCode(user.getId(), request.accountNumber(), request.bankCode());

        // 은행 이름 매핑
        String bankName = getBankName(request.bankCode());

        return new AccountLinkResponse(savedAccount.getId(), bankName);
    }

    /**
     * 계좌 실명 조회
     */
    public String getAccountHolderName(String bankCode, String accountNumber) {
        // 우리 DB에서 검색
        Optional<Account> localAccount = accountRepository.findByAccountNumberAndBankCode(accountNumber, bankCode);
        if (localAccount.isPresent()) {
            return userRepository.findById(localAccount.get().getUserId())
                    .map(User::getName)
                    .orElse("알 수 없음");
        }

        // 금융망 API 조회
        try {
            String systemUserKey = userRepository.findAll().get(0).getUserKey(); 
            FinanceAccountHolderNameResponse res = ssafyFinanceService.inquireDemandDepositAccountHolderName(systemUserKey, accountNumber);
            return res.userName();
        } catch (Exception e) {
            log.error("금융망 계좌 실명 조회 실패: {}", e.getMessage());
            return "외부 계좌 사용자"; 
        }
    }

    private String getBankName(String bankCode) {
        return switch (bankCode) {
            case "001" -> "한국은행";
            case "002" -> "산업은행";
            case "003" -> "기업은행";
            case "004" -> "국민은행";
            case "011" -> "농협은행";
            case "020" -> "우리은행";
            case "023" -> "SC제일은행";
            case "027" -> "시티은행";
            case "032" -> "대구은행";
            case "034" -> "광주은행";
            case "035" -> "제주은행";
            case "037" -> "전북은행";
            case "039" -> "경남은행";
            case "045" -> "새마을금고";
            case "081" -> "KEB하나은행";
            case "088" -> "신한은행";
            case "090" -> "카카오뱅크";
            case "999" -> "싸피은행";
            default -> "기타은행";
        };
    }
}
