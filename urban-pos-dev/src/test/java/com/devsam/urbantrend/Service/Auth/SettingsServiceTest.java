package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.AppSetting;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private AppSettingRepository settingRepository;

    private SettingsService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new SettingsService(settingRepository);
        admin = new User();
        admin.setId(UUID.randomUUID());
    }

    @Test
    void paybillIsEmptyUntilAnAdminSetsIt() {
        when(settingRepository.findById("mpesa_paybill")).thenReturn(Optional.empty());

        assertThat(service.getMpesaPaybill()).isEmpty();
    }

    @Test
    void savedPaybillIsTrimmedAndAttributedToTheAdmin() {
        when(settingRepository.findById("mpesa_paybill")).thenReturn(Optional.empty());

        String saved = service.setMpesaPaybill(" 522533 ", admin);

        assertThat(saved).isEqualTo("522533");
        ArgumentCaptor<AppSetting> captor = ArgumentCaptor.forClass(AppSetting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getKey()).isEqualTo("mpesa_paybill");
        assertThat(captor.getValue().getValue()).isEqualTo("522533");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(admin.getId());
    }

    @Test
    void nonNumericPaybillIsRejectedBecauseItIsPrintedOnReceipts() {
        assertThatThrownBy(() -> service.setMpesaPaybill("<b>522533</b>", admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 to 10 digits");

        verify(settingRepository, never()).save(any());
    }

    @Test
    void blankClearsThePaybill() {
        when(settingRepository.findById("mpesa_paybill")).thenReturn(Optional.empty());

        assertThat(service.setMpesaPaybill("   ", admin)).isEmpty();
    }
}
