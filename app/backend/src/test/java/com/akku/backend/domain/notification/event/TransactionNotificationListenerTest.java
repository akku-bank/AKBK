package com.akku.backend.domain.notification.event;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.bank.event.DepositReceivedEvent;
import com.akku.backend.domain.bank.event.RemittanceEvent;
import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionNotificationListener 테스트")
class TransactionNotificationListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private TransactionNotificationListener listener;

    @Test
    @DisplayName("카드 결제 시 알림 발송 성공")
    void onCardPayment_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fcmToken("mock-token")
                .build();

        CardPaymentEvent event = new CardPaymentEvent(
                userId, "123-456", "tx-123", "sync-123", "Category", 10000L, 90000L, "merchant-1", "스타벅스", LocalDate.now(), "120000", false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        listener.onCardPayment(event);

        // Then
        verify(fcmService, times(1)).sendMessage(any(NotificationRequest.class));
    }

    @Test
    @DisplayName("송금 시 알림 발송 성공")
    void onRemittance_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fcmToken("mock-token")
                .build();

        RemittanceEvent event = new RemittanceEvent(
                userId, "111-222", "001", "333-444", "홍길동", 5000L, 45000L, "remit-123"
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        listener.onRemittance(event);

        // Then
        verify(fcmService, times(1)).sendMessage(any(NotificationRequest.class));
    }

    @Test
    @DisplayName("입금 수신 시 알림 발송 성공")
    void onDepositReceived_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fcmToken("receive-token")
                .build();

        DepositReceivedEvent event = new DepositReceivedEvent(
                userId, "이싸피", 10000L, 110000L, "dep-123"
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        listener.onDepositReceived(event);

        // Then
        verify(fcmService, times(1)).sendMessage(argThat(request -> 
                request.getToken().equals("receive-token") && 
                request.getTitle().contains("입금") &&
                request.getBody().contains("이싸피")
        ));
    }

    @Test
    @DisplayName("FCM 토큰이 없으면 알림 발송 스킵")
    void onCardPayment_Skip_NoToken() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fcmToken(null).build();

        CardPaymentEvent event = new CardPaymentEvent(
                userId, "123-456", "tx-123", "sync-123", "Category", 10000L, 90000L, "merchant-1", "스타벅스", LocalDate.now(), "120000", false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        listener.onCardPayment(event);

        // Then
        verify(fcmService, never()).sendMessage(any());
    }
}
