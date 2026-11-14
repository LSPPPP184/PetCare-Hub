package com.petcare.service;

import com.petcare.config.MutableClock;
import com.petcare.domain.Role;
import com.petcare.scheduler.VaccinationScheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Đổi Clock demo rồi gọi scheduler qua proxy để trạng thái được lưu trong giao dịch. */
@Service @Profile("demo")
public class DemoDateService {
    private final MutableClock clock;
    private final ObjectProvider<VaccinationScheduler> scheduler;
    private final CommerceAccess access;

    public DemoDateService(@Qualifier("healthClock") MutableClock clock,
                           ObjectProvider<VaccinationScheduler> scheduler, CommerceAccess access) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.access = access;
    }

    public LocalDate today() { return LocalDate.now(clock); }
    // Scheduler phải hoàn tất giao dịch riêng trước khi công bố Clock, kể cả khi bên gọi có transaction.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void simulate(LocalDate date) {
        if (access.currentStaff().getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên được giả lập ngày.");
        var task = scheduler.getIfAvailable();
        if (task == null) throw new IllegalArgumentException("Hãy bật scheduler tiêm phòng trước khi giả lập.");
        clock.advanceAndRun(date, () -> {
            try {
                task.checkSchedules();
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Không thể giả lập ngày. Đã giữ nguyên Clock và lịch tiêm; vui lòng thử lại.", ex);
            }
        });
    }
}
