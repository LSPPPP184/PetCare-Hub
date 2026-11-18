package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.purchase.*;
import com.petcare.dto.ProductForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class CartPurchaseTest {
    static Product product(ProductType type) {
        var form = new ProductForm();
        form.setType(type);
        form.setName("Sản phẩm");
        form.setBasePrice(new BigDecimal("100.25"));
        form.setStockQuantity(5);
        form.setSpecies(Species.CAT);
        form.setPetSizeSurcharge(new BigDecimal("10.00"));
        var product = new ProductFactory().create(form);
        ReflectionTestUtils.setField(product, "id", 1L);
        return product;
    }
    private Cart cart() { return new Cart(new User("Khách", "test@petcare.vn", "hash", "", Role.CUSTOMER)); }

    @Test void petHasOneUnitAndMustBeAvailable() {
        var pet = product(ProductType.PET);
        var cart = cart();
        assertThatThrownBy(() -> cart.add(pet, 2)).isInstanceOf(PurchaseException.class);
        cart.add(pet, 1);
        assertThatThrownBy(() -> cart.add(pet, 1)).isInstanceOf(PurchaseException.class);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(1);
        PurchasePolicies.forProduct(pet).consume(pet, 1);
        assertThat(((Pet) pet).getStatus()).isEqualTo(PetStatus.SOLD);
        assertThatThrownBy(() -> cart().add(pet, 1)).isInstanceOf(PurchaseException.class);
        PurchasePolicies.forProduct(pet).restore(pet, 1);
        assertThat(((Pet) pet).getStatus()).isEqualTo(PetStatus.AVAILABLE);
        var form = pet.toForm();
        form.setStatus(PetStatus.RESERVED);
        pet.update(form);
        assertThatThrownBy(() -> cart().add(pet, 1)).isInstanceOf(PurchaseException.class);
    }
    @Test void accessoryAggregatesQuantityAndRestoresStock() {
        var product = product(ProductType.ACCESSORY);
        var cart = cart();
        cart.add(product, 2);
        cart.add(product, 3);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(5);
        assertThatThrownBy(() -> cart.add(product, 1)).isInstanceOf(PurchaseException.class);
        assertThatThrownBy(() -> cart.setQuantity(1L, 0)).isInstanceOf(PurchaseException.class);
        var policy = PurchasePolicies.forProduct(product);
        policy.consume(product, 5);
        assertThat(((Accessory) product).getStockQuantity()).isZero();
        policy.restore(product, 5);
        assertThat(((Accessory) product).getStockQuantity()).isEqualTo(5);
        assertThat(product.getName()).isEqualTo("Sản phẩm");
    }
    @ParameterizedTest @EnumSource(value = ProductType.class, names = {"GROOMING", "VET"})
    void appointmentsCannotEnterCart(ProductType type) {
        assertThatThrownBy(() -> cart().add(product(type), 1)).isInstanceOf(PurchaseException.class)
            .hasMessageContaining("lịch hẹn");
    }
    @Test void monthlyPackageCanBePurchasedWithoutPhysicalStock() {
        var product = product(ProductType.MONTHLY);
        product.configureCoveredServices(java.util.List.of((ServiceItem) product(ProductType.GROOMING)));
        var cart = cart();
        cart.add(product, 2);
        var policy = PurchasePolicies.forProduct(product);
        policy.consume(product, 2);
        policy.restore(product, 2);
        assertThat(cart.getItems()).hasSize(1);
    }
    @Test void hiddenProductAndNonPositiveQuantityAreRejected() {
        var product = product(ProductType.ACCESSORY);
        assertThatThrownBy(() -> cart().add(product, -1)).isInstanceOf(PurchaseException.class);
        product.hide();
        assertThatThrownBy(() -> cart().add(product, 1)).isInstanceOf(PurchaseException.class);
    }
    @Test void orderSnapshotsPriceAndRejectsEmptyCartOrAddress() {
        var cart = cart();
        assertThatThrownBy(() -> Order.fromCart(cart, "Địa chỉ")).isInstanceOf(PurchaseException.class);
        var product = product(ProductType.ACCESSORY);
        cart.add(product, 2);
        assertThatThrownBy(() -> Order.fromCart(cart, " ")).isInstanceOf(PurchaseException.class);
        var order = Order.fromCart(cart, " Địa chỉ ");
        var form = product.toForm();
        form.setBasePrice(new BigDecimal("999"));
        product.update(form);
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("100.25");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.50");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(order.getFinalAmount()).isEqualByComparingTo("200.50");
        assertThatThrownBy(() -> order.getItems().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
