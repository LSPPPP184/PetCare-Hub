package com.petcare.config;

import com.petcare.domain.*;
import com.petcare.domain.vaccination.VaccinationScheduleTemplate;
import com.petcare.repository.*;
import com.petcare.service.CommerceLocks;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;

/** Hai hồ sơ minh họa theo ngày chạy; seed lại không nhân đôi hoặc ghi đè thao tác demo. */
@Component @Profile("demo")
public class PetHealthDemoSeeder {
    private static final String COMPLETED_MARKER = "Mũi đã tiêm (demo)";
    private final UserRepository users;
    private final OwnedPetRepository pets;
    private final VaccinationRecordRepository vaccinations;
    private final HealthRecordRepository health;
    private final VaccinationProperties properties;
    private final Clock clock;
    private final CommerceLocks locks;

    public PetHealthDemoSeeder(UserRepository users, OwnedPetRepository pets, VaccinationRecordRepository vaccinations,
                               HealthRecordRepository health, VaccinationProperties properties,
                               @Qualifier("healthClock") Clock clock, CommerceLocks locks) {
        this.users = users;
        this.pets = pets;
        this.vaccinations = vaccinations;
        this.health = health;
        this.properties = properties;
        this.clock = clock;
        this.locks = locks;
    }

    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void seed() {
        var owner = users.findByEmail("customer@petcare.vn");
        if (owner.isEmpty()) return;
        // Khóa chủ trong suốt giao dịch để hai lượt seed không cùng tạo một khóa.
        var lockedOwner = locks.lock(User.class, owner.get().getId());
        var today = LocalDate.now(clock);
        seedPet(lockedOwner, "health-demo-milo", "Milo bé (demo)", Species.CAT, today.minusWeeks(6).plusDays(3), today);
        seedPet(lockedOwner, "health-demo-lucky", "Lucky lớn (demo)", Species.DOG, today.minusYears(5), today);
    }
    private void seedPet(User owner, String seedKey, String name, Species species, LocalDate birth, LocalDate today) {
        if (pets.existsByOwnerIdAndSeedKey(owner.getId(), seedKey)) return;
        var legacy = pets.findLegacyDemoPets(owner.getId(), species, COMPLETED_MARKER);
        if (legacy.size() > 1)
            throw new IllegalStateException("Có nhiều hồ sơ demo cũ cùng loài; cần xác định hồ sơ gốc trước khi gán khóa seed.");
        if (!legacy.isEmpty()) {
            legacy.getFirst().assignSeedKey(seedKey);
            pets.save(legacy.getFirst());
            return;
        }
        var pet = OwnedPet.create(owner, name, species, "Giống mẫu", birth, "Đực",
            "Dữ liệu mẫu, không phải tư vấn thú y.", today);
        pet.assignSeedKey(seedKey);
        pets.save(pet);
        vaccinations.saveAll(new VaccinationScheduleTemplate(properties.getAnnualBoosterYears(), clock)
            .generate(pet, properties.getDueSoonDays()));
        var completed = VaccinationRecord.schedule(pet, COMPLETED_MARKER, today.minusDays(1),
            "Dữ liệu mẫu, không phải tư vấn thú y.", today, properties.getDueSoonDays());
        completed.complete(today.minusDays(1), today);
        vaccinations.save(completed);
        health.save(new HealthRecord(pet, today, "Khám tổng quát (demo)", "Hồ sơ minh họa sức khỏe.",
            new BigDecimal("2.50"), "Bác sĩ mẫu"));
    }
}
