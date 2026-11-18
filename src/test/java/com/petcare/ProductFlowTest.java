package com.petcare;

import com.petcare.domain.*;
import com.petcare.repository.ProductRepository;
import com.petcare.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import jakarta.persistence.EntityManager;
import org.springframework.util.LinkedMultiValueMap;
import java.util.Map;
import java.util.function.Consumer;
import com.petcare.dto.ProductForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProductFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ProductService service;
    @Autowired ProductRepository repository;
    @Autowired EntityManager entityManager;

    private static final Map<ProductType, Consumer<ProductForm>> SUBTYPE_CHECKS = Map.of(
        ProductType.PET, f -> {
            assertThat(f.getSpecies()).isEqualTo(Species.CAT);
            assertThat(f.getBreed()).isEqualTo("Anh lông ngắn");
            assertThat(f.getAgeMonths()).isEqualTo(12);
            assertThat(f.getGender()).isEqualTo("Cái");
            assertThat(f.getSize()).isEqualTo("Nhỏ");
            assertThat(f.getHealthNote()).isEqualTo("Khỏe mạnh");
            assertThat(f.getStatus()).isEqualTo(PetStatus.AVAILABLE);
        },
        ProductType.ACCESSORY, f -> {
            assertThat(f.getBrand()).isEqualTo("PetCare");
            assertThat(f.getStockQuantity()).isEqualTo(5);
            assertThat(f.getCategory()).isEqualTo("toy");
        },
        ProductType.GROOMING, f -> {
            assertThat(f.getDurationMinutes()).isEqualTo(40);
            assertThat(f.getPetSizeSurcharge()).isEqualByComparingTo("25.50");
        },
        ProductType.VET, f -> {
            assertThat(f.getDurationMinutes()).isEqualTo(40);
            assertThat(f.getDoctorName()).isEqualTo("Bác sĩ Bình");
        },
        ProductType.MONTHLY, f -> {
            assertThat(f.getDurationMinutes()).isEqualTo(40);
            assertThat(f.getDurationMonths()).isEqualTo(2);
            assertThat(f.getIncludedUses()).isEqualTo(8);
            assertThat(f.getIncludedServices()).isEqualTo("Khám");
        });

    @ParameterizedTest
    @ValueSource(strings = {"", "INVALID"})
    void invalidProductTypeReturnsFormWithoutSaving(String type) throws Exception {
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("type", type).param("name", "Sản phẩm").param("basePrice", "100"))
            .andExpect(status().isOk()).andExpect(view().name("products/form"))
            .andExpect(model().attributeHasFieldErrors("form", "type"));
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest @EnumSource(ProductType.class)
    void staffCreatesEditsAndHidesEachSubtype(ProductType type) throws Exception {
        var covered = repository.saveAndFlush(new com.petcare.domain.factory.ProductFactory().create(
            ProductFactoryTest.form(ProductType.GROOMING, "Dịch vụ áp dụng", "50000")));
        mvc.perform(get("/staff/products/new").param("type", type.name()).with(user("staff").roles("STAFF")))
            .andExpect(status().isOk());
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("type", type.name()).param("name", "Sản phẩm mới").param("basePrice", "100")
            .param("stockQuantity", "5")
            .param("coveredServiceIds", covered.getId().toString())
            .param("durationMinutes", "30").param("doctorName", "Bác sĩ An")
            .param("durationMonths", "1").param("includedUses", "4").param("includedServices", "Tắm"))
            .andExpect(redirectedUrl("/staff/products"));
        var p = repository.findAll().stream().filter(item -> !item.getId().equals(covered.getId())).findFirst().orElseThrow();
        Long id = p.getId();
        repository.flush();
        entityManager.clear();
        p = repository.findById(id).orElseThrow();
        assertThat(p.getType()).isEqualTo(type);
        mvc.perform(get("/products/" + id)).andExpect(status().isOk());
        String editHtml = mvc.perform(get("/staff/products/" + id + "/edit").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(editHtml)
            .containsPattern("<input(?=[^>]*name=\"version\")(?=[^>]*value=\"" + p.getVersion() + "\")[^>]*>")
            .doesNotContain("name=\"stockQuantity\"", "name=\"status\"");
        mvc.perform(post("/staff/products/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("id", id.toString()).param("type", type.name()).param("name", "Đã sửa").param("basePrice", "200")
            .param("version", Long.toString(p.getVersion()))
            .param("coveredServiceIds", covered.getId().toString())
            .param("durationMinutes", "40").param("doctorName", "Bác sĩ Bình")
            .param("durationMonths", "2").param("includedUses", "8").param("includedServices", "Khám")
            .param("species", "CAT").param("breed", "Anh lông ngắn").param("ageMonths", "12")
            .param("gender", "Cái").param("size", "Nhỏ").param("healthNote", "Khỏe mạnh")
            .param("status", "RESERVED").param("brand", "PetCare").param("stockQuantity", "15")
            .param("category", "toy").param("petSizeSurcharge", "25.50"))
            .andExpect(redirectedUrl("/staff/products"));
        repository.flush();
        entityManager.clear();
        assertThat(service.get(id).getName()).isEqualTo("Đã sửa");
        assertThat(service.get(id).getBasePrice()).isEqualByComparingTo("200");
        SUBTYPE_CHECKS.get(type).accept(service.get(id).toForm());
        mvc.perform(post("/staff/products/" + id + "/hide").with(user("staff").roles("STAFF")).with(csrf()))
            .andExpect(redirectedUrl("/staff/products"));
        mvc.perform(get("/products/" + id)).andExpect(status().isNotFound());
        assertThat(service.search("", "Đã sửa", 0, false, false)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"PET,name,'',name", "PET,basePrice,-1,basePrice",
        "PET,basePrice,0.001,basePrice", "PET,ageMonths,-1,ageMonths",
        "ACCESSORY,stockQuantity,-1,stockQuantity", "GROOMING,petSizeSurcharge,-1,petSizeSurcharge",
        "GROOMING,durationMinutes,0,typeValid", "VET,doctorName,'',typeValid",
        "MONTHLY,durationMonths,0,typeValid", "MONTHLY,includedUses,0,typeValid",
        "MONTHLY,includedServices,'',typeValid"})
    void rejectsEachInvalidFieldIndependently(String type, String field, String value, String errorField) throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.set("type", type);
        params.set("name", "Hợp lệ");
        params.set("basePrice", "100");
        params.set("durationMinutes", "30");
        params.set("doctorName", "Bác sĩ An");
        params.set("durationMonths", "1");
        params.set("includedUses", "4");
        params.set("includedServices", "Tắm");
        params.set("coveredServiceIds", "1");
        params.set(field, value);
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf()).params(params))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", errorField))
            .andExpect(model().errorCount(1));
        assertThat(repository.count()).isZero();
    }

    @Test void customerCannotEditOrHideEvenWithValidCsrf() throws Exception {
        service.save(null, ProductFactoryTest.form(ProductType.PET, "Ban đầu", "100"));
        repository.flush();
        entityManager.clear();
        var original = repository.findAll().getFirst();
        Long id = original.getId();
        entityManager.clear();
        mvc.perform(post("/staff/products/save").with(user("customer").roles("CUSTOMER")).with(csrf())
            .param("id", id.toString()).param("type", "PET").param("name", "Bị sửa").param("basePrice", "1"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/staff/products/" + id + "/hide").with(user("customer").roles("CUSTOMER")).with(csrf()))
            .andExpect(status().isForbidden());
        repository.flush();
        entityManager.clear();
        assertThat(repository.findById(id).orElseThrow()).usingRecursiveComparison().isEqualTo(original);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test void publicCatalogAndProtectedManagement() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/staff/products")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/staff/products").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("type", "VET").param("name", "Khám").param("basePrice", "-1"))
            .andExpect(model().attributeHasErrors("form"));
        assertThat(repository.count()).isZero();
    }

    @Test void repositoryFiltersVisibilityAndLiteralNameAcrossSubtypes() {
        service.save(null, ProductFactoryTest.form(ProductType.PET, "Mèo con", "100"));
        service.save(null, ProductFactoryTest.form(ProductType.ACCESSORY, "Đồ chơi mèo 100%_", "50"));
        service.save(null, ProductFactoryTest.form(ProductType.VET, "Khám mèo", "200"));
        repository.flush();
        var hidden = repository.findAll().stream().filter(p -> p.getType() == ProductType.PET).findFirst().orElseThrow();
        service.hide(hidden.getId());
        repository.flush();
        entityManager.clear();
        assertThat(repository.findForCatalog("mèo", false)).extracting(Product::getName)
            .containsExactlyInAnyOrder("Đồ chơi mèo 100%_", "Khám mèo");
        assertThat(repository.findForCatalog("mèo", true)).hasSize(3);
        assertThat(repository.findForCatalog("%_", false)).extracting(Product::getName)
            .containsExactly("Đồ chơi mèo 100%_");
        assertThat(repository.findForCatalog("không tồn tại", false)).isEmpty();
        assertThat(repository.findForCatalog("", false)).hasSize(2);
        assertThat(service.search("SERVICE", " MÈO ", 0, false, false).getContent())
            .extracting(v -> v.name()).containsExactly("Khám mèo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/pet.jpg", "", "javascript:alert(1)"})
    void rendersSafeImageOrPlaceholderInCatalogAndDetail(String imageUrl) throws Exception {
        var form = ProductFactoryTest.form(ProductType.PET, "Mèo", "100");
        // Trường hợp URL cũ không hợp lệ cũng không được đưa vào thuộc tính src.
        form.setImageUrl(imageUrl);
        service.save(null, form);
        repository.flush();
        entityManager.clear();
        Long id = repository.findAll().getFirst().getId();
        for (String path : new String[]{"/products", "/products/" + id}) {
            String html = mvc.perform(get(path)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            if (imageUrl.startsWith("https://")) {
                assertThat(html).contains("src=\"https://example.com/pet.jpg\"", "alt=\"Ảnh Mèo\"");
            } else {
                assertThat(html).contains("Chưa có ảnh sản phẩm.").doesNotContain("<img", "javascript:");
            }
        }
    }

    @Test void rejectsUnsafeImageUrlAtRegistrationBoundary() throws Exception {
        mvc.perform(post("/staff/products/save").with(user("staff").roles("STAFF")).with(csrf())
            .param("type", "PET").param("name", "Mèo").param("basePrice", "100")
            .param("imageUrl", "javascript:alert(1)"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "imageUrlValid"));
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"basePrice,abc,Giá niêm yết phải là số hợp lệ.",
        "petSizeSurcharge,abc,Phụ phí kích thước phải là số hợp lệ.",
        "ageMonths,abc,Tuổi theo tháng phải là số nguyên hợp lệ.",
        "stockQuantity,abc,Tồn kho phải là số nguyên hợp lệ.",
        "durationMinutes,abc,Thời lượng phải là số nguyên phút hợp lệ.",
        "durationMonths,abc,Số tháng phải là số nguyên hợp lệ.",
        "includedUses,abc,Số lượt phải là số nguyên hợp lệ.",
        "type,INVALID,Loại sản phẩm không hợp lệ.",
        "species,INVALID,Loài thú cưng không hợp lệ.",
        "status,INVALID,Trạng thái thú cưng không hợp lệ."})
    void displaysVietnameseBindingErrors(String field, String value, String message) throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.set("type", "PET");
        params.set("name", "Mèo");
        params.set("basePrice", "100");
        params.set(field, value);
        String html = mvc.perform(post("/staff/products/save")
            .with(user("staff").roles("STAFF")).with(csrf()).params(params))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", field))
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains(message).doesNotContain("Failed to convert", "java.lang.");
        assertThat(repository.count()).isZero();
    }
}
