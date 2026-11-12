package com.petcare;

import com.petcare.config.AppointmentDemoSeeder;
import com.petcare.domain.Role;
import com.petcare.domain.appointment.AppointmentStatus;
import com.petcare.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:appointment_demo;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "demo"}) @Import(AppointmentDemoTest.FixedTime.class)
class AppointmentDemoTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
    @Autowired AppointmentRepository appointments;
    @Autowired UserRepository users;
    @Autowired AppointmentDemoSeeder seeder;
    @Autowired PasswordEncoder encoder;
    @Test @Transactional void seedsStaffAndStatesWithoutDuplicateOrBlockingCancelledSlots() {
        assertThat(users.findByRoleOrderByIdAsc(Role.STAFF)).hasSizeGreaterThanOrEqualTo(2);
        var second = users.findByEmail("appointment.staff2@petcare.vn").orElseThrow();
        assertThat(encoder.matches("Staff@123", second.getPassword())).isTrue();
        var rows = appointments.findAll();
        assertThat(rows).hasSize(5).extracting(a -> a.getStateName()).contains(AppointmentStatus.BOOKED,
            AppointmentStatus.IN_PROGRESS, AppointmentStatus.DONE, AppointmentStatus.CANCELLED);
        var booked = rows.stream().filter(a -> a.getStateName() == AppointmentStatus.BOOKED).toList();
        assertThat(booked).hasSize(2);
        assertThat(booked.get(0).getStartTime()).isEqualTo(booked.get(1).getStartTime());
        assertThat(booked.get(0).getStaff().getId()).isNotEqualTo(booked.get(1).getStaff().getId());
        for (var a : booked) assertThat(appointments.countOverlaps(a.getStaff().getId(), a.getStartTime(), a.getEndTime(), AppointmentStatus.CANCELLED)).isEqualTo(1);
        seeder.seed();
        assertThat(appointments.count()).isEqualTo(5);
    }
}
