package com.akku.backend.domain.auth.service;

import com.akku.backend.global.finance.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class SsafyFinanceService {

    private final RestClient restClient;
    private final String apiKey;

    public SsafyFinanceService(
            @Value("${ssafy.api.base-url}") String baseUrl,
            @Value("${ssafy.api.key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * 금융망 공통 헤더 생성
     */
    private FinanceRequestHeader createHeader(String apiName, String apiServiceCode, String userKey) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        
        // 기관 거래 고유 번호
        String uniqueNo = date + time + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        return FinanceRequestHeader.builder()
                .apiName(apiName)
                .transmissionDate(date)
                .transmissionTime(time)
                .institutionCode("00100") 
                .fintechAppNo("001") 
                .apiServiceCode(apiServiceCode)
                .institutionTransactionUniqueNo(uniqueNo)
                .apiKey(apiKey)
                .userKey(userKey)
                .build();
    }

    public String createMember(String userId) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("apiKey", apiKey);
            body.put("userId", userId);

            log.info("금융망 회원 생성 요청 - userId: {}", userId);
            Map<String, Object> response = restClient.post()
                    .uri("/member")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            log.info("금융망 회원 생성 응답: {}", response);

            if (response != null && response.containsKey("userKey")) {
                return (String) response.get("userKey");
            }

            throw new RuntimeException("금융망 응답에 userKey 없음");

        } catch (HttpClientErrorException e) {
            // 이미 존재하는 유저인 경우
            if (e.getResponseBodyAsString().contains("E4002")) {
                log.info("금융망에 이미 등록된 유저입니다. 조회를 통해 userKey를 가져옵니다. userId: {}", userId);
                return searchMember(userId);
            }
            throw e;
        }
    }

    private String searchMember(String userId) {
        Map<String, String> body = new HashMap<>();
        body.put("apiKey", apiKey);
        body.put("userId", userId);

        log.info("금융망 회원 조회 요청 - userId: {}", userId);
        Map<String, Object> response = restClient.post()
                .uri("/member/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        log.info("금융망 회원 조회 응답: {}", response);

        if (response != null && response.containsKey("userKey")) {
            return (String) response.get("userKey");
        }
        throw new RuntimeException("금융망 계정 조회 실패");
    }

    /**
     * 계좌 생성
     * @param userKey 금융망 사용자 키
     * @param accountTypeUniqueNo 상품 고유번호
     */
    public FinanceAccountCreateResponse.Rec createAccount(String userKey, String accountTypeUniqueNo) {
        FinanceRequestHeader header = createHeader("createDemandDepositAccount", "createDemandDepositAccount", userKey);
        FinanceAccountCreateRequest data = new FinanceAccountCreateRequest(accountTypeUniqueNo);
        
        FinanceRequest<FinanceAccountCreateRequest> request = new FinanceRequest<>(header, data);

        FinanceResponse<FinanceAccountCreateResponse> response = restClient.post()
                .uri("/edu/demandDeposit/createDemandDepositAccount")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        validateResponse(response);
        if (response != null && response.data() != null && response.data().rec() != null) {
            return response.data().rec();
        }
        
        throw new RuntimeException("금융망 계좌 생성 실패");
    }

    /**
     * 계좌 목록 조회
     */
    public List<FinanceAccountListResponse.AccountDetails> getAccounts(String userKey) {
        FinanceRequestHeader header = createHeader("inquireDemandDepositAccountList", "inquireDemandDepositAccountList", userKey);
        
        FinanceRequest<Map<String, String>> request = new FinanceRequest<>(header, Map.of());

        FinanceResponse<FinanceAccountListResponse> response = restClient.post()
                .uri("/edu/demandDeposit/inquireDemandDepositAccountList")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        validateResponse(response);
        if (response != null && response.data() != null && response.data().rec() != null) {
            return response.data().rec();
        }
        
        return Collections.emptyList();
    }

    /**
     * 계좌 이체
     */
    public FinanceTransferResponse.Rec transfer(String userKey, String withdrawalBankCode, String withdrawalAccountNo, String depositBankCode, String depositAccountNo, Long amount) {
        FinanceRequestHeader header = createHeader("createDemandDepositAccountTransfer", "createDemandDepositAccountTransfer", userKey);
        
        FinanceTransferRequest data = new FinanceTransferRequest(
                depositBankCode, // 입금은행
                depositAccountNo,
                amount,
                withdrawalBankCode, // 출금은행
                withdrawalAccountNo,
                "송금",
                "이체"
        );

        FinanceRequest<FinanceTransferRequest> request = new FinanceRequest<>(header, data);

        FinanceResponse<FinanceTransferResponse> response = restClient.post()
                .uri("/edu/demandDeposit/updateDemandDepositAccountTransfer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        validateResponse(response);
        if (response != null && response.data() != null && response.data().rec() != null && !response.data().rec().isEmpty()) {
            return response.data().rec().get(0);
        }

        throw new RuntimeException("금융망 이체 처리 실패");
    }

    /**
     * 계좌 거래 내역 조회
     */
    public List<FinanceTransactionHistoryResponse.TransactionDetails> getTransactionHistory(
            String userKey, String accountNo, String startDate, String endDate) {
        
        FinanceRequestHeader header = createHeader("inquireTransactionHistoryList", "inquireTransactionHistoryList", userKey);
        
        FinanceTransactionHistoryRequest data = new FinanceTransactionHistoryRequest(
                accountNo,
                startDate,
                endDate,
                "A", // 전체
                "DESC" // 최신순
        );

        FinanceRequest<FinanceTransactionHistoryRequest> request = new FinanceRequest<>(header, data);

        FinanceResponse<FinanceTransactionHistoryResponse> response = restClient.post()
                .uri("/edu/demandDeposit/inquireTransactionHistoryList")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        validateResponse(response);
        if (response != null && response.data() != null && response.data().rec() != null && response.data().rec().list() != null) {
            return response.data().rec().list();
        }

        return Collections.emptyList();
    }

    /**
     * 타행 계좌 연동
     */
    public void linkAccount(String userKey, String bankCode, String accountNumber) {
        throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다: 타행 계좌 연동");
    }

    private void validateResponse(FinanceResponse<?> response) {
        if (response == null || response.header() == null) {
            throw new RuntimeException("금융 API 응답이 비어있습니다.");
        }

        String resCode = response.header().responseCode();
        if (!FinanceErrorCode.SUCCESS.getCode().equals(resCode)) {
            FinanceErrorCode error = FinanceErrorCode.fromCode(resCode);
            String msg = (error != null) ? error.getDescription() : response.header().responseMessage();
            log.error("금융 API 호출 실패: {} ({})", resCode, msg);
            throw new RuntimeException("금융 API 오류: " + msg);
        }
    }
}
