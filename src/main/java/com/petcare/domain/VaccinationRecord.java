package com.petcare.domain;

import com.petcare.domain.vaccination.*;
import com.petcare.event.VaccinationStatusChangedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Hồ sơ tiêm giữ dữ liệu và ủy quyền quyết định cho State; không cung cấp setter bỏ qua quy tắc. */
@Entity
@Table(name = "vaccination_records", indexes = {
    @Index(name = "idx_vaccination_state", columnList = "state_name"),
    @Index(name = "idx_vaccination_pet", columnList = "owned_pet_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaccinationRecord extends AbstractAggregateRoot<VaccinationRecord> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owned_pet_id", nullable = false) private OwnedPet ownedPet;
    @Column(nullable = false, length = 100) private String vaccineName;
    @Column(nullable = false) private LocalDate scheduledDate;
    private LocalDate administeredDate;
    /** Năm thứ mấy kể từ mốc 12 tuần; null là mũi cơ bản hoặc lịch nhập riêng. */
    @Column(updatable = false) private Integer annualDoseYear;
    @Column(length = 2000) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private VaccinationStatus stateName = VaccinationStatus.SCHEDULED;

    public static VaccinationRecord schedule(OwnedPet pet, String vaccineName, LocalDate date,
                                             String note, LocalDate today, int dueSoonDays) {
        var record = new VaccinationRecord();
        record.ownedPet = Objects.requireNonNull(pet, "Thú cưng không được trống.");
        record.vaccineName = PetHealthValidation.required(vaccineName, 100, "Tên vaccine");
        record.note = PetHealthValidation.optional(note, 2000, "Ghi chú");
        record.getState().reschedule(record, date);
        record.scheduledDate = date;
        record.checkTime(today, dueSoonDays);
        return record;
    }

    /** Mũi nhắc giữ số năm trong chu kỳ, không nhận diện bằng cách phân tích tên vaccine. */
    public static VaccinationRecord annualBooster(OwnedPet pet, String vaccineName, int year,
                                                  String note, LocalDate today, int dueSoonDays) {
        if (year < 1) throw new IllegalArgumentException("Năm nhắc lại phải lớn hơn 0.");
        Objects.requireNonNull(pet, "Thú cưng không được trống.");
        var record = schedule(pet, vaccineName, pet.getBirthDate().plusWeeks(12).plusYears(year),
            note, today, dueSoonDays);
        record.annualDoseYear = year;
        return record;
    }

    /**
     * Giữ chu kỳ gốc khi tiêm trễ/đổi lịch, kể cả năm nhuận; mũi mới là hồ sơ độc lập.
     * Service kiểm tra tồn tại và lưu trong cùng giao dịch hoàn tất.
     */
    public Optional<VaccinationRecord> nextAnnualBooster(LocalDate today, int dueSoonDays) {
        if (stateName != VaccinationStatus.COMPLETED)
            throw new IllegalStateException("Chỉ sinh mũi kế tiếp sau khi hoàn tất lịch tiêm.");
        if (annualDoseYear == null) return Optional.empty();
        return Optional.of(annualBooster(ownedPet, vaccineName, annualDoseYear + 1, note, today, dueSoonDays));
    }

    @Transient public VaccinationState getState() {
        return new VaccinationStateFactory(VaccinationStateFactory.DEFAULT_DUE_SOON_DAYS).from(stateName);
    }

    public boolean checkTime(LocalDate today, int dueSoonDays) {
        Objects.requireNonNull(today, "Ngày hiện tại không được trống.");
        var next = new VaccinationStateFactory(dueSoonDays).from(stateName).checkTime(this, today);
        return transitionTo(next, today);
    }

    public void complete(LocalDate date, LocalDate today) {
        Objects.requireNonNull(today, "Ngày hiện tại không được trống.");
        var next = getState().complete(this, date);
        if (date.isAfter(today)) throw new IllegalArgumentException("Ngày đã tiêm không được ở tương lai.");
        administeredDate = date;
        transitionTo(next, today);
    }

    public void reschedule(LocalDate newDate, LocalDate today, int dueSoonDays) {
        Objects.requireNonNull(today, "Ngày hiện tại không được trống.");
        var factory = new VaccinationStateFactory(dueSoonDays);
        var restart = getState().reschedule(this, newDate);
        scheduledDate = newDate;
        transitionTo(factory.from(restart).checkTime(this, today), today);
    }

    private boolean transitionTo(VaccinationStatus next, LocalDate today) {
        if (next == stateName) return false;
        var previous = stateName;
        stateName = next;
        if (getState().notifiesStatusChange())
            registerEvent(new VaccinationStatusChangedEvent(this, previous, next, today));
        return true;
    }
}
