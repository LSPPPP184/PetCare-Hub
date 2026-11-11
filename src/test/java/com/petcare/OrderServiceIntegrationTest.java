package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.order.*;
import com.petcare.domain.purchase.PurchaseException;
import com.petcare.repository.*;
import com.petcare.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:commerce_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OrderServiceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProductService productService;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired CartRepository cartRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    private Long customerId;
    private Long otherId;

    private <T> T db(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
    private <T> T as(String email, Supplier<T> work) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(email, "", List.of()));
        SecurityContextHolder.setContext(context);
        try { return work.get(); }
        finally { SecurityContextHolder.setContext(previous); }
    }
    private <T> T customer(Supplier<T> work) { return as("customer@petcare.vn", work); }
    private <T> T staff(Supplier<T> work) { return as("staff@petcare.vn", work); }

    @BeforeEach void setup() {
        db(() -> {
            orderRepository.deleteAll();
            cartRepository.deleteAll();
            products.deleteAll();
            return null;
        });
        customerId = users.findByEmail("customer@petcare.vn").orElseThrow().getId();
        otherId = users.findByEmail("other@petcare.vn").orElseGet(() -> users.saveAndFlush(
            new User("Khách khác", "other@petcare.vn", encoder.encode("TestOnly@123"), "", Role.CUSTOMER))).getId();
    }

    private Long product(ProductType type) {
        var form = ProductFactoryTest.form(type, "Sản phẩm", "100.25");
        form.setStockQuantity(5);
        form.setSpecies(Species.CAT);
        return products.saveAndFlush(new ProductFactory().create(form)).getId();
    }
    private Long place(Long productId, int quantity) {
        customer(() -> carts.addItem(customerId, productId, quantity));
        return customer(() -> orders.placeOrderFromCart(customerId, "123 Đường A")).getId();
    }
    private OrderStatus state(Long id) { return db(() -> orderRepository.findById(id).orElseThrow().getStateName()); }
    private int stock(Long id) { return db(() -> ((Accessory) products.findById(id).orElseThrow()).getStockQuantity()); }
    private PetStatus petStatus(Long id) { return db(() -> ((Pet) products.findById(id).orElseThrow()).getStatus()); }

    @ParameterizedTest @EnumSource(value = ProductType.class, names = {"ACCESSORY", "PET"})
    void staleCatalogFormCannotOverwriteConfirmedInventory(ProductType type) throws Exception {
        Long productId = product(type);
        var oldForm = productService.get(productId).toForm();
        Long orderId = place(productId, 1);
        staff(() -> orders.confirm(orderId));
        var confirmed = productService.get(productId).toForm();
        assertThat(confirmed.getVersion()).isGreaterThan(oldForm.getVersion());

        String html = mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("id", productId.toString()).param("type", type.name())
            .param("version", oldForm.getVersion().toString()).param("name", "Không được lưu")
            .param("basePrice", "1").param("stockQuantity", "5").param("status", "AVAILABLE"))
            .andExpect(status().isOk()).andExpect(view().name("products/form"))
            .andExpect(model().attributeHasErrors("form"))
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Dữ liệu đã được thay đổi, vui lòng tải lại trang");
        assertThat(productService.get(productId).toForm()).usingRecursiveComparison().isEqualTo(confirmed);
        assertThat(state(orderId)).isEqualTo(OrderStatus.CONFIRMED);
    }

    @ParameterizedTest @EnumSource(value = ProductType.class, names = {"ACCESSORY", "PET"})
    void freshCatalogFormEditsDetailsButCannotOverwriteInventory(ProductType type) throws Exception {
        Long productId = product(type);
        Long orderId = place(productId, 1);
        staff(() -> orders.confirm(orderId));
        var confirmed = productService.get(productId).toForm();
        mvc.perform(post("/staff/products/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("id", productId.toString()).param("type", type.name())
            .param("version", confirmed.getVersion().toString()).param("name", "Tên mới")
            .param("basePrice", "200.50").param("stockQuantity", "99").param("status", "AVAILABLE"))
            .andExpect(redirectedUrl("/staff/products"));
        var saved = productService.get(productId).toForm();
        assertThat(saved.getName()).isEqualTo("Tên mới");
        assertThat(saved.getBasePrice()).isEqualByComparingTo("200.50");
        assertThat(saved.getVersion()).isGreaterThan(confirmed.getVersion());
        assertThat(saved.getStockQuantity()).isEqualTo(confirmed.getStockQuantity());
        assertThat(saved.getStatus()).isEqualTo(confirmed.getStatus());
        staff(() -> orders.cancel(orderId));
        var restored = productService.get(productId).toForm();
        assertThat(restored.getName()).isEqualTo("Tên mới");
        assertThat(restored.getBasePrice()).isEqualByComparingTo("200.50");
        assertThat(restored.getVersion()).isGreaterThan(saved.getVersion());
        assertThatThrownBy(() -> productService.save(productId, saved))
            .isInstanceOf(ProductVersionConflictException.class);
        assertThat(productService.get(productId).toForm()).usingRecursiveComparison().isEqualTo(restored);
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"invalid", "-1"})
    void missingOrInvalidVersionCannotBypassEditProtection(String version) throws Exception {
        Long productId = product(ProductType.ACCESSORY);
        var original = productService.get(productId).toForm();
        var request = post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("id", productId.toString()).param("type", "ACCESSORY")
            .param("name", "Không được lưu").param("basePrice", "1");
        if (version != null) request.param("version", version);
        String html = mvc.perform(request).andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("form"))
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Dữ liệu đã được thay đổi, vui lòng tải lại trang");
        assertThat(productService.get(productId).toForm()).usingRecursiveComparison().isEqualTo(original);
    }

    @Test void checkoutSnapshotsPriceAndConfirmationDeductsInventory() {
        Long accessory = product(ProductType.ACCESSORY);
        Long pet = product(ProductType.PET);
        customer(() -> carts.addItem(customerId, accessory, 2));
        customer(() -> carts.addItem(customerId, pet, 1));
        Long id = customer(() -> orders.placeOrderFromCart(customerId, " Địa chỉ ")).getId();
        assertThat(stock(accessory)).isEqualTo(5);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.AVAILABLE);
        assertThat(db(() -> cartRepository.findByCustomerId(customerId).orElseThrow().getItems().size())).isZero();
        db(() -> {
            var changed = products.findById(accessory).orElseThrow();
            var form = changed.toForm();
            form.setBasePrice(new BigDecimal("999"));
            changed.update(form);
            return null;
        });
        staff(() -> orders.confirm(id));
        assertThat(stock(accessory)).isEqualTo(3);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.SOLD);
        db(() -> {
            var order = orderRepository.findById(id).orElseThrow();
            assertThat(order.getState()).isInstanceOf(ConfirmedState.class);
            assertThat(order.getCreatedAt()).isNotNull();
            assertThat(order.getShippingAddress()).isEqualTo("Địa chỉ");
            assertThat(order.getTotalAmount()).isEqualByComparingTo("300.75");
            assertThat(order.getFinalAmount()).isEqualByComparingTo("300.75");
            assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
            order.getItems().forEach(i -> assertThat(i.getUnitPrice()).isEqualByComparingTo("100.25"));
            assertThat(entityManager.createNativeQuery("select state_name from orders where id = :id")
                .setParameter("id", id).getSingleResult()).isEqualTo("CONFIRMED");
            return null;
        });
        assertThatThrownBy(() -> staff(() -> orders.confirm(id))).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(stock(accessory)).isEqualTo(3);
    }

    @Test void cancellationRestoresExactlyOnceAndPendingCancellationDoesNotRestore() {
        Long accessory = product(ProductType.ACCESSORY);
        Long pending = place(accessory, 2);
        customer(() -> orders.cancel(pending));
        assertThat(stock(accessory)).isEqualTo(5);
        Long pet = product(ProductType.PET);
        customer(() -> carts.addItem(customerId, accessory, 2));
        Long confirmed = place(pet, 1);
        staff(() -> orders.confirm(confirmed));
        db(() -> { products.findById(accessory).orElseThrow().hide(); return null; });
        customer(() -> orders.cancel(confirmed));
        assertThat(stock(accessory)).isEqualTo(5);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.AVAILABLE);
        assertThat(state(confirmed)).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> customer(() -> orders.cancel(confirmed))).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(stock(accessory)).isEqualTo(5);
    }

    @Test void shippingAndCompletionPersistAndCannotBeCancelled() {
        Long accessory = product(ProductType.ACCESSORY);
        Long id = place(accessory, 2);
        staff(() -> orders.confirm(id));
        staff(() -> orders.ship(id));
        assertThat(state(id)).isEqualTo(OrderStatus.SHIPPING);
        assertThatThrownBy(() -> customer(() -> orders.cancel(id))).isInstanceOf(InvalidStateTransitionException.class);
        staff(() -> orders.complete(id));
        assertThat(state(id)).isEqualTo(OrderStatus.COMPLETED);
        assertThatThrownBy(() -> staff(() -> orders.complete(id))).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(stock(accessory)).isEqualTo(3);
    }

    @Test void soldPetCannotBeBoughtAndFailedConfirmationDoesNotReleaseAnotherOrdersPet() {
        Long pet = product(ProductType.PET);
        Long first = place(pet, 1);
        Long second = place(pet, 1);
        staff(() -> orders.confirm(first));
        assertThatThrownBy(() -> customer(() -> carts.addItem(customerId, pet, 1))).isInstanceOf(PurchaseException.class);
        assertThatThrownBy(() -> staff(() -> orders.confirm(second))).isInstanceOf(PurchaseException.class);
        assertThat(state(second)).isEqualTo(OrderStatus.PENDING);
        customer(() -> orders.cancel(second));
        assertThat(petStatus(pet)).isEqualTo(PetStatus.SOLD);
    }

    @Test void failedCheckoutPreservesCartAndDoesNotInsertOrder() {
        Long pet = product(ProductType.PET);
        customer(() -> carts.addItem(customerId, pet, 1));
        db(() -> {
            var p = products.findById(pet).orElseThrow();
            var form = p.toForm();
            form.setStatus(PetStatus.SOLD);
            p.update(form);
            return null;
        });
        assertThatThrownBy(() -> customer(() -> orders.placeOrderFromCart(customerId, "Địa chỉ")))
            .isInstanceOf(PurchaseException.class);
        assertThat(orderRepository.count()).isZero();
        assertThat(db(() -> cartRepository.findByCustomerId(customerId).orElseThrow().getItems().size())).isEqualTo(1);
    }

    @Test void insufficientStockRollsBackWholeConfirmation() {
        Long pet = product(ProductType.PET);
        Long accessory = product(ProductType.ACCESSORY);
        customer(() -> carts.addItem(customerId, pet, 1));
        Long id = place(accessory, 5);
        db(() -> {
            var p = products.findById(accessory).orElseThrow();
            var form = p.toForm();
            form.setStockQuantity(0);
            p.update(form);
            return null;
        });
        assertThatThrownBy(() -> staff(() -> orders.confirm(id))).isInstanceOf(PurchaseException.class);
        assertThat(state(id)).isEqualTo(OrderStatus.PENDING);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.AVAILABLE);
        assertThat(stock(accessory)).isZero();
    }

    @Test void failedSecondRestorationRollsBackFirstRestorationAndOrderState() {
        Long firstProduct = product(ProductType.ACCESSORY);
        Long secondProduct = product(ProductType.ACCESSORY);
        customer(() -> carts.addItem(customerId, firstProduct, 2));
        Long orderId = place(secondProduct, 2);
        staff(() -> orders.confirm(orderId));
        // Chọn dòng gây lỗi theo thứ tự thực tế của đơn đã lưu, không giả định thứ tự ID.
        var itemIds = db(() -> orderRepository.findById(orderId).orElseThrow().getItems().stream()
            .map(i -> i.getProduct().getId()).toList());
        Long restoredId = itemIds.getFirst();
        Long failingId = itemIds.getLast();
        db(() -> {
            var product = products.findById(failingId).orElseThrow();
            var form = product.toForm();
            // Dữ liệu lỗi có chủ đích: hoàn thêm hai đơn vị sẽ tràn số nguyên.
            form.setStockQuantity(Integer.MAX_VALUE);
            product.update(form);
            return null;
        });
        var firstBefore = productService.get(restoredId).toForm();
        var secondBefore = productService.get(failingId).toForm();
        long orderVersion = db(() -> orderRepository.findById(orderId).orElseThrow().getVersion());

        assertThatThrownBy(() -> customer(() -> db(() -> {
            try {
                orders.cancel(orderId);
            } catch (PurchaseException ex) {
                // Buộc ghi xuống DB trước rollback, chứng minh dòng đầu đã được hoàn thật sự.
                entityManager.flush();
                entityManager.clear();
                assertThat(((Accessory) products.findById(restoredId).orElseThrow()).getStockQuantity()).isEqualTo(5);
                assertThat(orderRepository.findById(orderId).orElseThrow().getStateName()).isEqualTo(OrderStatus.CANCELLED);
                throw ex;
            }
            throw new AssertionError("Hủy đơn phải thất bại khi dòng sau không thể hoàn kho.");
        }))).isInstanceOf(PurchaseException.class).hasMessage("Số lượng hoàn kho không hợp lệ.");

        // Đọc lại bằng giao dịch mới để kiểm chứng rollback cả dữ liệu lẫn version.
        assertThat(productService.get(restoredId).toForm()).usingRecursiveComparison().isEqualTo(firstBefore);
        assertThat(productService.get(failingId).toForm()).usingRecursiveComparison().isEqualTo(secondBefore);
        assertThat(state(orderId)).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(db(() -> orderRepository.findById(orderId).orElseThrow().getVersion())).isEqualTo(orderVersion);
    }

    @Test void checksOwnerAndStaffAtServiceBoundary() {
        Long accessory = product(ProductType.ACCESSORY);
        Long id = place(accessory, 1);
        assertThatThrownBy(() -> as("other@petcare.vn", () -> carts.addItem(customerId, accessory, 1)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> customer(() -> carts.addItem(otherId, accessory, 1))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> as("other@petcare.vn", () -> orders.cancel(id))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> customer(() -> orders.confirm(id))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> orders.placeOrderFromCart(customerId, "Địa chỉ")).isInstanceOf(AccessDeniedException.class);
        assertThat(state(id)).isEqualTo(OrderStatus.PENDING);
        staff(() -> orders.cancel(id));
        assertThat(stock(accessory)).isEqualTo(5);
    }

    @Test void concurrentConfirmationsCannotSellOnePetTwice() throws Exception {
        Long pet = product(ProductType.PET);
        Long first = place(pet, 1);
        Long second = place(pet, 1);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = List.of(first, second).stream().map(id -> executor.submit(() -> {
                start.await();
                try { staff(() -> orders.confirm(id)); return true; }
                catch (PurchaseException ex) { return false; }
            })).toList();
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        }
        assertThat(List.of(state(first), state(second)))
            .containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.PENDING);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.SOLD);
    }

    @Test void composedTransactionDoesNotDiscardEarlierChangesWhenRefreshingLocks() {
        Long accessory = product(ProductType.ACCESSORY);
        Long id = place(accessory, 2);
        staff(() -> db(() -> {
            orders.confirm(id);
            orders.cancel(id);
            return null;
        }));
        assertThat(state(id)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock(accessory)).isEqualTo(5);
    }

    @Test void concurrentCheckoutCannotCreateTwoOrdersFromOneCart() throws Exception {
        Long accessory = product(ProductType.ACCESSORY);
        customer(() -> carts.addItem(customerId, accessory, 2));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> checkout = () -> {
                start.await();
                try { customer(() -> orders.placeOrderFromCart(customerId, "Địa chỉ")); return true; }
                catch (PurchaseException ex) { return false; }
            };
            var first = executor.submit(checkout);
            var second = executor.submit(checkout);
            start.countDown();
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(stock(accessory)).isEqualTo(5);
    }
}
