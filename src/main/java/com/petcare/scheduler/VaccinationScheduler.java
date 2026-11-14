package com.petcare.scheduler;

import com.petcare.config.VaccinationProperties;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.repository.VaccinationRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

/** Kiểm tra hằng ngày bằng Clock inject; State tự quyết định chuyển tiếp, tác vụ chỉ điều phối lưu. */
@Component
@ConditionalOnProperty(prefix = "petcare.vaccinations.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VaccinationScheduler {
    private final VaccinationRecordRepository records;
    private final VaccinationProperties properties;
    private final Clock clock;

    public VaccinationScheduler(VaccinationRecordRepository records, VaccinationProperties properties,
                                @Qualifier("healthClock") Clock clock) {
        this.records = records;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${petcare.vaccinations.scheduler.cron:0 0 1 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void checkSchedules() {
        LocalDate today = LocalDate.now(clock);
        for (var record : records.findByStateNameNot(VaccinationStatus.COMPLETED)) {
            if (record.checkTime(today, properties.getDueSoonDays())) {
                // save cần thiết để Spring Data phát và xóa hàng đợi sự kiện domain.
                records.save(record);
            }
        }
    }
}
