package com.akku.backend.domain.social.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.social.dto.FriendInviteData;
import com.akku.backend.domain.social.dto.FriendInformationData;
import com.akku.backend.domain.social.entity.FriendInvite;
import com.akku.backend.domain.social.repository.FriendInviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendInviteRepository friendInviteRepository;
    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;

    /**
     * 친구 초대 코드 생성 (이미 있으면 기존 코드 반환)
     * 동시성 발생 시 고유 제약 조건을 통해 중복 생성을 방지하고 기존 정보를 조회해 반환
     */
    @Transactional
    public FriendInviteData createInviteCode(UUID userId) {
        return friendInviteRepository.findByUserId(userId)
                .map(invite -> new FriendInviteData(invite.getInviteCode()))
                .orElseGet(() -> {
                    try {
                        String code = UUID.randomUUID().toString();
                        FriendInvite invite = FriendInvite.builder()
                                .inviteCode(code)
                                .userId(userId)
                                .build();
                        // 멱등성 보장을 위해 즉시 반영하여 제약 조건 위반 체크
                        friendInviteRepository.saveAndFlush(invite);
                        return new FriendInviteData(code);
                    } catch (DataIntegrityViolationException e) {
                        // 다른 트랜잭션에서 이미 생성된 경우, 다시 조회하여 반환
                        return friendInviteRepository.findByUserId(userId)
                                .map(invite -> new FriendInviteData(invite.getInviteCode()))
                                .orElseThrow(() -> e);
                    }
                });
    }

    /**
     * 초대 코드 기반 친구 정보 조회 (초대자 정보)
     * 코드가 유효하지 않으면 isValid = false를 반환
     */
    public FriendInformationData getInviteInfo(String inviteCode) {
        return friendInviteRepository.findById(inviteCode)
                .map(invite -> {
                    User inviter = userRepository.findById(invite.getUserId())
                            .orElse(null);
                    if (inviter == null) {
                        return new FriendInformationData(null, null, List.of(), false);
                    }

                    // 아바타 정보 조회
                    List<String> avatarUrls = userItemRepository.findEquippedItemsByUserId(inviter.getId())
                            .stream()
                            .map(ui -> ui.getItem().getResourceUrl())
                            .toList();

                    return new FriendInformationData(inviter.getId(), inviter.getName(), avatarUrls, true);
                })
                .orElse(new FriendInformationData(null, null, List.of(), false));
    }
}
