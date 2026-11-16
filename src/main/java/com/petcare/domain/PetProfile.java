package com.petcare.domain;

import com.petcare.dto.PetProfileForm;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Objects;
import java.util.UUID;

/** Hồ sơ ghép đôi thuộc riêng một thú cưng; phiên bản bảo vệ form sửa đồng thời. */
@Entity @Table(name = "pet_profiles") @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false, unique = true) private Pet pet;
    @Column(nullable = false, unique = true, length = 80) private String seedKey;
    @Column(nullable = false) private int sizeLevel;
    @Column(nullable = false) private int energyLevel;
    @Column(nullable = false) private boolean childFriendly;
    @Column(nullable = false) private int sheddingLevel;
    @Column(nullable = false) private int spaceNeed;
    @Column(nullable = false) private int careCostLevel;
    @Column(nullable = false) private int experienceNeeded;
    @Column(nullable = false) private double timeNeedHoursPerDay;

    public PetProfile(Pet pet, PetProfileForm form) {
        this.pet = Objects.requireNonNull(pet);
        seedKey = "profile-" + UUID.randomUUID();
        update(form);
    }

    public void update(PetProfileForm f) {
        if (id != null && !Objects.equals(f.getVersion(), version)) throw new ProductVersionConflictException();
        sizeLevel = level(f.getSizeLevel());
        energyLevel = level(f.getEnergyLevel());
        childFriendly = f.isChildFriendly();
        sheddingLevel = level(f.getSheddingLevel());
        spaceNeed = level(f.getSpaceNeed());
        careCostLevel = level(f.getCareCostLevel());
        experienceNeeded = level(f.getExperienceNeeded());
        if (f.getTimeNeedHoursPerDay() == null || !Double.isFinite(f.getTimeNeedHoursPerDay())
            || f.getTimeNeedHoursPerDay() < 0.25 || f.getTimeNeedHoursPerDay() > 24)
            throw new IllegalArgumentException("Thời gian chăm sóc phải từ 0,25 đến 24 giờ/ngày.");
        timeNeedHoursPerDay = f.getTimeNeedHoursPerDay();
    }

    private int level(Integer value) {
        if (value == null || value < 1 || value > 5)
            throw new IllegalArgumentException("Mức hồ sơ phải từ 1 đến 5.");
        return value;
    }

    public void assignSeedKey(String key) {
        if (id != null || key == null || key.isBlank() || key.length() > 80)
            throw new IllegalArgumentException("Chỉ gán khóa seed hợp lệ lúc tạo hồ sơ.");
        seedKey = key;
    }

    public PetProfileForm toForm() {
        var f = new PetProfileForm();
        f.setVersion(version);
        f.setSizeLevel(sizeLevel); f.setEnergyLevel(energyLevel); f.setChildFriendly(childFriendly);
        f.setSheddingLevel(sheddingLevel); f.setSpaceNeed(spaceNeed); f.setCareCostLevel(careCostLevel);
        f.setExperienceNeeded(experienceNeeded); f.setTimeNeedHoursPerDay(timeNeedHoursPerDay);
        return f;
    }
}
