package com.petcare.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/** Một lần ghi nhận sức khỏe; cân nặng tính theo kg và có thể chưa được đo. */
@Entity
@Table(name = "health_records", indexes = @Index(name = "idx_health_pet_date", columnList = "owned_pet_id,record_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owned_pet_id", nullable = false) private OwnedPet ownedPet;
    @Column(name = "record_date", nullable = false) private LocalDate date;
    @Column(nullable = false, length = 100) private String type;
    @Column(nullable = false, length = 2000) private String description;
    @Column(precision = 7, scale = 2) private BigDecimal weight;
    @Column(length = 100) private String vetName;

    public HealthRecord(OwnedPet ownedPet, LocalDate date, String type, String description,
                        BigDecimal weight, String vetName) {
        this.ownedPet = Objects.requireNonNull(ownedPet, "Thú cưng không được trống.");
        if (date == null || date.isBefore(ownedPet.getBirthDate()))
            throw new IllegalArgumentException("Ngày ghi nhận không được trống hoặc trước ngày sinh.");
        this.date = date;
        this.type = PetHealthValidation.required(type, 100, "Loại hồ sơ");
        this.description = PetHealthValidation.required(description, 2000, "Mô tả");
        this.vetName = PetHealthValidation.optional(vetName, 100, "Tên bác sĩ");
        if (weight != null) {
            try { this.weight = weight.setScale(2, RoundingMode.UNNECESSARY); }
            catch (ArithmeticException ex) { throw new IllegalArgumentException("Cân nặng chỉ có tối đa hai chữ số thập phân."); }
            if (this.weight.signum() <= 0 || this.weight.precision() > 7)
                throw new IllegalArgumentException("Cân nặng phải lớn hơn 0 và không vượt 99999,99 kg.");
        }
    }
}
