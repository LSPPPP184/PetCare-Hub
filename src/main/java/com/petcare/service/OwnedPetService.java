package com.petcare.service;

import com.petcare.config.VaccinationProperties;
import com.petcare.domain.OwnedPet;
import com.petcare.domain.vaccination.VaccinationScheduleTemplate;
import com.petcare.repository.OwnedPetRepository;
import com.petcare.repository.VaccinationRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

/** Lưu thú cưng mới và lịch mẫu trong cùng giao dịch, dùng Clock của phạm vi sức khỏe. */
@Service
public class OwnedPetService {
    private final OwnedPetRepository pets;
    private final VaccinationRecordRepository vaccinations;
    private final VaccinationProperties properties;
    private final CommerceAccess access;
    private final Clock clock;

    public OwnedPetService(OwnedPetRepository pets, VaccinationRecordRepository vaccinations,
                           VaccinationProperties properties, CommerceAccess access,
                           @Qualifier("healthClock") Clock clock) {
        this.pets = pets;
        this.vaccinations = vaccinations;
        this.properties = properties;
        this.access = access;
        this.clock = clock;
    }

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
