package com.akku.backend.domain.social.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.donation.entity.ActiveCharity;
import com.akku.backend.domain.donation.entity.Charity;
import com.akku.backend.domain.donation.repository.ActiveCharityRepository;
import com.akku.backend.domain.social.dto.*;
import com.akku.backend.domain.social.entity.Friend;
import com.akku.backend.domain.social.entity.FriendId;
import com.akku.backend.domain.social.entity.FriendInvite;
import com.akku.backend.domain.social.repository.FriendInviteRepository;
import com.akku.backend.domain.social.repository.FriendRepository;
import com.akku.backend.domain.social.exception.SocialErrorCode;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @InjectMocks
    private FriendService friendService;

    @Mock
    private FriendInviteRepository friendInviteRepository;
    
    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserItemRepository userItemRepository;

    @Mock
    private ActiveCharityRepository activeCharityRepository;

    @Test
    @DisplayName("초대 코드 생성 - 기존 코드가 없으면 새로 생성한다")
    void createInviteCode_New() {
        // given
        UUID userId = UUID.randomUUID();
        given(friendInviteRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when
        FriendInviteData result = friendService.createInviteCode(userId);

        // then
        assertThat(result.inviteCode()).isNotNull();
        verify(friendInviteRepository, times(1)).saveAndFlush(any(FriendInvite.class));
    }

    @Test
    @DisplayName("초대 코드 생성 - 기존 코드가 있으면 재사용한다")
    void createInviteCode_Reuse() {
        // given
        UUID userId = UUID.randomUUID();
        String existingCode = "existing-code";
        FriendInvite existingInvite = FriendInvite.builder()
                .inviteCode(existingCode)
                .userId(userId)
                .build();
        given(friendInviteRepository.findByUserId(userId)).willReturn(Optional.of(existingInvite));

        // when
        FriendInviteData result = friendService.createInviteCode(userId);

        // then
        assertThat(result.inviteCode()).isEqualTo(existingCode);
        verify(friendInviteRepository, times(0)).saveAndFlush(any(FriendInvite.class));
    }

    @Test
    @DisplayName("초대 코드 생성 - 동시성 상황에서 중복 생성이 시도되면 기존 코드를 반환한다")
    void createInviteCode_ConcurrencyConflict() {
        // given
        UUID userId = UUID.randomUUID();
        String existingCode = "already-created-code";
        FriendInvite existingInvite = FriendInvite.builder()
                .inviteCode(existingCode)
                .userId(userId)
                .build();

        given(friendInviteRepository.findByUserId(userId))
                .willReturn(Optional.empty()) // 첫 조회시에는 없음
                .willReturn(Optional.of(existingInvite)); // 예외 발생 후 재조회 시에는 있음

        given(friendInviteRepository.saveAndFlush(any(FriendInvite.class)))
                .willThrow(new DataIntegrityViolationException("Unique constraint violation"));

        // when
        FriendInviteData result = friendService.createInviteCode(userId);

        // then
        assertThat(result.inviteCode()).isEqualTo(existingCode);
        verify(friendInviteRepository, times(2)).findByUserId(userId);
    }

    @Test
    @DisplayName("초대 정보 조회 - 유효한 코드이면 정보와 함께 true를 반환한다")
    void getInviteInfo_Valid() {
        // given
        String inviteCode = "valid-code";
        UUID inviterId = UUID.randomUUID();
        FriendInvite invite = FriendInvite.builder()
                .inviteCode(inviteCode)
                .userId(inviterId)
                .build();
        
        User inviter = User.builder()
                .id(inviterId)
                .name("초대자")
                .build();

        given(friendInviteRepository.findById(inviteCode)).willReturn(Optional.of(invite));
        given(userRepository.findById(inviterId)).willReturn(Optional.of(inviter));
        given(userItemRepository.findEquippedItemsByUserId(inviterId)).willReturn(List.of());

        // when
        FriendInformationData result = friendService.getInviteInfo(inviteCode);

        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.inviterName()).isEqualTo("초대자");
    }

    @Test
    @DisplayName("초대 정보 조회 - 유효하지 않은 코드이면 false를 반환한다")
    void getInviteInfo_Invalid() {
        // given
        String inviteCode = "invalid-code";
        given(friendInviteRepository.findById(inviteCode)).willReturn(Optional.empty());

        // when
        FriendInformationData result = friendService.getInviteInfo(inviteCode);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.inviterId()).isNull();
    }

    @Test
    @DisplayName("친구 목록 조회 - 성공적으로 친구 목록을 반환한다")
    void getFriendList_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        FriendId id = new FriendId(userId, friendId);
        Friend friend = Friend.builder().id(id).build();

        User friendUser = User.builder()
                .id(friendId)
                .name("친구")
                .build();

        given(friendRepository.findAllByIdUserId(userId)).willReturn(List.of(friend));
        given(userRepository.findById(friendId)).willReturn(Optional.of(friendUser));
        given(userItemRepository.findEquippedItemsByUserId(friendId)).willReturn(List.of());

        // when
        FriendListResponse result = friendService.getFriendList(userId);

        // then
        assertThat(result.getFriends()).hasSize(1);
        assertThat(result.getFriends().get(0).getName()).isEqualTo("친구");
        assertThat(result.getFriends().get(0).getFriendId()).isEqualTo(friendId);
    }

    @Test
    @DisplayName("친구 삭제 - 리포지토리의 deleteById를 호출한다")
    void deleteFriend_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        FriendId id = new FriendId(userId, friendId);

        // when
        friendService.deleteFriend(userId, friendId);

        // then
        verify(friendRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("친구 타운 정보 조회 - 성공적으로 정보를 반환한다")
    void getFriendTown_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        User friendUser = User.builder()
                .id(friendId)
                .name("친구")
                .build();

        Charity charity = Charity.builder()
                .name("유니세프")
                .build();
        ActiveCharity activeCharity = ActiveCharity.builder()
                .charity(charity)
                .build();

        given(friendRepository.existsById(new FriendId(userId, friendId))).willReturn(true);
        given(userRepository.findById(friendId)).willReturn(Optional.of(friendUser));
        given(userItemRepository.findEquippedItemsByUserId(friendId)).willReturn(List.of());
        given(activeCharityRepository.findFirstByUserIdOrderByCreatedAtDesc(friendId)).willReturn(Optional.of(activeCharity));

        // when
        FriendTownResponse result = friendService.getFriendTown(userId, friendId);

        // then
        assertThat(result.friendName()).isEqualTo("친구");
        assertThat(result.recentCharity()).isEqualTo("유니세프");
        assertThat(result.avatar().eyeType()).isEqualTo("BASIC");
    }

    @Test
    @DisplayName("친구 타운 정보 조회 - 친구 관계가 아니면 예외를 던진다")
    void getFriendTown_NotFriends_ThrowsException() {
        // given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        given(friendRepository.existsById(new FriendId(userId, friendId))).willReturn(false);

        // when & then
        ApiException exception = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class, () -> 
                friendService.getFriendTown(userId, friendId));
        assertThat(exception.getErrorCode()).isEqualTo(SocialErrorCode.SOC_004);
    }
}
