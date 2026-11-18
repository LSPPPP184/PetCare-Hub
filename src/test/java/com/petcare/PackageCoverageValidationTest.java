package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.appointment.AppointmentException;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.repository.ProductRepository;
import com.petcare.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PackageCoverageValidationTest {
    @Autowired ProductService service;
    @Autowired ProductRepository products;
    @Autowired MockMvc mvc;

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void missingOrEmptySelectionReturnsVietnameseFormError(boolean empty) throws Exception {
        var request = post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("type", "MONTHLY").param("name", "Gói chưa cấu hình").param("basePrice", "100000")
            .param("durationMinutes", "30").param("durationMonths", "1").param("includedUses", "4").param("includedServices", "Tắm tỉa");
        if (empty) request.param("_coveredServiceIds", "on");
        mvc.perform(request).andExpect(status().isOk()).andExpect(view().name("products/form"))
            .andExpect(model().attributeHasFieldErrors("form", "coveredServicesSelected"))
            .andExpect(content().string(containsString("Gói tháng phải có ít nhất một dịch vụ áp dụng hợp lệ.")));
        assertThat(products.count()).isZero();
    }
    @Test void serviceAlsoRejectsMissingServicesWithoutBeanValidation() {
        var form = ProductFactoryTest.form(ProductType.MONTHLY, "Gói thiếu dịch vụ", "100000");
        assertThatThrownBy(() -> service.save(null, form)).isInstanceOf(AppointmentException.class);
        assertThat(products.count()).isZero();
    }
    @ParameterizedTest @ValueSource(strings = {"missing", "hidden", "accessory", "package"})
    void nonexistentHiddenOrNonBookableServiceReturnsFormError(String kind) throws Exception {
        var type = kind.equals("accessory") ? ProductType.ACCESSORY : kind.equals("package") ? ProductType.MONTHLY : ProductType.GROOMING;
        var selected = new ProductFactory().create(ProductFactoryTest.form(type, "Dịch vụ kiểm thử", "50000"));
        if (kind.equals("hidden")) selected.hide();
        var id = products.saveAndFlush(selected).getId();
        var selectedId = kind.equals("missing") ? Long.MAX_VALUE : id;
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("type", "MONTHLY").param("name", "Không được lưu").param("basePrice", "100000")
            .param("durationMinutes", "30").param("durationMonths", "1").param("includedUses", "4")
            .param("includedServices", "Tắm tỉa").param("coveredServiceIds", Long.toString(selectedId)))
            .andExpect(status().isOk()).andExpect(view().name("products/form")).andExpect(model().hasErrors());
        assertThat(products.count()).isOne();
    }
    @Test void configuredPackageCanBeSavedAndCannotHaveAllServicesRemoved() {
        var selected = products.saveAndFlush(new ProductFactory().create(ProductFactoryTest.form(ProductType.VET, "Khám", "50000")));
        var form = ProductFactoryTest.form(ProductType.MONTHLY, "Gói hợp lệ", "100000");
        form.setCoveredServiceIds(List.of(selected.getId())); service.save(null, form); products.flush();
        var pack = products.findAll().stream().filter(p -> p.getType() == ProductType.MONTHLY).findFirst().orElseThrow();
        var edit = service.form(pack.getId()); edit.setCoveredServiceIds(List.of());
        assertThatThrownBy(() -> service.save(pack.getId(), edit)).isInstanceOf(AppointmentException.class);
    }
}
