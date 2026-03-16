package com.akku.backend.domain.quiz.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter를 userId 기준으로 in-memory에서 관리하는 컴포넌트.
 *
 * <p>애플리케이션 싱글톤 스코프. Kafka 컨슈머 스레드와 HTTP 요청 스레드가
 * 동시에 접근하므로 모든 상태 변경은 {@link ConcurrentHashMap}의 원자적 연산을 사용한다.</p>
 *
 * <h3>생명주기</h3>
 * <pre>
 *  GET /chat/stream  →  connect()  →  emitter 등록
 *  Kafka consumer    →  send()     →  AI 응답 푸시
 *  Kafka consumer    →  complete() →  스트림 정상 종료
 *  timeout / error   →  자동 제거  →  메모리 누수 방지
 * </pre>
 */
@Slf4j
@Component
public class SseConnectionManager {

    /** AI 응답 최대 허용 대기 시간 (3분). 이 시간 내에 CHAT_RESPONSE가 도착해야 한다. */
    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 사용자의 SSE 연결을 수립하고 {@link SseEmitter}를 반환한다.
     *
     * <p>동일 userId로 재연결 시 기존 emitter를 즉시 완료 처리한 뒤 새 emitter로 교체한다.
     * (브라우저 탭 새로고침 등에서 발생하는 중복 연결 방어)</p>
     */
    public SseEmitter connect(UUID userId) {
        // 기존 연결이 있으면 먼저 정리 (중복 연결 방지)
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            existing.complete();
            log.debug("기존 SSE 연결 교체 - userId: {}", userId);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 콜백 등록 — 모두 ConcurrentHashMap.remove(key, value) 로 정확한 인스턴스만 제거
        emitter.onTimeout(() -> {
            log.warn("SSE 타임아웃 ({} ms) - userId: {}", SSE_TIMEOUT_MS, userId);
            emitters.remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(ex -> {
            log.warn("SSE 에러 - userId: {}, cause: {}", userId, ex.getMessage());
            emitters.remove(userId, emitter);
        });
        emitter.onCompletion(() -> emitters.remove(userId, emitter));

        emitters.put(userId, emitter);

        // 초기 핸드셰이크 이벤트 — 클라이언트가 연결 성공을 즉시 확인할 수 있도록 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결 완료", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            log.warn("SSE 초기 이벤트 전송 실패 - userId: {}. 연결을 즉시 정리합니다.", userId, e);
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }

        log.info("SSE 연결 수립 - userId: {}, 현재 활성 연결 수: {}", userId, emitters.size());
        return emitter;
    }

    /**
     * 특정 사용자에게 SSE 이벤트 페이로드를 전송한다.
     *
     * <p>활성 연결이 없으면 warn 로그만 남기고 조용히 종료한다 (예외 전파 없음).
     * {@link IOException} 발생 시 해당 emitter를 맵에서 제거한다.</p>
     *
     * @param userId    수신 대상 사용자 ID
     * @param payload   Jackson이 직렬화할 데이터 객체 (record, POJO, Map 모두 가능)
     * @param eventName SSE 이벤트 이름 (클라이언트의 addEventListener 이름과 일치해야 함)
     */
    public void send(UUID userId, Object payload, String eventName) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.warn("SSE 전송 실패 — 활성 연결 없음. userId: {}, event: {}", userId, eventName);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
            log.debug("SSE 이벤트 전송 완료 - userId: {}, event: {}", userId, eventName);
        } catch (IOException e) {
            log.warn("SSE 전송 IOException - userId: {}. Emitter 제거.", userId, e);
            emitters.remove(userId, emitter);
        }
    }

    /**
     * SSE 스트림을 정상 종료한다. Kafka 컨슈머가 응답 처리를 완료한 후 호출한다.
     */
    public void complete(UUID userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 연결 정상 종료 - userId: {}", userId);
        }
    }
}
