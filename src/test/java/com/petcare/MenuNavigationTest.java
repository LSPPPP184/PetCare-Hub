package com.petcare;

import com.petcare.repository.ProductRepository;
import com.petcare.domain.ProductType;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.dto.ProductForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiểm tra menu render thật: đánh dấu đúng khu vực và giữ nguyên quyền truy cập. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:menu_navigation;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuNavigationTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

    // HTML5 có thẻ void, không dùng bộ phân tích XML/XPath cho phản hồi Thymeleaf.
    private List<Map<String, String>> menuLinks(String html) {
        var header = Pattern.compile("<header\\b[^>]*>(.*?)</header>", Pattern.DOTALL).matcher(html);
        assertThat(header.find()).isTrue();
        var links = new ArrayList<Map<String, String>>();
        var anchors = Pattern.compile("<a\\b([^>]*)>").matcher(header.group(1));
        while (anchors.find()) {
            var attributes = new HashMap<String, String>();
            var pairs = Pattern.compile("([\\w:-]+)=\"([^\"]*)\"").matcher(anchors.group(1));
            while (pairs.find()) attributes.put(pairs.group(1), pairs.group(2));
            links.add(attributes);
        }
        assertThat(links).isNotEmpty();
        return links;
    }

    private void current(MockHttpServletRequestBuilder request, String... hrefs) throws Exception {
        var html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var links = menuLinks(html);
        assertThat(links.stream().filter(a -> a.containsKey("aria-current")).map(a -> a.get("href")))
            .containsExactlyInAnyOrder(hrefs);
        assertThat(links.stream().filter(a -> Arrays.asList(a.getOrDefault("class", "").split("\\s+")).contains("pc-nav-current"))
            .map(a -> a.get("href"))).containsExactlyInAnyOrder(hrefs);
        links.stream().filter(a -> a.containsKey("aria-current"))
            .forEach(a -> assertThat(a.get("aria-current")).isEqualTo("page"));
    }

    @ParameterizedTest
    @CsvSource({"/, /", "/products, /products", "/login, /login", "/register, /register",
        "/quiz, /quiz", "/quiz/results, /quiz/results", "/appointments/services, /appointments/services"})
    void guestCurrentPageAndOtherLinksRemainInactive(String path, String href) throws Exception {
        current(get(path), href);
    }

    @ParameterizedTest
    @CsvSource({"/cart, /cart", "/checkout, /checkout", "/orders, /orders",
        "/appointments, /appointments", "/my-pets, /my-pets", "/my-pets/new, /my-pets/new",
        "/membership, /membership", "/subscriptions, /subscriptions", "/quiz/history, /quiz/history",
        "/appointments/services, /appointments/services"})
    void customerCurrentPageAndOtherLinksRemainInactive(String path, String href) throws Exception {
        current(get(path).with(user("customer@petcare.vn").roles("CUSTOMER")), href);
    }

    @ParameterizedTest
    @CsvSource({"STAFF, /staff", "ADMIN, /admin"})
    void roleDashboardIsCurrent(String role, String path) throws Exception {
        current(get(path).with(user(role.toLowerCase() + "@petcare.vn").roles(role)), path);
    }

    @ParameterizedTest
    @CsvSource({"/staff/products, /staff/products", "/staff/products/new, /staff/products",
        "/staff/orders, /staff/orders", "/staff/appointments, /staff/appointments",
        "/staff/health, /staff/health", "/staff/health/pets, /staff/health/pets"})
    void staffChildPageMarksItsSectionAndDashboardParent(String path, String href) throws Exception {
        current(get(path).with(user("staff@petcare.vn").roles("STAFF")), "/staff", href);
    }

    @ParameterizedTest
    @CsvSource({"/admin/members, /admin/members", "/admin/subscriptions, /admin/subscriptions"})
    void adminChildPageMarksItsSectionAndDashboardParent(String path, String href) throws Exception {
        current(get(path).with(user("admin@petcare.vn").roles("ADMIN")), "/admin", href);
    }

    @ParameterizedTest
    @CsvSource({"DUE_SOON", "OVERDUE"})
    void healthFiltersDoNotMarkEachOther(String status) throws Exception {
        current(get("/staff/health").param("status", status).with(user("staff@petcare.vn").roles("STAFF")),
            "/staff", "/staff/health?status=" + status);
    }

    @Test @Transactional void productDetailMarksCatalogParent() throws Exception {
        var form = new ProductForm();
        form.setType(ProductType.ACCESSORY);
        form.setName("Phụ kiện kiểm tra điều hướng");
        form.setStockQuantity(1);
        var id = products.save(new ProductFactory().create(form)).getId();
        current(get("/products/" + id), "/products");
    }

    @Test void applicationContextPathDoesNotBreakSelection() throws Exception {
        current(get("/petcare/products").contextPath("/petcare"), "/petcare/products");
    }

    @Test void errorPageDoesNotMarkAnUnrelatedMenuLink() throws Exception {
        var html = mvc.perform(get("/products-extra")).andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();
        assertThat(menuLinks(html)).noneMatch(a -> a.containsKey("aria-current"));
    }

    @Test void staffCannotSeeOrOpenAdminNavigation() throws Exception {
        var html = mvc.perform(get("/staff").with(user("staff@petcare.vn").roles("STAFF")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(menuLinks(html)).noneMatch(a -> a.getOrDefault("href", "").startsWith("/admin"));
        mvc.perform(get("/admin").with(user("staff@petcare.vn").roles("STAFF")))
            .andExpect(status().isForbidden());
    }
}
