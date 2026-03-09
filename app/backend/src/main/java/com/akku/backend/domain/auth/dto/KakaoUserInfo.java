package com.akku.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 API 응답 DTO
 * GET https://kapi.kakao.com/v2/user/me
 */
@Getter
@NoArgsConstructor
public class KakaoUserInfo {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        private String email;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        private Profile profile;

        @Getter
        @NoArgsConstructor
        public static class Profile {
            private String nickname;

            @JsonProperty("thumbnail_image_url")
            private String thumbnailImageUrl;
        }
    }

    public String getEmail() {
        if (kakaoAccount == null) return null;
        return kakaoAccount.getEmail();
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.getProfile() == null) return null;
        return kakaoAccount.getProfile().getNickname();
    }

    public String getProviderId() {
        return String.valueOf(id);
    }
}
