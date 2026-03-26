package com.akku.backend.domain.donation.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.donation.dto.*;
import com.akku.backend.domain.donation.entity.ActiveCharity;
import com.akku.backend.domain.donation.entity.Charity;
import com.akku.backend.domain.donation.exception.DonationErrorCode;
import com.akku.backend.domain.donation.repository.ActiveCharityRepository;
import com.akku.backend.domain.donation.repository.CharityRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private CharityRepository charityRepository;
    @Mock
    private ActiveCharityRepository activeCharityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JellingRepository jellingRepository;
    @Mock
    private JellingTransactionRepository jellingTransactionRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserItemRepository userItemRepository;

    @InjectMocks
    private DonationService donationService;

    @Test
    @DisplayName("젤링 허브 통합 정보를 조회한다.")
    void getJellingHubInfo() {
        // given
        UUID userId = UUID.randomUUID();
        Jelling jelling = Jelling.builder().userId(userId).balance(100L).build();
        Charity charity = Charity.builder().name("Test Charity").build();
        ActiveCharity activeCharity = ActiveCharity.builder()
                .id(UUID.randomUUID())
                .charity(charity)
                .targetAmount(500)
                .status("IN_PROGRESS")
                .build();

        given(jellingRepository.findById(userId)).willReturn(Optional.of(jelling));
        given(activeCharityRepository.findByUserIdAndStatus(userId, "IN_PROGRESS")).willReturn(Optional.of(activeCharity));

        // when
        JellingHubResponse response = donationService.getJellingHubInfo(userId);

        // then
        assertThat(response.remainJelling()).isEqualTo(100L);
        assertThat(response.activeCharity()).isNotNull();
        assertThat(response.activeCharity().name()).isEqualTo("Test Charity");
        assertThat(response.activeCharity().targetAmount()).isEqualTo(500);
    }

    @Test
    @DisplayName("기부처 목록을 조회한다.")
    void getCharityList() {
        // given
        Charity charity = Charity.builder()
                .id(UUID.randomUUID())
                .name("Test Charity")
                .description("Test Description")
                .build();
        given(charityRepository.findAll()).willReturn(List.of(charity));

        // when
        CharityListResponse result = donationService.getCharityList();

        // then
        assertThat(result.charities()).hasSize(1);
        assertThat(result.charities().get(0).name()).isEqualTo("Test Charity");
    }

    @Test
    @DisplayName("기부 목표를 성공적으로 설정한다.")
    void setTargetCharity_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID charityId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Charity charity = Charity.builder().id(charityId).build();

        given(activeCharityRepository.existsByUserIdAndStatus(userId, "IN_PROGRESS")).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(charityRepository.findById(charityId)).willReturn(Optional.of(charity));
        given(activeCharityRepository.save(any(ActiveCharity.class))).willReturn(ActiveCharity.builder().id(UUID.randomUUID()).build());

        // when
        donationService.setTargetCharity(userId, charityId);

        // then
        verify(activeCharityRepository).save(any(ActiveCharity.class));
    }

    @Test
    @DisplayName("이미 진행 중인 기부가 있으면 예외가 발생한다.")
    void setTargetCharity_alreadyExists() {
        // given
        UUID userId = UUID.randomUUID();
        UUID charityId = UUID.randomUUID();
        given(activeCharityRepository.existsByUserIdAndStatus(userId, "IN_PROGRESS")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> donationService.setTargetCharity(userId, charityId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(DonationErrorCode.ACTIVE_CHARITY_ALREADY_EXISTS.getDefaultMessage());
    }

    @Test
    @DisplayName("존재하지 않는 기부처인 경우 예외가 발생한다.")
    void setTargetCharity_charityNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID charityId = UUID.randomUUID();
        given(activeCharityRepository.existsByUserIdAndStatus(userId, "IN_PROGRESS")).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().build()));
        given(charityRepository.findById(charityId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> donationService.setTargetCharity(userId, charityId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(DonationErrorCode.CHARITY_NOT_FOUND.getDefaultMessage());
    }

    @Test
    @DisplayName("보상을 성공적으로 수령하고 젤링이 차감된다.")
    void receiveReward_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("test-key").build();
        Charity charity = Charity.builder().name("Test Charity").build();
        ActiveCharity activeCharity = ActiveCharity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .charity(charity)
                .targetAmount(500)
                .status("IN_PROGRESS")
                .build();
        Jelling jelling = Jelling.builder().userId(userId).user(user).balance(600L).build();

        given(activeCharityRepository.findByUserIdAndStatus(userId, "IN_PROGRESS")).willReturn(Optional.of(activeCharity));
        given(jellingRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(jelling));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(itemRepository.count()).willReturn(1L);
        given(itemRepository.findAll()).willReturn(List.of(Item.builder().id(UUID.randomUUID()).build()));

        // when
        ReceiveRewardResponse response = donationService.receiveReward(userId);

        // then
        assertThat(jelling.getBalance()).isEqualTo(100L);
        assertThat(activeCharity.getStatus()).isEqualTo("REWARDED");
        verify(jellingTransactionRepository).save(any());
        verify(userItemRepository).save(any());
    }

    @Test
    @DisplayName("젤링 잔액이 부족하면 보상 수령에 실패한다.")
    void receiveReward_insufficientBalance() {
        // given
        UUID userId = UUID.randomUUID();
        ActiveCharity activeCharity = ActiveCharity.builder()
                .targetAmount(500)
                .status("IN_PROGRESS")
                .build();
        Jelling jelling = Jelling.builder().balance(300L).build();

        given(activeCharityRepository.findByUserIdAndStatus(userId, "IN_PROGRESS")).willReturn(Optional.of(activeCharity));
        given(jellingRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(jelling));

        // when & then
        assertThatThrownBy(() -> donationService.receiveReward(userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(DonationErrorCode.INSUFFICIENT_JELLING.getDefaultMessage());
    }
}
