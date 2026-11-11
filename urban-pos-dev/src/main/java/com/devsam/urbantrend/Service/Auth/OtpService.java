package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.OtpData;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Service.Auth.Messaging.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final MessagingService messagingService;

    // ⚠️ In production → replace with Redis
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    // 🔥 Generate + Send OTP
    public void generateAndSendOtp(User user) {

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        OtpData otpData = new OtpData(
                otp,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES),
                0
        );

        otpStorage.put(user.getPhoneNumber(), otpData);

        messagingService.sendOtpSms(user.getPhoneNumber(), otp);

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
        if (!data.getOtp().equals(inputOtp)) {
            data.setAttempts(data.getAttempts() + 1);
            return false;
        }

        // ✅ Success
        otpStorage.remove(phoneNumber);
        return true;
    }

}