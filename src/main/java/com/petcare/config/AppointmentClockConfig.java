package com.petcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppointmentClockConfig {
    @Bean public Clock appointmentClock() { return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")); }

    // Clock sức khỏe chỉ được inject khi có qualifier, không chiếm Clock của module khác.
    @Bean(name = "healthClock", defaultCandidate = false) @Profile("!demo")
    public Clock healthClock(Clock realClock) { return realClock; }

    @Bean(name = "healthClock", defaultCandidate = false) @Profile("demo")
    public MutableClock demoClock(Clock realClock) { return new MutableClock(realClock); }
}
