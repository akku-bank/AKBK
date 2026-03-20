package com.akku.backend.domain.donation.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.entity.Item;
import com.akku.backend.domain.avatar.entity.UserItem;
import com.akku.backend.domain.avatar.repository.ItemRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.donation.dto.*;
import com.akku.backend.domain.donation.entity.ActiveCharity;
import com.akku.backend.domain.donation.entity.Charity;
import com.akku.backend.domain.donation.exception.DonationErrorCode;
import com.akku.backend.domain.donation.repository.ActiveCharityRepository;
import com.akku.backend.domain.donation.repository.CharityRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.entity.JellingTransaction;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final CharityRepository charityRepository;
    private final ActiveCharityRepository activeCharityRepository;
    private final UserRepository userRepository;
    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;

    /**
     * 유저의 젤링 허브 정보(잔액, 진행 중인 저금통) 조회
     */
    public JellingHubResponse getJellingHubInfo(UUID userId) {
        // 젤링 잔액 조회
        Long remainJelling = jellingRepository.findById(userId)
                .map(Jelling::getBalance)
                .orElse(0L);

        // 진행 중인 저금통 조회
        ActiveCharityInfo activeCharityInfo = activeCharityRepository.findByUserIdAndStatus(userId, "IN_PROGRESS")
                .map(activeCharity -> new ActiveCharityInfo(
                        activeCharity.getId(),
                        activeCharity.getCharity().getName(),
                        activeCharity.getCurrentAmount(),
                        activeCharity.getTargetAmount()
                ))
                .orElse(null);

        return new JellingHubResponse(remainJelling, activeCharityInfo);
    }

    /**
     * 전체 기부처 목록 조회
     */
    public CharityListResponse getCharityList() {
        List<CharityResponse> charities = charityRepository.findAll().stream()
                .map(charity -> new CharityResponse(
                        charity.getId(),
                        charity.getName(),
                        charity.getDescription()
                ))
                .toList();
        return new CharityListResponse(charities);
    }

    /**
     * 기부 목표 설정
     */
    @Transactional
    public UUID setTargetCharity(UUID userId, UUID charityId) {
        // 진행 중인 기부 목표가 있는지 확인
        if (activeCharityRepository.existsByUserIdAndStatus(userId, "IN_PROGRESS")) {
            throw new ApiException(DonationErrorCode.ACTIVE_CHARITY_ALREADY_EXISTS);
        }

        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 기부처 존재 확인
        Charity charity = charityRepository.findById(charityId)
                .orElseThrow(() -> new ApiException(DonationErrorCode.CHARITY_NOT_FOUND));

        // 새로운 기부 목표 저장
        ActiveCharity activeCharity = ActiveCharity.builder()
                .user(user)
                .charity(charity)
                .currentAmount(0L)
                .status("IN_PROGRESS")
                .build();

        return activeCharityRepository.save(activeCharity).getId();
    }

    /**
     * 진행 중인 저금통에 기부 실행
     */
    @Transactional
    public ExecuteDonationResponse executeDonation(UUID userId, Integer amount) {
        // 진행 중인 저금통 조회
        ActiveCharity activeCharity = activeCharityRepository.findByUserIdAndStatus(userId, "IN_PROGRESS")
                .orElseThrow(() -> new ApiException(DonationErrorCode.CHARITY_NOT_FOUND)); // TODO: 전전 단계에서 명세에 맞는 코드로 교체 필요할 수 있음

        // 젤링 잔액 조회 및 락
        Jelling jelling = jellingRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ApiException(DonationErrorCode.INSUFFICIENT_JELLING)); // 지갑이 없으면 0으로 간주하거나 에러

        if (jelling.getBalance() < amount) {
            throw new ApiException(DonationErrorCode.INSUFFICIENT_JELLING);
        }

        // 젤링 차감
        jelling.deductBalance(amount);

        // 젤링 변동 내역 저장
        JellingTransaction transaction = JellingTransaction.builder()
                .user(jelling.getUser())
                .amount(-(long)amount)
                .type("DONATE")
                .description(activeCharity.getCharity().getName() + " 기부")
                .build();
        jellingTransactionRepository.save(transaction);

        // 저금통 진행률 업데이트
        activeCharity.donate(amount);

        return new ExecuteDonationResponse(
                jelling.getBalance(),
                activeCharity.getCurrentAmount(),
                activeCharity.isCompleted()
        );
    }

    /**
     * 완료된 저금통에 대해 보상 수령
     */
    @Transactional
    public ReceiveRewardResponse receiveReward(UUID userId) {
        // 완료된 저금통 조회
        ActiveCharity activeCharity = activeCharityRepository.findByUserIdAndStatus(userId, "COMPLETED")
                .orElseThrow(() -> new ApiException(DonationErrorCode.CHARITY_NOT_FOUND)); // 적절한 에러 코드가 없으면 새로 정의 필요하나 일단은 NOT_FOUND

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 랜덤 아이템 보상 (이미 보유한 아이템도 지급 가능)
        List<Item> allItems = itemRepository.findAll();
        String rewardItemName = "준비된 아이템이 없습니다.";
        boolean isDuplicate = false;

        if (!allItems.isEmpty()) {
            int randomIndex = (int) (Math.random() * allItems.size());
            Item pickedItem = allItems.get(randomIndex);

            // 이미 보유 중인지 확인하여 중복 여부 체크 및 신규 보상 지급
            List<UserItem> userItems = userItemRepository.findAllByUserIdWithItem(userId);
            isDuplicate = userItems.stream()
                    .anyMatch(ui -> ui.getItem().getId().equals(pickedItem.getId()));

            if (!isDuplicate) {
                userItemRepository.save(UserItem.builder()
                        .user(user)
                        .item(pickedItem)
                        .isEquipped(false)
                        .build());
            }
            rewardItemName = pickedItem.getName();
        }
        // 커스텀 티켓 보상 (나중에 아바타 도메인의 UserItem.quantity와 연결할 로직)
        /*
        // Avatar 도메인 완료 시 교체할 로직 (UserItem.quantity 사용)
        Item ticketItem = itemRepository.findByName("커스텀 티켓").orElseThrow();
        UserItem userTicket = userItemRepository.findByUserIdAndItemId(userId, ticketItem.getId())
                .orElseGet(() -> userItemRepository.save(new UserItem(user, ticketItem, false, 0)));
        userTicket.addQuantity(1);
        */
        int ticketCount = 0; // 아직 티켓 기능이 비활성화 상태이므로 0 반환

        // 저금통 상태 변경 (REWARDED)
        activeCharity.markRewarded();

        return new ReceiveRewardResponse(rewardItemName, ticketCount, isDuplicate);
    }
}
