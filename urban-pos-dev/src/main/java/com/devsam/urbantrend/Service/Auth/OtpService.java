package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.OtpData;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Service.Auth.Messaging.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final MessagingService messagingService;

    // ⚠️ In production → replace with Redis
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

        OtpData data = otpStorage.get(phoneNumber);

        if (data == null) {
            return false;
        }

        // ⏱️ Expiry check
        if (data.getExpiry().isBefore(LocalDateTime.now())) {
            otpStorage.remove(phoneNumber);
            return false;
        }

        // 🚫 Attempt limit
        if (data.getAttempts() >= MAX_ATTEMPTS) {
            otpStorage.remove(phoneNumber);
            return false;
        }

        // ❌ Wrong OTP
        if (inputOtp == null || !data.getOtp().equals(inputOtp)) {
            data.setAttempts(data.getAttempts() + 1);
            return false;
        }

        // ✅ Success
        otpStorage.remove(phoneNumber);
        return true;
    }
}
