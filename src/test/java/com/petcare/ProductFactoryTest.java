package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.dto.ProductForm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ProductFactoryTest {
    private final ProductFactory factory = new ProductFactory();
    private static final Map<ProductType, Class<?>> CLASSES = Map.of(
        ProductType.PET, Pet.class, ProductType.ACCESSORY, Accessory.class,
        ProductType.GROOMING, GroomingService.class, ProductType.VET, VetService.class,
        ProductType.MONTHLY, MonthlyCarePackage.class);

    static ProductForm form(ProductType type, String name, String price) {
        var f = new ProductForm();
        f.setType(type);
        f.setName(name);
        f.setBasePrice(new BigDecimal(price));
        f.setPetSizeSurcharge(new BigDecimal("25.50"));
        f.setDurationMinutes(30);
        f.setDoctorName("Bác sĩ An");
        f.setDurationMonths(1);
        f.setIncludedUses(4);
        f.setIncludedServices("Tắm tỉa");
        return f;
    }

    @ParameterizedTest @EnumSource(ProductType.class)
    void createsCorrectSubtype(ProductType type) {
        var p = factory.create(form(type, "Sản phẩm", "100.10"));
        assertThat(p).isExactlyInstanceOf(CLASSES.get(type));
        assertThat(p.getType()).isEqualTo(type);
        assertThat(p.toForm().getName()).isEqualTo("Sản phẩm");
    }

    @ParameterizedTest @EnumSource(ProductType.class)
    void calculatesExactPrice(ProductType type) {
        var p = factory.create(form(type, "Sản phẩm", "100.10"));
        var expected = Map.of(ProductType.PET, "100.10", ProductType.ACCESSORY, "100.10",
            ProductType.GROOMING, "125.60", ProductType.VET, "100.10", ProductType.MONTHLY, "100.10");
        assertThat(p.calculatePrice()).isEqualByComparingTo(expected.get(type));
        assertThat(p.calculatePrice()).isEqualByComparingTo(expected.get(type));
    }
}
