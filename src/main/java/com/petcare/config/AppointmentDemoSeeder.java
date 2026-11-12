package com.petcare.config;

import com.petcare.domain.*;
import com.petcare.repository.*;
import com.petcare.service.AppointmentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Seed demo sau các CommandLineRunner cũ bằng sự kiện Spring; chạy lại không nhân đôi lịch. */
@Component @Profile("demo") @RequiredArgsConstructor
public class AppointmentDemoSeeder {
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final AppointmentServiceRepository services;
    private final PasswordEncoder encoder;
    private final AppointmentPolicy policy;
    private final AppointmentHours hours;

    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void seed() {
        for (int i = 1; i <= 2; i++) {
            String email = "appointment.staff" + i + "@petcare.vn";
            if (users.findByEmail(email).isEmpty()) users.save(new User("Nhân viên lịch hẹn " + i, email,
                encoder.encode("Staff@123"), "", Role.STAFF));
        }
        if (appointments.count() != 0) return;
        var staff = users.findByRoleOrderByIdAsc(Role.STAFF);
        var customers = users.findByRoleOrderByIdAsc(Role.CUSTOMER);
        var bookable = services.findBookable();
        if (staff.size() < 2 || customers.isEmpty() || bookable.isEmpty()) return;
        var service = bookable.stream().filter(s -> s.getDurationMinutes() != null && s.getDurationMinutes() > 0
            && s.getDurationMinutes() <= java.time.Duration.between(hours.getOpen(), hours.getClose()).toMinutes()).findFirst();
        if (service.isEmpty()) return;
        var day = workingDay(policy.today().plusDays(1), 1);
        var history = workingDay(policy.today().minusDays(1), -1);
        var later = workingDay(day.plusDays(1), 1);
        var customer = customers.getFirst();
        var a = Appointment.create(customer, "Milo", Species.CAT, service.get(), staff.get(0), day.atTime(hours.getOpen()), "Lịch mẫu nhân viên thứ nhất");
        var b = Appointment.create(customer, "Bông", Species.DOG, service.get(), staff.get(1), day.atTime(hours.getOpen()), "Cùng giờ, khác nhân viên");
        var cancelled = Appointment.create(customer, "Mun", Species.CAT, service.get(), staff.get(0), day.atTime(hours.getOpen()), "Lịch đã hủy không chiếm chỗ");
        cancelled.cancel();
        var done = Appointment.create(customer, "Lucky", Species.DOG, service.get(), staff.get(0), history.atTime(hours.getOpen()), "Lịch sử mẫu");
        done.start(); done.finish();
        var inProgress = Appointment.create(customer, "Cún", Species.DOG, service.get(), staff.get(0), later.atTime(hours.getOpen()), "Minh họa trạng thái đang thực hiện");
        inProgress.start();
        appointments.saveAll(java.util.List.of(a, b, cancelled, done, inProgress));
    }
    private LocalDate workingDay(LocalDate day, int direction) {
        while (!policy.isWorkingDay(day)) day = day.plusDays(direction);
        return day;
    }
}
