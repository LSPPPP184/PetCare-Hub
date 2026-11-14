package com.petcare.service;

import com.petcare.config.VaccinationProperties;
import com.petcare.domain.OwnedPet;
import com.petcare.domain.VaccinationRecord;
import com.petcare.repository.VaccinationRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

/** Hoàn tất và bổ sung mũi nhắc trong cùng giao dịch; khóa thú cưng trước hồ sơ để tránh tạo trùng. */
@Service
public class VaccinationService {
    private final VaccinationRecordRepository records;
    private final VaccinationProperties properties;
    private final CommerceAccess access;
    private final CommerceLocks locks;
    private final Clock clock;

    public VaccinationService(VaccinationRecordRepository records, VaccinationProperties properties,
                              CommerceAccess access, CommerceLocks locks, @Qualifier("healthClock") Clock clock) {
        this.records = records;
        this.properties = properties;
        this.access = access;
        this.locks = locks;
        this.clock = clock;
    }

    @Transactional
    public Long rescheduleMine(Long recordId, LocalDate date, long version) {
        var record = lockedRecord(recordId);
        access.customer(record.getOwnedPet().getOwner().getId());
        return reschedule(record, date, version);
    }

    @Transactional
    public Long rescheduleByStaff(Long recordId, LocalDate date, long version) {
        access.staff();
        return reschedule(lockedRecord(recordId), date, version);
    }

    private VaccinationRecord lockedRecord(Long recordId) {
        var petId = records.findOwnedPetIdById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch tiêm."));
        locks.lock(OwnedPet.class, petId);
        return locks.lock(VaccinationRecord.class, recordId);
    }

    private Long reschedule(VaccinationRecord record, LocalDate date, long version) {
        if (record.getVersion() != version)
            throw new org.springframework.dao.OptimisticLockingFailureException("Lịch tiêm vừa được thay đổi. Vui lòng tải lại trang.");
        var today = LocalDate.now(clock);
        if (date == null || date.isBefore(today))
            throw new IllegalArgumentException("Ngày dời lịch phải từ ngày hiện tại trở đi.");
        record.reschedule(date, today, properties.getDueSoonDays());
        records.save(record);
        return record.getOwnedPet().getId();
    }

    /**
     * STAFF/ADMIN xác nhận đã tiêm. Gọi lại vẫn bị CompletedState từ chối, không sinh thêm mũi.
     * Mũi năm sau đã có (kể cả đã đổi ngày hoặc hoàn tất) thì giữ nguyên.
     */
    @Transactional
    public VaccinationRecord complete(Long recordId, LocalDate date) {
        access.staff();
        var petId = records.findOwnedPetIdById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch tiêm."));
        // Chỉ đọc ID trước khóa, tránh giữ phiên bản hồ sơ cũ trong persistence context khi chờ.
        locks.lock(OwnedPet.class, petId);
        var record = locks.lock(VaccinationRecord.class, recordId);
        LocalDate today = LocalDate.now(clock);
        record.complete(date, today);
        records.save(record);
        record.nextAnnualBooster(today, properties.getDueSoonDays()).ifPresent(next -> {
            if (!records.existsByOwnedPetIdAndVaccineNameAndAnnualDoseYear(
                    next.getOwnedPet().getId(), next.getVaccineName(), next.getAnnualDoseYear())) {
                records.save(next);
            }
        });
        return record;
    }
}
