package com.petcare.service;

import com.petcare.config.VaccinationProperties;
import com.petcare.domain.OwnedPet;
import com.petcare.domain.vaccination.VaccinationScheduleTemplate;
import com.petcare.repository.OwnedPetRepository;
import com.petcare.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

/** Lưu thú cưng mới và lịch mẫu trong cùng giao dịch, dùng chung Clock của ứng dụng. */
@Service
@RequiredArgsConstructor
public class OwnedPetService {
    private final OwnedPetRepository pets;
    private final VaccinationRecordRepository vaccinations;
    private final VaccinationProperties properties;
    private final CommerceAccess access;
    private final Clock clock;

    @Transactional
    public OwnedPet register(OwnedPet pet) {
        access.customer(pet.getOwner().getId());
        if (pet.getId() != null) throw new IllegalArgumentException("Chỉ sinh lịch mẫu khi thêm thú cưng mới.");
        LocalDate today = LocalDate.now(clock);
        if (pet.getBirthDate().isAfter(today)) throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
        var saved = pets.save(pet);
        var template = new VaccinationScheduleTemplate(properties.getAnnualBoosterYears(), clock);
        vaccinations.saveAll(template.generate(saved, properties.getDueSoonDays()));
        return saved;
    }
}
