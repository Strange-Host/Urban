package com.devsam.urbantrend;

import com.devsam.urbantrend.Util.AppTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class UrbanTrendApplication {

    public static void main(String[] args) {
        // Before anything reads the clock or opens a database connection.
        AppTimeZone.apply();
        SpringApplication.run(UrbanTrendApplication.class, args);
    }

}
