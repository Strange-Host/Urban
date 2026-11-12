package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Entity.Notification;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.NotificationRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSseService sseService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── SSE Subscribe ──────────────────────────────────────────────────────────
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        log.info("📡 SSE subscribe request received | username: {}", userDetails.getUsername());

        User user = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> {
                    return new RuntimeException("User not found");
                });


        SseEmitter emitter = sseService.subscribe(user.getId());
        return emitter;
    }

    // ── Get Unread Notifications ───────────────────────────────────────────────
    @GetMapping("/unread")
    public List<Notification> getUnread(@AuthenticationPrincipal UserDetails userDetails) {

        log.info("🔔 Fetching unread notifications | username: {}", userDetails.getUsername());

        User user = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> {
                    return new RuntimeException("User not found");
                });

        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderBySentAtDesc(user.getId());

        if (!unread.isEmpty()) {
            unread.forEach(n -> log.debug(
                    "  → id: {} | type: {} | title: {} | referenceId: {} | sentAt: {}",
                    n.getId(), n.getType(), n.getTitle(), n.getReferenceId(), n.getSentAt()
            ));
        }

        return unread;
    }

    // ── Mark Single Notification as Read ──────────────────────────────────────
    @PatchMapping("/{id}/read")
    public void markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        notificationRepository.findById(id).ifPresentOrElse(
                n -> {
                    if (!n.getUserId().equals(user.getId())) {
                        throw new AccessDeniedException("Not allowed to modify this notification");
                    }
                    if (n.isRead()) {
                        return;
                    }
                    n.setRead(true);
                    notificationRepository.save(n);
                },
                () -> log.error("❌ Notification not found | id: {}", id)
        );
    }

    // ── Mark All as Read ───────────────────────────────────────────────────────
    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("❌ markAllRead failed — user not found: {}", userDetails.getUsername());
                    return new RuntimeException("User not found");
                });

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        if (unreadCount == 0) {
            return;
        }

        notificationRepository.markAllAsReadForUser(user.getId());
    }
}