package com.petcare.config;

import com.petcare.domain.*;
import com.petcare.dto.*;
import com.petcare.repository.*;
import com.petcare.service.CommerceLocks;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Seed GĐ6 theo khóa cố định; chỉ bổ sung hồ sơ thiếu, không ghi đè dữ liệu đã chỉnh hoặc trạng thái bán. */
@Component @Profile("demo") @RequiredArgsConstructor
public class MatchingDemoSeeder {
    private final MatchingPetRepository pets;
    private final UserRepository users;
    private final CommerceLocks locks;

    private record Sample(String key, String name, Species species, String breed, int size, int energy,
                          boolean childFriendly, int shedding, int space, int cost, int experience, double hours) {
        PetProfileForm profile() {
            var f = new PetProfileForm();
            f.setSizeLevel(size); f.setEnergyLevel(energy); f.setChildFriendly(childFriendly);
            f.setSheddingLevel(shedding); f.setSpaceNeed(space); f.setCareCostLevel(cost);
            f.setExperienceNeeded(experience); f.setTimeNeedHoursPerDay(hours);
            return f;
        }
    }

    private static final List<Sample> SAMPLES = List.of(
        new Sample("matching-poodle", "Bông – chó Poodle", Species.DOG, "Poodle", 2, 3, true, 1, 2, 3, 2, 2),
        new Sample("matching-golden", "Nắng – chó Golden", Species.DOG, "Golden Retriever", 5, 4, true, 5, 5, 4, 2, 3),
        new Sample("matching-husky", "Tuyết – chó Husky", Species.DOG, "Husky", 4, 5, false, 5, 5, 5, 5, 4),
        new Sample("matching-corgi", "Bắp – chó Corgi", Species.DOG, "Corgi", 3, 4, true, 4, 3, 3, 3, 2.5),
        new Sample("matching-british", "Mây – mèo Anh lông ngắn", Species.CAT, "Anh lông ngắn", 3, 1, true, 3, 2, 3, 1, 1),
        new Sample("matching-siamese", "Mun – mèo Xiêm", Species.CAT, "Xiêm", 2, 4, false, 2, 2, 2, 3, 2),
        new Sample("matching-rabbit", "Bông Gòn – thỏ lùn", Species.OTHER, "Thỏ lùn", 1, 2, true, 3, 2, 2, 2, 1.5),
        new Sample("matching-hamster", "Hạt Dẻ – chuột Hamster", Species.OTHER, "Hamster", 1, 1, false, 1, 1, 1, 1, 0.5));

    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void seed() {
        users.findByEmail("admin@petcare.vn").ifPresent(u -> locks.lock(User.class, u.getId()));
        for (var pet : pets.findAll()) {
            if (pet.getProfile() == null) {
                var locked = locks.lock(Pet.class, pet.getId());
                if (locked.getProfile() == null) {
                    locked.updateProfile(SAMPLES.get(Math.floorMod(pet.getId().intValue(), SAMPLES.size())).profile());
                    locked.getProfile().assignSeedKey("matching-existing-" + pet.getId());
                }
            }
        }
        for (var sample : SAMPLES) {
            if (pets.findByProfileSeedKey(sample.key()).isPresent()) continue;
            var form = new ProductForm();
            form.setName(sample.name()); form.setSpecies(sample.species()); form.setBreed(sample.breed());
            form.setDescription("Thú cưng minh họa ghép đôi; thông số dùng để trình bày thuật toán.");
            form.setBasePrice(new BigDecimal("500000")); form.setAgeMonths(8); form.setGender("Đực");
            form.setHealthNote("Hồ sơ minh họa, cần trao đổi thêm với cửa hàng.");
            form.setProfile(sample.profile());
            var pet = new Pet(); pet.update(form); pet.getProfile().assignSeedKey(sample.key());
            pets.save(pet);
        }
    }
}
