package com.devsam.urbantrend.Service.Auth.Messaging;

public interface SmsProvider {
    void sendSms(String phone, String message);
}


