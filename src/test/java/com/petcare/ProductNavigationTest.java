package com.petcare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductNavigationTest {
    @Autowired
    private MockMvc mvc;

    private String header(String html) {
        var matcher = Pattern.compile("<header\\b[^>]*>(.*?)</header>", Pattern.DOTALL).matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/products"})
    void guestSeesCatalogWithoutManagementLink(String path) throws Exception {
        var html = mvc.perform(get(path)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(header(html)).containsPattern("<a\\b(?=[^>]*href=\"/products\")[^>]*>Danh mục</a>")
            .doesNotContain("href=\"/staff/products\"", "Quản lý sản phẩm");
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER, false", "STAFF, true", "ADMIN, true"})
    void headerShowsManagementOnlyToStaffAndAdmin(String role, boolean managementVisible) throws Exception {
        for (String path : new String[]{"/", "/products"}) {
            var html = mvc.perform(get(path).with(user("viewer").roles(role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String menu = header(html);

            assertThat(menu).containsPattern("<a\\b(?=[^>]*href=\"/products\")[^>]*>Danh mục</a>");
            assertThat(Pattern.compile("<a\\b(?=[^>]*href=\"/staff/products\")[^>]*>Quản lý sản phẩm</a>")
                .matcher(menu).find()).isEqualTo(managementVisible);
            if (!managementVisible) {
                assertThat(menu).doesNotContain("href=\"/staff/products\"");
            }
        }
    }

    @Test
    void homeLinksToCatalogAndExistingGroupFilters() throws Exception {
        var html = mvc.perform(get("/")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/products\">Xem danh mục</a>",
            "href=\"/products?group=PET\">Thú cưng</a>",
            "href=\"/products?group=ACCESSORY\">Phụ kiện</a>",
            "href=\"/products?group=SERVICE\">Dịch vụ</a>");
    }
}
