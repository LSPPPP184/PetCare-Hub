package com.petcare.service;

import com.petcare.config.VaccinationProperties;
import com.petcare.domain.OwnedPet;
import com.petcare.domain.VaccinationRecord;
import com.petcare.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

/** Hoàn tất và bổ sung mũi nhắc trong cùng giao dịch; khóa thú cưng trước hồ sơ để tránh tạo trùng. */
@Service
@RequiredArgsConstructor
public class VaccinationService {
    private final VaccinationRecordRepository records;
    private final VaccinationProperties properties;
    private final CommerceAccess access;
    private final CommerceLocks locks;
    private final Clock clock;

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
