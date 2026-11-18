package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.OtpCode;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.OtpCodeRepository;
import com.devsam.urbantrend.Service.Auth.Messaging.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final MessagingService messagingService;
    private final OtpCodeRepository otpCodeRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    // Not transactional on purpose: the code is saved first (each repository call commits on its
    // own) and the SMS goes out afterwards, so a slow SMS provider never holds a database lock.
    public void generateAndSendOtp(User user) {

        String phone = user.getPhoneNumber();
        LocalDateTime now = LocalDateTime.now();

        Optional<OtpCode> existing = otpCodeRepository.findById(phone);
        if (existing.isPresent()
                && existing.get().getSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }

        otpCodeRepository.deleteExpired(now);

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpCode code = existing.orElseGet(OtpCode::new);
        code.setPhoneNumber(phone);
        code.setCodeHash(hash(phone, otp));
        code.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        code.setSentAt(now);
        code.setAttempts(0);
        otpCodeRepository.save(code);

        messagingService.sendOtpSms(phone, otp);
    }

    // The row is locked for the whole check, so parallel guesses are counted one at a time and
    // cannot each read the same attempt count and slip past the limit.
    @Transactional
    public boolean verifyOtp(String phoneNumber, String inputOtp) {

        if (phoneNumber == null) {
            return false;
        }

        Optional<OtpCode> found = otpCodeRepository.lockByPhoneNumber(phoneNumber);
        if (found.isEmpty()) {
            return false;
        }

        OtpCode code = found.get();

        if (code.getExpiresAt().isBefore(LocalDateTime.now()) || code.getAttempts() >= MAX_ATTEMPTS) {
            otpCodeRepository.delete(code);
            return false;
        }

        boolean matches = inputOtp != null && MessageDigest.isEqual(
                code.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(phoneNumber, inputOtp).getBytes(StandardCharsets.UTF_8));

        if (matches) {
            otpCodeRepository.delete(code);
            return true;
        }

        code.setAttempts(code.getAttempts() + 1);
        otpCodeRepository.save(code);
        return false;
    }

    private static String hash(String phone, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((phone + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
