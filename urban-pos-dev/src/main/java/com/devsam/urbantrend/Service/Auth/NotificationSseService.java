package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationSseService {

    // One emitter per connected user
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        emitters.put(userId, emitter);
        return emitter;
    }

    public void sendToUser(UUID userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return; // user not connected, they'll see it on next login

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }

    // Add this method to NotificationSseService
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}