package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.OtpData;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Service.Auth.Messaging.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final MessagingService messagingService;

    // Held in memory: codes are lost on restart and are not shared between servers
    // (documented limitation M6). Attempt counting is atomic per phone number.
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSentAt = new ConcurrentHashMap<>();

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    // 🔥 Generate + Send OTP
    public void generateAndSendOtp(User user) {

        String phone = user.getPhoneNumber();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime lastSent = lastSentAt.get(phone);
        if (lastSent != null && lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }

        otpStorage.entrySet().removeIf(entry -> entry.getValue().getExpiry().isBefore(now));
        lastSentAt.entrySet().removeIf(entry -> entry.getValue().plusHours(1).isBefore(now));

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpData otpData = new OtpData(
                otp,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                0
        );

        otpStorage.put(phone, otpData);
        lastSentAt.put(phone, now);

        messagingService.sendOtpSms(phone, otp);
    }

    public boolean verifyOtp(String phoneNumber, String inputOtp) {

        if (phoneNumber == null) {
            return false;
        }

        // The check, the attempt count and the removal all happen inside one atomic
        // per-key operation, so parallel guesses cannot each read the same attempt count
        // and slip past the limit.
        boolean[] verified = {false};
        LocalDateTime now = LocalDateTime.now();

        otpStorage.computeIfPresent(phoneNumber, (key, data) -> {
            if (data.getExpiry().isBefore(now) || data.getAttempts() >= MAX_ATTEMPTS) {
                return null;
            }

            boolean matches = inputOtp != null && MessageDigest.isEqual(
                    data.getOtp().getBytes(StandardCharsets.UTF_8),
                    inputOtp.getBytes(StandardCharsets.UTF_8));

            if (matches) {
                verified[0] = true;
                return null;
            }

            data.setAttempts(data.getAttempts() + 1);
            return data;
        });

        return verified[0];
    }
}
