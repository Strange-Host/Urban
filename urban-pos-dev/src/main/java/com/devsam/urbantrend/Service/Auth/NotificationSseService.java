package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationSseService {

    // Several emitters per user: every open tab, plus the bell and the CRM
    // page can each hold a connection.
    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // A finite timeout lets Spring close streams that went silent; the browser
    // reconnects on its own and reloads the unread list when it does.
    private static final long TIMEOUT_MS = 30L * 60 * 1000;

    public SseEmitter subscribe(UUID userId) {

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitters.compute(userId, (id, connections) -> {
            Set<SseEmitter> next = connections == null ? ConcurrentHashMap.newKeySet() : connections;
            next.add(emitter);
            return next;
        });

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));

        return emitter;
    }

    public void sendToUser(UUID userId, Notification notification) {

        Set<SseEmitter> connections = emitters.get(userId);
        if (connections == null) return; // user not connected, they'll see it on next login

        for (SseEmitter emitter : connections) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException | IllegalStateException e) {
                drop(userId, emitter);
            }
        }
    }

    // Proxies and mobile networks drop idle streams without telling anyone;
    // a periodic comment both keeps the connection alive and surfaces the
    // dead ones so they stop accumulating.
    @Scheduled(fixedDelay = 25000)
    public void heartbeat() {

        emitters.forEach((userId, connections) ->
                connections.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (IOException | IllegalStateException e) {
                        drop(userId, emitter);
                    }
                }));
    }

    public int getActiveConnectionCount() {
        return emitters.values().stream().mapToInt(Set::size).sum();
    }

    private void remove(UUID userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id, connections) -> {
            connections.remove(emitter);
            return connections.isEmpty() ? null : connections;
        });
    }

    private void drop(UUID userId, SseEmitter emitter) {
        remove(userId, emitter);
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // already completed
        }
        log.debug("Dropped dead SSE connection for user {}", userId);
    }
}
