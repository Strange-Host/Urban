package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.OtpCode;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.OtpCodeRepository;
import com.devsam.urbantrend.Service.Auth.Messaging.MessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String PHONE = "254700000001";

    @Mock private MessagingService messagingService;
    @Mock private OtpCodeRepository otpCodeRepository;

    // A tiny in-memory stand-in for the table, so the tests exercise the real counting rules.
    private final Map<String, OtpCode> table = new HashMap<>();

    private OtpService service;
    private String code;

    @BeforeEach
    void setUp() {
        lenient().when(otpCodeRepository.findById(anyString()))
                .thenAnswer(call -> Optional.ofNullable(table.get(call.<String>getArgument(0))));
        lenient().when(otpCodeRepository.lockByPhoneNumber(anyString()))
                .thenAnswer(call -> Optional.ofNullable(table.get(call.<String>getArgument(0))));
        lenient().when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(call -> {
            OtpCode saved = call.getArgument(0);
            table.put(saved.getPhoneNumber(), saved);
            return saved;
        });
        lenient().doAnswer(call -> {
            OtpCode gone = call.getArgument(0);
            table.remove(gone.getPhoneNumber());
            return null;
        }).when(otpCodeRepository).delete(any(OtpCode.class));

        service = new OtpService(messagingService, otpCodeRepository);

        User user = new User();
        user.setPhoneNumber(PHONE);
        service.generateAndSendOtp(user);

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(messagingService).sendOtpSms(eq(PHONE), sent.capture());
        code = sent.getValue();
    }

    private String wrongCode() {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    void theCodeIsStoredHashedNotInTheClear() {
        assertThat(table.get(PHONE).getCodeHash()).isNotEqualTo(code).hasSize(64);
    }

    @Test
    void correctCodeVerifiesOnceOnly() {
        assertThat(service.verifyOtp(PHONE, code)).isTrue();
        assertThat(service.verifyOtp(PHONE, code)).isFalse();
    }

    @Test
    void codeIsLockedAfterThreeWrongGuesses() {
        assertThat(service.verifyOtp(PHONE, wrongCode())).isFalse();
        assertThat(service.verifyOtp(PHONE, wrongCode())).isFalse();
        assertThat(service.verifyOtp(PHONE, wrongCode())).isFalse();

        assertThat(service.verifyOtp(PHONE, code)).isFalse();
    }

    @Test
    void expiredCodeIsRejected() {
        table.get(PHONE).setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThat(service.verifyOtp(PHONE, code)).isFalse();
        assertThat(table).doesNotContainKey(PHONE);
    }

    @Test
    void missingInputOrPhoneIsRejected() {
        assertThat(service.verifyOtp(PHONE, null)).isFalse();
        assertThat(service.verifyOtp(null, code)).isFalse();
        assertThat(service.verifyOtp("254799999999", code)).isFalse();
    }

    @Test
    void asecondRequestWithinTheCooldownIsRefused() {
        User user = new User();
        user.setPhoneNumber(PHONE);

        assertThatThrownBy(() -> service.generateAndSendOtp(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wait");
    }
}
