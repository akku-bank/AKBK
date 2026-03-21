package com.akku.backend.domain.social.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.social.dto.FriendInviteData;
import com.akku.backend.domain.social.dto.FriendInformationData;
import com.akku.backend.domain.social.entity.FriendInvite;
import com.akku.backend.domain.social.repository.FriendInviteRepository;
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
    private UserRepository userRepository;

    @Mock
    private UserItemRepository userItemRepository;

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
        verify(friendInviteRepository, times(1)).save(any(FriendInvite.class));
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
        verify(friendInviteRepository, times(0)).save(any(FriendInvite.class));
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
}
