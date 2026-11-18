package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.repository.ProductRepository;
import com.petcare.service.ProductService;
import com.petcare.service.CommerceLocks;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.context.SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:product_search;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class ProductServiceTest {
    @org.springframework.beans.factory.annotation.Autowired ProductRepository repository;
    @org.springframework.beans.factory.annotation.Autowired ProductService service;
    private final ProductFactory factory = new ProductFactory();

    private Product product(long id, ProductType type, String name, String price) {
        var p = factory.create(ProductFactoryTest.form(type, name, price));
        repository.saveAndFlush(p);
        return p;
    }

    @Test void filtersGroupNameAndHiddenProducts() {
        var hidden = product(3, ProductType.PET, "Mèo ẩn", "10");
        hidden.hide();
        var visible = List.of(
            product(1, ProductType.PET, "Mèo con", "100"),
            product(2, ProductType.ACCESSORY, "Đồ chơi mèo", "50"));
        assertThat(service.search("PET", " MÈO ", 0, false, false).getContent())
            .extracting(v -> v.name()).containsExactly("Mèo con");
        assertThat(service.search("", "mèo", 0, false, true).getTotalElements()).isEqualTo(3);
        assertThat(service.search("SERVICE", "", 0, false, false)).isEmpty();

    }

    @Test void sortsByCalculatedPriceAndPaginates() {
        var grooming = product(1, ProductType.GROOMING, "Tắm", "100");
        var accessory = product(2, ProductType.ACCESSORY, "Bóng", "120");
        assertThat(service.search("", "", 0, false, false).getContent()).extracting(v -> v.id()).containsExactly(accessory.getId(), grooming.getId());
        assertThat(service.search("", "", 0, true, false).getContent()).extracting(v -> v.id()).containsExactly(grooming.getId(), accessory.getId());
        for (int i = 0; i < 13; i++) product(i, ProductType.PET, "Thú " + i, "100");
        assertThat(service.search("PET", "", 0, false, false).getNumberOfElements()).isEqualTo(12);
        assertThat(service.search("PET", "", 1, false, false).getNumberOfElements()).isOne();
        assertThat(service.search("PET", "", 2, false, false)).isEmpty();
    }
}
