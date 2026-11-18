package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Service.Auth.Messaging.MessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String PHONE = "254700000001";

    @Mock private MessagingService messagingService;

    private OtpService service;
    private String code;

    @BeforeEach
    void setUp() {
        service = new OtpService(messagingService);

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
    void missingInputOrPhoneIsRejected() {
        assertThat(service.verifyOtp(PHONE, null)).isFalse();
        assertThat(service.verifyOtp(null, code)).isFalse();
        assertThat(service.verifyOtp("254799999999", code)).isFalse();
    }

    @Test
    void parallelGuessesCannotExceedTheAttemptLimit() throws Exception {
        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                // Only one of these 40 guesses is correct; it must not get through once
                // the three allowed attempts have been used up by wrong guesses.
                if (service.verifyOtp(PHONE, wrongCode())) {
                    accepted.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(accepted.get()).isZero();
        assertThat(service.verifyOtp(PHONE, code)).isFalse();
    }
}
