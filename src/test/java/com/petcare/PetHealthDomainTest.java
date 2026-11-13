package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static com.petcare.VaccinationStateTest.*;

class PetHealthDomainTest {
    @Test void ownedPetRequiresCustomerAndValidBirthDate() {
        var owner = pet(Species.DOG, TODAY).getOwner();
        assertThatThrownBy(() -> OwnedPet.create(owner, "Milo", Species.DOG, "", TODAY.plusDays(1), "", "", TODAY))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwnedPet.create(owner, " ", Species.DOG, "", TODAY, "", "", TODAY))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwnedPet.create(owner, "Milo", Species.DOG, "", null, "", "", TODAY))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwnedPet.create(owner, "Milo", null, "", TODAY, "", "", TODAY))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest @EnumSource(value = Role.class, names = {"STAFF", "ADMIN"})
    void staffAndAdminCannotBeCustomerPetOwners(Role role) {
        var owner = new User("Nhân viên", "staff@test.vn", "hash", "", role);
        assertThatThrownBy(() -> OwnedPet.create(owner, "Milo", Species.DOG, "", TODAY, "", "", TODAY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void healthRecordPreservesDetailsAndRejectsInvalidWeightOrDate() {
        var pet = pet(Species.CAT, TODAY.minusYears(1));
        var record = new HealthRecord(pet, TODAY, " Khám định kỳ ", " Khỏe mạnh ", new BigDecimal("4.25"), " Bác sĩ A ");
        assertThat(record.getOwnedPet()).isSameAs(pet);
        assertThat(record.getType()).isEqualTo("Khám định kỳ");
        assertThat(record.getDescription()).isEqualTo("Khỏe mạnh");
        assertThat(record.getVetName()).isEqualTo("Bác sĩ A");
        assertThat(record.getWeight()).isEqualByComparingTo("4.25");
        assertThat(new HealthRecord(pet, TODAY, "Khám", "Khỏe", null, null).getWeight()).isNull();
        for (String invalid : new String[]{"0", "-1", "1.234", "100000"}) {
            assertThatThrownBy(() -> new HealthRecord(pet, TODAY, "Khám", "Khỏe", new BigDecimal(invalid), ""))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new HealthRecord(pet, pet.getBirthDate().minusDays(1), "Khám", "Khỏe", null, ""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HealthRecord(pet, TODAY, "", "Khỏe", null, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void completedPurchaseCanCreateIndependentPetWithoutChangingCatalogOrOrder() {
        var form = ProductFactoryTest.form(ProductType.PET, "Milo", "100");
        form.setSpecies(Species.DOG);
        form.setBreed("Giống mẫu");
        form.setGender("Đực");
        form.setHealthNote("Khỏe mạnh");
        Pet product = (Pet) new ProductFactory().create(form);
        var cart = new Cart(pet(Species.DOG, TODAY).getOwner());
        cart.add(product, 1);
        var order = Order.fromCart(cart, "Địa chỉ");
        assertThatThrownBy(() -> OwnedPet.fromCompletedOrder(order, product, TODAY, TODAY))
            .isInstanceOf(IllegalArgumentException.class);
        order.confirm(); order.ship();
        assertThatThrownBy(() -> OwnedPet.fromCompletedOrder(order, product, TODAY, TODAY))
            .isInstanceOf(IllegalArgumentException.class);
        order.complete();
        var owned = OwnedPet.fromCompletedOrder(order, product, TODAY.minusMonths(2), TODAY);
        assertThat(owned.getOwner()).isSameAs(order.getCustomer());
        assertThat(owned.getName()).isEqualTo(product.getName());
        assertThat(owned.getSpecies()).isEqualTo(product.getSpecies());
        assertThat(owned.getBreed()).isEqualTo(product.getBreed());
        assertThat(owned.getNote()).isEqualTo(product.getHealthNote());
        assertThat(owned.getBirthDate()).isEqualTo(TODAY.minusMonths(2));
        var before = owned.getName();
        form.setName("Tên danh mục mới"); product.updateCatalog(form);
        assertThat(owned.getName()).isEqualTo(before);
        var other = (Pet) new ProductFactory().create(form);
        assertThatThrownBy(() -> OwnedPet.fromCompletedOrder(order, other, TODAY, TODAY))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
