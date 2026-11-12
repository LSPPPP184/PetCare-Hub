package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.order.OrderAction;
import com.petcare.domain.order.OrderStatus;
import com.petcare.dto.OrderSummaryView;
import com.petcare.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm tra projection và số truy vấn thật, không để N+1 bị che bởi persistence context. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:order_summary_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderSummaryRepositoryTest {
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    private Long customerId;
    private Long firstId;
    private Long secondId;
    private Long otherId;
    private static final LocalDateTime PLACED_AT = LocalDateTime.of(2026, 1, 1, 10, 0);

    private Order place(User customer, Product accessory, Product pet, int quantity) {
        var cart = new Cart(customer);
        cart.add(accessory, quantity);
        if (pet != null) cart.add(pet, 1);
        return orders.saveAndFlush(Order.fromCart(cart, "Địa chỉ thử nghiệm"));
    }
    @BeforeEach void setup() {
        var customer = users.saveAndFlush(new User("Khách thứ nhất", "first-summary@petcare.vn", "test-hash", "", Role.CUSTOMER));
        var other = users.saveAndFlush(new User("Khách thứ hai", "other-summary@petcare.vn", "test-hash", "", Role.CUSTOMER));
        customerId = customer.getId();
        var form = ProductFactoryTest.form(ProductType.ACCESSORY, "Phụ kiện", "100.00");
        form.setStockQuantity(10);
        var accessory = products.saveAndFlush(new ProductFactory().create(form));
        var pet = products.saveAndFlush(new ProductFactory().create(ProductFactoryTest.form(ProductType.PET, "Thú cưng", "200.00")));
        var first = place(customer, accessory, pet, 3);
        var second = place(customer, accessory, null, 5);
        var otherOrder = place(other, accessory, null, 2);
        second.confirm();
        otherOrder.cancel();
        firstId = first.getId();
        secondId = second.getId();
        otherId = otherOrder.getId();
        // Đổi giá hiện tại để bảo đảm tóm tắt lấy số tiền đã chốt, không tính lại từ sản phẩm.
        var changed = accessory.toForm();
        changed.setBasePrice(new BigDecimal("999.00"));
        accessory.updateCatalog(changed);
        entityManager.flush();
        entityManager.createNativeQuery("update orders set created_at = :createdAt")
            .setParameter("createdAt", PLACED_AT).executeUpdate();
        entityManager.clear();
        entityManagerFactory.unwrap(SessionFactory.class).getStatistics().clear();
    }

    @Test void summaryContainsSnapshotMoneyLineCountAndStateActionsWithoutLoadingEntities() {
        var summaries = orders.findSummaries(null, null);
        assertThat(summaries).extracting(OrderSummaryView::id).containsExactly(otherId, secondId, firstId);
        var first = summaries.getLast();
        assertThat(first.createdAt()).isEqualTo(PLACED_AT);
        assertThat(first.customerName()).isEqualTo("Khách thứ nhất");
        assertThat(first.itemCount()).isEqualTo(2);
        assertThat(first.finalAmount()).isEqualByComparingTo("500.00");
        assertThat(first.stateName()).isEqualTo(OrderStatus.PENDING);
        assertThat(first.getStatus().label()).isEqualTo("Chờ xác nhận");
        assertThat(first.getStatus().badgeClass()).isEqualTo("text-bg-warning");
        assertThat(first.getActions()).containsExactly(OrderAction.CONFIRM, OrderAction.CANCEL);
        assertThat(first.isCancellable()).isTrue();
        assertThat(summaries.get(1).itemCount()).isEqualTo(1);
        assertThat(summaries.get(1).getActions()).containsExactly(OrderAction.SHIP, OrderAction.CANCEL);
        assertThat(summaries.getFirst().getActions()).isEmpty();
        assertThat(summaries.getFirst().isCancellable()).isFalse();

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount()).isZero();
        assertThat(statistics.getCollectionLoadCount()).isZero();
    }

    @Test void filtersOwnerAndStatusInDatabase() {
        assertThat(orders.findSummaries(customerId, null)).extracting(OrderSummaryView::id)
            .containsExactly(secondId, firstId);
        assertThat(orders.findSummaries(null, OrderStatus.CANCELLED)).extracting(OrderSummaryView::id)
            .containsExactly(otherId);
        assertThat(orders.findSummaries(customerId, OrderStatus.CONFIRMED)).extracting(OrderSummaryView::id)
            .containsExactly(secondId);
        assertThat(orders.findSummaries(customerId, OrderStatus.CANCELLED)).isEmpty();
    }
}
