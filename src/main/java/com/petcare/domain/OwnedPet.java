package com.petcare.domain;

import com.petcare.domain.order.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Objects;

/** Thú cưng thuộc khách hàng, độc lập với Pet là sản phẩm trong danh mục bán hàng. */
@Entity
@Table(name = "owned_pets", indexes = @Index(name = "idx_owned_pet_owner", columnList = "owner_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnedPet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Species species;
    @Column(length = 100) private String breed;
    @Column(nullable = false) private LocalDate birthDate;
    @Column(length = 30) private String gender;
    @Column(length = 2000) private String note;

    public static OwnedPet create(User owner, String name, Species species, String breed,
                                  LocalDate birthDate, String gender, String note, LocalDate today) {
        if (owner == null || owner.getRole() != Role.CUSTOMER)
            throw new IllegalArgumentException("Chủ thú cưng phải là khách hàng.");
        Objects.requireNonNull(today, "Ngày hiện tại không được trống.");
        if (birthDate == null || birthDate.isAfter(today))
            throw new IllegalArgumentException("Ngày sinh không được trống hoặc ở tương lai.");
        var pet = new OwnedPet();
        pet.owner = owner;
        pet.name = PetHealthValidation.required(name, 100, "Tên thú cưng");
        pet.species = Objects.requireNonNull(species, "Loài không được trống.");
        pet.breed = PetHealthValidation.optional(breed, 100, "Giống");
        pet.birthDate = birthDate;
        pet.gender = PetHealthValidation.optional(gender, 30, "Giới tính");
        pet.note = PetHealthValidation.optional(note, 2000, "Ghi chú");
        return pet;
    }

    /** Tạo hồ sơ từ đúng Pet trong đơn hoàn tất; ngày sinh phải được xác nhận, không suy từ tuổi tháng. */
    public static OwnedPet fromCompletedOrder(Order order, Pet pet, LocalDate birthDate, LocalDate today) {
        if (order == null || order.getStateName() != OrderStatus.COMPLETED || pet == null
                || order.getItems().stream().noneMatch(item -> item.getProduct() == pet
                    || (pet.getId() != null && pet.getId().equals(item.getProduct().getId()))))
            throw new IllegalArgumentException("Chỉ tạo hồ sơ cho thú cưng thuộc đơn hàng đã hoàn tất.");
        return create(order.getCustomer(), pet.getName(), pet.getSpecies(), pet.getBreed(), birthDate,
            pet.getGender(), pet.getHealthNote(), today);
    }
}
