package com.akku.backend.domain.auth.service;

import com.akku.backend.domain.auth.dto.LoginRequest;
import com.akku.backend.domain.auth.dto.SignupPinData;
import com.akku.backend.domain.auth.dto.SignupPinRequest;
import com.akku.backend.domain.auth.dto.SocialLoginData;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServicePinTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("PIN 설정 성공 테스트")
    void signupPin_Success() {
        // given
        UUID userId = UUID.randomUUID();
        SignupPinRequest request = new SignupPinRequest("123456");
        User user = spy(User.builder().id(userId).build());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode(request.pin())).willReturn("encoded_pin");
        given(jwtProvider.generateAccessToken(any(), any())).willReturn("access_token");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refresh_token");

        // when
        SignupPinData result = authService.signupPin(userId, request);

        // then
        assertNotNull(result);
        assertEquals("access_token", result.token());
        verify(user).updatePinPassword("encoded_pin");
    }

    @Test
    @DisplayName("간편 로그인 성공 테스트")
    void login_Success() {
        // given
        UUID userId = UUID.randomUUID();
        LoginRequest request = new LoginRequest(userId.toString(), "123456", "fcm_token");
        User user = spy(User.builder()
                .id(userId)
                .pinPassword("encoded_pin")
                .isActive(true)
                .build());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.pin(), "encoded_pin")).willReturn(true);
        given(jwtProvider.generateAccessToken(any(), any())).willReturn("access_token");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refresh_token");

        // when
        SocialLoginData result = authService.login(request);

        // then
        assertNotNull(result);
        assertTrue(result.isRegistered());
        verify(user).updateFcmToken("fcm_token");
    }
}
