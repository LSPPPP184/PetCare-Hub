package com.petcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppointmentClockConfig {
    @Bean public Clock appointmentClock() { return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")); }
}
