package com.akku.backend.domain.home.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.domain.home.dto.ChildHomeResponse;
import com.akku.backend.domain.home.dto.ChildHomeResponse.AvatarDto;
import com.akku.backend.domain.home.dto.ChildHomeResponse.EquippedItemDto;
import com.akku.backend.domain.home.dto.ParentHomeResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.repository.WeeklyReportRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;
    private final FamilyProfileRepository familyProfileRepository;
    private final AccountRepository accountRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final com.akku.backend.domain.bank.service.AccountService accountService;

    public ChildHomeResponse getChildHome(UUID userId) {

        // 1. 유저 기본 정보 조회 (Real)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 2. 장착 중인 아바타 아이템 목록 조회 (Real)
        List<EquippedItemDto> equippedItems = userItemRepository.findEquippedItemsByUserId(userId)
                .stream()
                .map(ui -> new EquippedItemDto(
                        ui.getItem().getId(),
                        ui.getItem().getCategory(),
                        ui.getItem().getResourceUrl()
                ))
                .toList();

        // 3. 아바타 객체 조립 (eyeType, skinColor는 일단 더미 데이터)
        AvatarDto avatarDto = new AvatarDto(
                "BASIC", // TODO: User 엔티티에 외형 컬럼 추가 후 user.getEyeType()으로 변경
                "BASIC", // TODO: User 엔티티에 외형 컬럼 추가 후 user.getSkinColor()로 변경
                equippedItems
        );

        // 4. 최종 홈 데이터 반환 (나머지 도메인 데이터는 프론트 작업용 더미)
        long currentBalance = accountService.getPrimaryAccountBalance(userId);

        return new ChildHomeResponse(
                currentBalance,  // Real 데이터 (현금 잔액)
                1500L,           // TODO: WalletService 연동 시 교체 (젤링 잔액)
                user.getLevel(), // Real 데이터 (현재 레벨)
                450,             // TODO: UserService/LevelService 연동 시 교체 (현재 점수)
                true,            // TODO: 레벨업 상태 검증 로직 연동 시 교체
                false,           // TODO: NotificationService 연동 시 교체 (알림 여부)
                350,             // TODO: DonationService 연동 시 교체 (기부 게이지)
                avatarDto
        );
    }

    /**
     * 부모 대시보드 홈 데이터 조회
     */
    @Transactional
    public ParentHomeResponse getParentHome(UUID userId) {

        // 1. 부모 유저 정보 조회
        User parent = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        UUID familyId = parent.getFamilyId();

        // 2. 가족 구성원 프로필(빈 의자 포함) 목록 조회
        List<FamilyProfileEntity> familyProfiles = familyProfileRepository.findAllByFamilyId(familyId);

        // 3. 자녀(CHILD) 프로필만 필터링하여 DTO 변환 (Real + Mock 하이브리드)
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        List<ParentHomeResponse.ChildSummaryDto> children = familyProfiles.stream()
                .filter(profile -> "CHILD".equals(profile.getRole()))
                .map(profile -> {
                    UUID childId = profile.getLinkedUserId();
                    long childBalance = 0L;
                    long weeklySpending = 0L;

                    String bankCode = null;
                    String accountNumber = null;

                    if (childId != null) {
                        Account childAccount = accountService.getPrimaryAccount(childId);
                        childBalance = childAccount.getBalance();
                        bankCode = childAccount.getBankCode();
                        accountNumber = childAccount.getAccountNumber();

                        weeklySpending = weeklyReportRepository.findByIdUserIdAndIdStartDay(childId, weekStart)
                                .stream()
                                .filter(r -> "SPEND".equals(r.getId().getType()))
                                .mapToLong(WeeklyReport::getTotalAmount)
                                .sum();
                    }

                    return new ParentHomeResponse.ChildSummaryDto(
                            childId,
                            profile.getName(),
                            childBalance,
                            weeklySpending,
                            bankCode,
                            accountNumber
                    );
                })
                .toList();

        // 4. 최종 부모 대시보드 데이터 반환
        long parentBalance = accountService.getPrimaryAccountBalance(userId);

        return new ParentHomeResponse(
                parentBalance, // Real 데이터 (부모 잔액)
                2,             // TODO: ChallengeService 연동 시 교체 (대기 중인 챌린지 수)
                children       // Real 데이터
        );
    }

}