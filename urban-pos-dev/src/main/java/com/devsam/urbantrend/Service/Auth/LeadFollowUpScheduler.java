package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.Notification;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.NotificationRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeadFollowUpScheduler {

    private final LeadRepository leadRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSseService sseService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkFollowUps() {

        LocalDateTime now = LocalDateTime.now();

        List<Lead> dueLeads = leadRepository
                .findByFollowUpAtBeforeAndNotifiedFalseAndStatusNot(now, "CONVERTED");


        if (dueLeads.isEmpty()) return;

        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        if (admins.isEmpty()) {
            return;
        }

        for (Lead lead : dueLeads) {

            for (User admin : admins) {

                boolean alreadySent = notificationRepository
                        .existsByUserIdAndReferenceIdAndType(
                                admin.getId(), lead.getId(), "LEAD_FOLLOWUP");

                if (alreadySent) {
                    continue;
                }

                Notification notification = Notification.builder()
                        .userId(admin.getId())
                        .title("Follow-up Due: " + lead.getName())
                        .message("Time to follow up with " + lead.getName()
                                + " (" + lead.getPhoneNumber() + ")"
                                + " — " + lead.getFormName())
                        .type("LEAD_FOLLOWUP")
                        .referenceId(lead.getId())
                        .isRead(false)
                        .sentAt(now)
                        .build();

                Notification saved = notificationRepository.save(notification);
                sseService.sendToUser(admin.getId(), notification);
            }

            // Mark lead notified after all admins are notified
            lead.setNotified(true);
            leadRepository.save(lead);
        }
    }
}
