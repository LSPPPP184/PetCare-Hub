package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.dto.*;
import com.petcare.repository.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

/** Điều phối hồ sơ sức khỏe; xác thực chủ tại service và chỉ đưa snapshot ra giao diện. */
@Service @Transactional(readOnly = true)
public class PetHealthService {
    private final OwnedPetRepository pets;
    private final VaccinationRecordRepository vaccinations;
    private final HealthRecordRepository health;
    private final OwnedPetService registration;
    private final CommerceAccess access;
    private final CommerceLocks locks;
    private final Clock clock;

    public PetHealthService(OwnedPetRepository pets, VaccinationRecordRepository vaccinations,
                            HealthRecordRepository health, OwnedPetService registration, CommerceAccess access,
                            CommerceLocks locks, @Qualifier("healthClock") Clock clock) {
        this.pets = pets;
        this.vaccinations = vaccinations;
        this.health = health;
        this.registration = registration;
        this.access = access;
        this.locks = locks;
        this.clock = clock;
    }

    public LocalDate today() { return LocalDate.now(clock); }
    public List<OwnedPetView> mine() {
        return pets.findByOwnerIdOrderByIdAsc(access.currentCustomer().getId()).stream().map(OwnedPetView::from).toList();
    }
    public List<OwnedPetView> allPets() {
        access.staff();
        return pets.findAllByOrderByIdAsc().stream().map(OwnedPetView::from).toList();
    }
    private OwnedPet pet(Long id) {
        return pets.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thú cưng."));
    }
    public PetHealthView myDetail(Long id) {
        var pet = pet(id); access.customer(pet.getOwner().getId());
        return detail(pet);
    }
    public PetHealthView staffDetail(Long id) { access.staff(); return detail(pet(id)); }
    private PetHealthView detail(OwnedPet pet) {
        return new PetHealthView(OwnedPetView.from(pet),
            vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(pet.getId()).stream().map(VaccinationView::from).toList(),
            health.findByOwnedPetIdOrderByDateDescIdDesc(pet.getId()).stream().map(HealthRecordView::from).toList());
    }
    @Transactional
    public Long create(OwnedPetForm form) {
        var pet = OwnedPet.create(access.currentCustomer(), form.getName(), form.getSpecies(), form.getBreed(),
            form.getBirthDate(), form.getGender(), form.getNote(), today());
        return registration.register(pet).getId();
    }
    @Transactional
    public void update(Long id, OwnedPetForm form) {
        var pet = locks.lock(OwnedPet.class, id);
        access.customer(pet.getOwner().getId());
        if (form.getVersion() == null || form.getVersion() != pet.getVersion())
            throw new OptimisticLockingFailureException("Hồ sơ vừa được sửa. Vui lòng tải lại trang.");
        if (form.getSpecies() != pet.getSpecies() || !pet.getBirthDate().equals(form.getBirthDate()))
            throw new IllegalArgumentException("Không đổi loài hoặc ngày sinh vì lịch tiêm đã được tạo.");
        pet.updateDetails(form.getName(), form.getBreed(), form.getGender(), form.getNote());
        pets.save(pet);
    }
    public List<VaccinationView> alerts(VaccinationStatus status) {
        access.staff();
        var states = status == null ? List.of(VaccinationStatus.DUE_SOON, VaccinationStatus.OVERDUE) : List.of(status);
        return vaccinations.findByStateNameInOrderByScheduledDateAscIdAsc(states).stream().map(VaccinationView::from).toList();
    }
    @Transactional
    public void addHealthRecord(Long petId, HealthRecordForm form) {
        access.staff();
        var pet = locks.lock(OwnedPet.class, petId);
        if (form.getDate() == null || form.getDate().isAfter(today()))
            throw new IllegalArgumentException("Ngày khám không được trống hoặc ở tương lai.");
        health.save(new HealthRecord(pet, form.getDate(), form.getType(), form.getDescription(), form.getWeight(), form.getVetName()));
    }
}
