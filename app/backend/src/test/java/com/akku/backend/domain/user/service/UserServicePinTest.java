package com.akku.backend.domain.user.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.user.dto.PinChangeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServicePinTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("PIN 변경 성공 테스트")
    void updatePin_Success() {
        // given
        UUID userId = UUID.randomUUID();
        PinChangeRequest request = new PinChangeRequest("123456", "654321");
        User user = spy(User.builder()
                .id(userId)
                .pinPassword("encoded_old_pin")
                .build());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded_old_pin")).willReturn(true);
        given(passwordEncoder.encode("654321")).willReturn("encoded_new_pin");

        // when
        userService.updatePin(userId, request);

        // then
        verify(user).updatePinPassword("encoded_new_pin");
    }
}
