package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.order.OrderStatus;
import com.petcare.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiểm thử MVC qua transaction thật, tắt OSIV để view không dựa vào entity còn mở. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:commerce_web;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class CommerceWebTest {
    @Autowired com.petcare.repository.PointTransactionRepository pointTransactions;
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired CartRepository carts;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    private Long customerId;

    private <T> T db(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
    @BeforeEach void setup() {
        db(() -> { pointTransactions.deleteAll(); orders.deleteAll(); carts.deleteAll(); products.deleteAll(); return null; });
        customerId = users.findByEmail("customer@petcare.vn").orElseThrow().getId();
        if (users.findByEmail("other-web@petcare.vn").isEmpty()) {
            users.saveAndFlush(new User("Khách khác", "other-web@petcare.vn", encoder.encode("TestOnly@123"), "", Role.CUSTOMER));
        }
    }
    private Long product(ProductType type) {
        var form = ProductFactoryTest.form(type, "Sản phẩm thử " + type, "100.00");
        form.setStockQuantity(10);
        return products.saveAndFlush(new ProductFactory().create(form)).getId();
    }
    private ResultActions customerPost(String path, String... parameters) throws Exception {
        var request = post(path).with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf());
        for (int i = 0; i < parameters.length; i += 2) request.param(parameters[i], parameters[i + 1]);
        return mvc.perform(request);
    }
    private ResultActions staffPost(Long id, String action) throws Exception {
        return mvc.perform(post("/staff/orders/" + id + "/" + action)
            .with(user("staff@petcare.vn").roles("STAFF")).with(csrf()));
    }
    private String customerGet(String path) throws Exception {
        return mvc.perform(get(path).with(user("customer@petcare.vn").roles("CUSTOMER")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private String staffGet(String path) throws Exception {
        return mvc.perform(get(path).with(user("staff@petcare.vn").roles("STAFF")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private Long checkout(Long productId, int quantity) throws Exception {
        customerPost("/cart/items", "productId", productId.toString(), "quantity", Integer.toString(quantity))
            .andExpect(redirectedUrl("/cart"));
        var result = customerPost("/checkout", "shippingAddress", "123 Đường A")
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success")).andReturn();
        return Long.valueOf(result.getResponse().getRedirectedUrl().substring("/orders/".length()));
    }
    private OrderStatus state(Long id) { return orders.findById(id).orElseThrow().getStateName(); }
    private int stock(Long id) { return ((Accessory) products.findById(id).orElseThrow()).getStockQuantity(); }
    private PetStatus petStatus(Long id) { return ((Pet) products.findById(id).orElseThrow()).getStatus(); }

    @Test void customerBuysThenStaffConfirmsShipsAndCompletes() throws Exception {
        Long accessory = product(ProductType.ACCESSORY);
        Long pet = product(ProductType.PET);
        assertThat(customerGet("/products/" + accessory)).contains("action=\"/cart/items\"", "Thêm vào giỏ", "name=\"_csrf\"");
        customerPost("/cart/items", "productId", accessory.toString(), "quantity", "1").andExpect(redirectedUrl("/cart"));
        customerPost("/cart/items/" + accessory + "/quantity", "quantity", "2").andExpect(redirectedUrl("/cart"));
        customerPost("/cart/items", "productId", pet.toString(), "quantity", "1").andExpect(redirectedUrl("/cart"));
        assertThat(customerGet("/cart")).contains("300.00 VNĐ", "href=\"/checkout\"", "/quantity", "/remove");
        assertThat(customerGet("/checkout")).contains("name=\"shippingAddress\"", "300.00 VNĐ");
        customerPost("/checkout", "shippingAddress", " ").andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("checkoutForm", "shippingAddress"));
        assertThat(orders.count()).isZero();
        var result = customerPost("/checkout", "shippingAddress", " 123 Đường A ")
            .andExpect(status().is3xxRedirection()).andReturn();
        Long id = Long.valueOf(result.getResponse().getRedirectedUrl().substring("/orders/".length()));
        assertThat(customerGet("/cart")).contains("Giỏ hàng đang trống");
        assertThat(customerGet("/orders")).contains("href=\"/orders/" + id + "\"");
        assertThat(customerGet("/orders/" + id)).contains("300.00 VNĐ", "123 Đường A", "text-bg-warning", "/" + id + "/cancel")
            .doesNotContain("/" + id + "/confirm");
        assertThat(stock(accessory)).isEqualTo(10);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.AVAILABLE);
        assertThat(staffGet("/staff/orders?status=PENDING")).contains("href=\"/staff/orders/" + id + "\"");
        assertThat(staffGet("/staff/orders/" + id)).contains("/" + id + "/confirm", "/" + id + "/cancel")
            .doesNotContain("/" + id + "/ship", "/" + id + "/complete");
        staffPost(id, "confirm").andExpect(redirectedUrl("/staff/orders/" + id));
        assertThat(stock(accessory)).isEqualTo(8);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.SOLD);
        assertThat(customerGet("/products/" + pet)).doesNotContain("action=\"/cart/items\"");
        customerPost("/cart/items", "productId", pet.toString(), "quantity", "1")
            .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("error"));
        assertThat(staffGet("/staff/orders/" + id)).contains("text-bg-info", "/" + id + "/ship", "/" + id + "/cancel")
            .doesNotContain("/" + id + "/confirm", "/" + id + "/complete");
        staffPost(id, "ship").andExpect(redirectedUrl("/staff/orders/" + id));
        assertThat(staffGet("/staff/orders/" + id)).contains("text-bg-primary", "/" + id + "/complete")
            .doesNotContain("/" + id + "/cancel", "/" + id + "/confirm", "/" + id + "/ship");
        assertThat(customerGet("/orders/" + id)).doesNotContain("/" + id + "/cancel");
        staffPost(id, "complete").andExpect(redirectedUrl("/staff/orders/" + id));
        assertThat(state(id)).isEqualTo(OrderStatus.COMPLETED);
        assertThat(staffGet("/staff/orders/" + id)).contains("text-bg-success", "Hoàn thành")
            .doesNotContain("/" + id + "/cancel", "/" + id + "/complete", "/" + id + "/ship", "/" + id + "/confirm");
        assertThat(staffGet("/staff/orders?status=PENDING")).doesNotContain("href=\"/staff/orders/" + id + "\"");
        assertThat(staffGet("/staff/orders?status=COMPLETED")).contains("href=\"/staff/orders/" + id + "\"");
        var failure = customerPost("/orders/" + id + "/cancel").andExpect(redirectedUrl("/orders"))
            .andExpect(flash().attributeExists("error")).andReturn();
        String html = mvc.perform(get("/orders").with(user("customer@petcare.vn").roles("CUSTOMER"))
                .flashAttrs(failure.getFlashMap())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("alert-danger", "Không thể hủy", "Hoàn thành");
        assertThat(state(id)).isEqualTo(OrderStatus.COMPLETED);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void customerCancelsPendingOrConfirmedOrder(boolean confirmed) throws Exception {
        Long accessory = product(ProductType.ACCESSORY);
        Long pet = product(ProductType.PET);
        customerPost("/cart/items", "productId", pet.toString(), "quantity", "1");
        Long id = checkout(accessory, 2);
        if (confirmed) staffPost(id, "confirm").andExpect(status().is3xxRedirection());
        assertThat(customerGet("/orders/" + id)).contains("/" + id + "/cancel");
        customerPost("/orders/" + id + "/cancel").andExpect(redirectedUrl("/orders/" + id));
        assertThat(state(id)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock(accessory)).isEqualTo(10);
        assertThat(petStatus(pet)).isEqualTo(PetStatus.AVAILABLE);
        assertThat(customerGet("/orders/" + id)).contains("text-bg-secondary", "Đã hủy").doesNotContain("/" + id + "/cancel");
        customerPost("/orders/" + id + "/cancel").andExpect(flash().attributeExists("error"));
        assertThat(stock(accessory)).isEqualTo(10);
    }

    @Test void otherCustomerCannotReadOrCancelOrderAndCannotChooseCartOwner() throws Exception {
        Long accessory = product(ProductType.ACCESSORY);
        Long id = checkout(accessory, 1);
        mvc.perform(get("/orders/" + id).with(user("other-web@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/orders/" + id + "/cancel").with(user("other-web@petcare.vn").roles("CUSTOMER")).with(csrf()))
            .andExpect(status().isForbidden());
        String html = mvc.perform(get("/orders").with(user("other-web@petcare.vn").roles("CUSTOMER")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("href=\"/orders/" + id + "\"");
        mvc.perform(post("/cart/items").with(user("other-web@petcare.vn").roles("CUSTOMER")).with(csrf())
            .param("productId", accessory.toString()).param("quantity", "2").param("customerId", customerId.toString()))
            .andExpect(redirectedUrl("/cart"));
        assertThat(db(() -> carts.findByCustomerId(customerId).orElseThrow().getItems().size())).isZero();
        assertThat(state(id)).isEqualTo(OrderStatus.PENDING);
    }

    @Test void businessFailuresBecomeFlashAndDoNotMutateCartOrOrder() throws Exception {
        Long accessory = product(ProductType.ACCESSORY);
        Long vet = product(ProductType.VET);
        customerPost("/cart/items", "productId", accessory.toString(), "quantity", "11")
            .andExpect(redirectedUrl("/cart")).andExpect(flash().attribute("error", "Phụ kiện không đủ tồn kho."));
        customerPost("/cart/items", "productId", vet.toString(), "quantity", "1")
            .andExpect(flash().attributeExists("error"));
        assertThat(customerGet("/products/" + vet)).contains("cần đặt lịch hẹn").doesNotContain("action=\"/cart/items\"");
        Long id = checkout(accessory, 2);
        staffPost(id, "complete").andExpect(redirectedUrl("/staff/orders")).andExpect(flash().attributeExists("error"));
        assertThat(state(id)).isEqualTo(OrderStatus.PENDING);
        customerPost("/cart/items", "productId", accessory.toString(), "quantity", "1");
        db(() -> { products.findById(accessory).orElseThrow().hide(); return null; });
        customerPost("/checkout", "shippingAddress", "Địa chỉ hợp lệ")
            .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("error"));
        assertThat(orders.count()).isEqualTo(1);
        assertThat(customerGet("/cart")).contains("Sản phẩm đã ẩn");
        customerPost("/cart/items/" + accessory + "/remove").andExpect(redirectedUrl("/cart"));
        assertThat(customerGet("/cart")).contains("Giỏ hàng đang trống");
        customerPost("/checkout", "shippingAddress", "Địa chỉ hợp lệ").andExpect(flash().attributeExists("error"));
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "abc", "2147483648", ""})
    void rejectsInvalidQuantityWithoutSaving(String quantity) throws Exception {
        Long id = product(ProductType.ACCESSORY);
        customerPost("/cart/items", "productId", id.toString(), "quantity", quantity)
            .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("error"));
        assertThat(carts.count()).isZero();
        assertThat(stock(id)).isEqualTo(10);
    }

    @Test void missingQuantityKeepsCartUnchangedAndValidUpdateStillSucceeds() throws Exception {
        Long productId = product(ProductType.ACCESSORY);
        assertThat(customerGet("/products/" + productId)).contains("name=\"quantity\" value=\"1\"");
        customerPost("/cart/items", "productId", productId.toString(), "quantity", "3")
            .andExpect(redirectedUrl("/cart"));

        var failure = customerPost("/cart/items/" + productId + "/quantity")
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra số lượng, mã sản phẩm hoặc trạng thái đã chọn."))
            .andExpect(flash().attributeCount(1)).andReturn();
        assertThat(db(() -> carts.findByCustomerId(customerId).orElseThrow().getItems().getFirst().getQuantity()))
            .isEqualTo(3);
        String html = mvc.perform(get("/cart").with(user("customer@petcare.vn").roles("CUSTOMER"))
                .flashAttrs(failure.getFlashMap())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("alert-danger", "Dữ liệu không hợp lệ", "300.00 VNĐ");

        customerPost("/cart/items/" + productId + "/quantity", "quantity", "4")
            .andExpect(redirectedUrl("/cart")).andExpect(flash().attribute("success", "Đã cập nhật số lượng."));
        assertThat(db(() -> carts.findByCustomerId(customerId).orElseThrow().getItems().getFirst().getQuantity()))
            .isEqualTo(4);
        assertThat(stock(productId)).isEqualTo(10);
    }

    @Test void enforcesRolesCsrfAndInvalidFilter() throws Exception {
        for (String path : new String[]{"/cart", "/checkout", "/orders", "/orders/1", "/staff/orders", "/staff/orders/1"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
        }
        for (String role : new String[]{"STAFF", "ADMIN"}) {
            for (String path : new String[]{"/cart", "/checkout", "/orders", "/orders/1"}) {
                mvc.perform(get(path).with(user("viewer").roles(role))).andExpect(status().isForbidden());
            }
        }
        Long id = checkout(product(ProductType.ACCESSORY), 1);
        mvc.perform(get("/staff/orders").with(user("customer@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        customerPost("/staff/orders/" + id + "/confirm").andExpect(status().isForbidden());
        mvc.perform(post("/orders/" + id + "/cancel").with(user("customer@petcare.vn").roles("CUSTOMER")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/staff/orders/" + id + "/confirm").with(user("staff@petcare.vn").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/staff/orders?status=INVALID").with(user("staff@petcare.vn").roles("STAFF")))
            .andExpect(redirectedUrl("/staff/orders")).andExpect(flash().attributeExists("error"));
        mvc.perform(get("/staff/orders/" + id).with(user("admin@petcare.vn").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/staff/orders/" + id + "/confirm").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/staff/orders/" + id));
        assertThat(state(id)).isEqualTo(OrderStatus.CONFIRMED);
    }

    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void headerAndHomeExposeOnlyAuthorizedNavigation(String role) throws Exception {
        var request = get("/");
        if (!role.equals("GUEST")) request.with(user("viewer").roles(role));
        String html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String header = html.substring(html.indexOf("<header"), html.indexOf("</header>"));
        String main = html.substring(html.indexOf("<main"), html.indexOf("</main>"));
        for (String section : new String[]{header, main}) {
            for (String path : new String[]{"/cart", "/checkout", "/orders"}) {
                assertThat(section.contains("href=\"" + path + "\"")).isEqualTo(role.equals("CUSTOMER"));
            }
            assertThat(section.contains("href=\"/staff/orders\""))
                .isEqualTo(role.equals("STAFF") || role.equals("ADMIN"));
        }
        Long productId = product(ProductType.ACCESSORY);
        var detailRequest = get("/products/" + productId);
        if (!role.equals("GUEST")) detailRequest.with(user("viewer").roles(role));
        String detail = mvc.perform(detailRequest).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail.contains("action=\"/cart/items\"")).isEqualTo(role.equals("CUSTOMER"));
    }
}
