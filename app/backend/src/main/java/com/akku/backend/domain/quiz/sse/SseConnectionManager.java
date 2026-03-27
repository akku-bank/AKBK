package com.akku.backend.domain.quiz.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseConnectionManager {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingSseEvent> pendingEvents = new ConcurrentHashMap<>();

    public SseEmitter connect(UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onTimeout(() -> {
            emitters.remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(ex -> emitters.remove(userId, emitter));
        emitter.onCompletion(() -> emitters.remove(userId, emitter));

        SseEmitter existing = emitters.put(userId, emitter);
        if (existing != null) {
            existing.complete();
            emitters.remove(userId, existing);
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connected", MediaType.TEXT_PLAIN));
            flushPendingEvent(userId, emitter);
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void send(UUID userId, Object payload, String eventName) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            pendingEvents.put(userId, new PendingSseEvent(payload, eventName, false));
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }
    }

    public void complete(UUID userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            return;
        }

        pendingEvents.computeIfPresent(
                userId,
                (key, event) -> new PendingSseEvent(event.payload(), event.eventName(), true)
        );
    }

    private void flushPendingEvent(UUID userId, SseEmitter emitter) throws IOException {
        PendingSseEvent pendingEvent = pendingEvents.remove(userId);
        if (pendingEvent == null) {
            return;
        }

        emitter.send(SseEmitter.event()
                .name(pendingEvent.eventName())
                .data(pendingEvent.payload(), MediaType.APPLICATION_JSON));

        if (pendingEvent.completeAfterSend()) {
            emitters.remove(userId, emitter);
            emitter.complete();
        }
    }

    private record PendingSseEvent(Object payload, String eventName, boolean completeAfterSend) {}
}
