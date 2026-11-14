package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.AppSetting;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Admin-editable shop settings. */
@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    static final String MPESA_PAYBILL = "mpesa_paybill";

    private final AppSettingRepository settingRepository;

    /** The configured paybill, or an empty string when none has been entered yet. */
    @Transactional(readOnly = true)
    public String getMpesaPaybill() {
        return settingRepository.findById(MPESA_PAYBILL)
                .map(AppSetting::getValue)
                .map(String::trim)
                .orElse("");
    }

    /** Saves the paybill; a blank value clears it. */
    public String setMpesaPaybill(String paybill, User updater) {
        String cleaned = paybill == null ? "" : paybill.trim();

        // A paybill is 5-7 digits in practice; allow a little slack but never free text,
        // because the value is printed on receipts and sent to customers.
        if (!cleaned.isEmpty() && !cleaned.matches("\\d{4,10}")) {
            throw new IllegalArgumentException("Paybill must be 4 to 10 digits");
        }

        AppSetting setting = settingRepository.findById(MPESA_PAYBILL).orElseGet(AppSetting::new);
        setting.setKey(MPESA_PAYBILL);
        setting.setValue(cleaned);
        setting.setUpdatedAt(OffsetDateTime.now());
        setting.setUpdatedBy(updater != null ? updater.getId() : null);
        settingRepository.save(setting);

        return cleaned;
    }
}
