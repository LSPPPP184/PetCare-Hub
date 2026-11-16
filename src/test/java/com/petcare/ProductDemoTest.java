package com.petcare;

import com.petcare.config.ProductDemoSeeder;
import com.petcare.domain.Product;
import com.petcare.domain.ProductType;
import com.petcare.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:product_demo_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "demo"})
class ProductDemoTest {
    @Autowired ProductRepository repository;
    @Autowired ProductDemoSeeder seeder;

    @Test void seedsExpectedCountsAndDoesNotDuplicate() {
        var products = repository.findAll();
        // GĐ6 bổ sung 8 thú cưng có khóa seed ổn định, giữ nguyên 19 sản phẩm GĐ2.
        assertThat(products).hasSize(27);
        assertThat(products).filteredOn(p -> p.getType() == ProductType.PET).hasSize(14);
        assertThat(products).filteredOn(p -> p.getType() == ProductType.ACCESSORY).hasSize(8);
        assertThat(products).filteredOn(p -> p.getType() == ProductType.GROOMING).hasSize(2);
        assertThat(products).filteredOn(p -> p.getType() == ProductType.VET).hasSize(2);
        assertThat(products).filteredOn(p -> p.getType() == ProductType.MONTHLY).hasSize(1);
        seeder.run();
        assertThat(repository.findAll()).extracting(Product::getId)
            .containsExactlyInAnyOrderElementsOf(products.stream().map(Product::getId).toList());
    }
}
