package com.petcare;

import com.petcare.domain.*;
import com.petcare.dto.*;
import com.petcare.repository.*;
import com.petcare.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:matching_web;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class MatchingWebTest {
    @Autowired MockMvc mvc;
    @Autowired MatchingPetRepository pets;
    @Autowired QuizResultRepository history;
    @Autowired ProductService products;
    @Autowired MatchingService matching;
    @Autowired PlatformTransactionManager transactions;
    private <T> T db(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }

    @BeforeEach void setup() {
        history.deleteAll(); pets.deleteAll();
        for (int i = 1; i <= 4; i++) {
            var pet = MatchingStrategyTest.pet(i, 1, i, true, 1, 1, 1, 1, 1);
            ReflectionTestUtils.setField(pet, "id", null);
            pets.saveAndFlush(pet);
        }
    }
    private MockHttpServletRequestBuilder quiz() {
        return post("/quiz").with(csrf()).param("homeSize", "LARGE").param("hasChildren", "true")
            .param("activityLevel", "3").param("freeHoursPerDay", "2").param("budgetLevel", "3")
            .param("furAllergy", "false").param("experienceLevel", "3");
    }

    @Test void guestCompletesQuizGetsTopThreeLinksAndFormulaWithoutDatabaseHistory() throws Exception {
        mvc.perform(get("/quiz")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Bước 3")));
        var response = mvc.perform(quiz()).andExpect(redirectedUrl("/quiz/results")).andReturn();
        var session = (MockHttpSession) response.getRequest().getSession();
        var report = (QuizReport) session.getAttribute("latestQuiz");
        assertThat(report.matches()).hasSize(3);
        assertThat(report.matches().getFirst().scorePercent()).isEqualTo(100);
        assertThat(history.count()).isZero();
        var html = mvc.perform(get("/quiz/results").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Vì sao được điểm này?", "Điểm = 100", "Ngân sách", "độ khớp", "100.00%");
        for (var match : report.matches()) assertThat(html).contains("href=\"/products/" + match.petId() + "\"");
        mvc.perform(get("/quiz/results").session(session)).andExpect(status().isOk());
        assertThat(history.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"CUSTOMER", "STAFF", "ADMIN"})
    void authenticatedRolesSaveHistoryAndViewTheirOwnSnapshot(String role) throws Exception {
        String email = role.toLowerCase() + "@petcare.vn";
        mvc.perform(quiz().with(user(email).roles(role)).param("strategy", "budget-first"))
            .andExpect(redirectedUrl("/quiz/results"));
        var reports = matching.history(email);
        assertThat(reports).hasSize(1);
        var report = reports.getFirst();
        assertThat(report.strategyLabel()).isEqualTo("Ưu tiên ngân sách");
        var petId = report.matches().getFirst().petId();
        var edit = products.get(petId).toForm(); edit.setName("Tên đã sửa sau quiz"); edit.getProfile().setEnergyLevel(5);
        products.save(petId, edit);
        assertThat(matching.detail(report.id(), email).matches()).isEqualTo(report.matches());
        var html = mvc.perform(get("/quiz/history").with(user(email).roles(role))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("/quiz/history/" + report.id());
        mvc.perform(get("/quiz/history/" + report.id()).with(user(email).roles(role))).andExpect(status().isOk());
        mvc.perform(get("/quiz/history/" + report.id()).with(user("another@petcare.vn").roles(role))).andExpect(status().isNotFound());
    }

    @Test void invalidInputUnknownStrategyAndCsrfDoNotSaveResults() throws Exception {
        mvc.perform(post("/quiz").with(csrf())).andExpect(status().isOk()).andExpect(model().attributeHasErrors("answers"));
        for (var pair : new String[][]{{"activityLevel", "6"}, {"freeHoursPerDay", "-1"}, {"budgetLevel", "0"},
            {"experienceLevel", "6"}, {"homeSize", "UNKNOWN"}, {"freeHoursPerDay", "NaN"}}) {
            var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
            params.set("homeSize", "LARGE"); params.set("hasChildren", "true"); params.set("activityLevel", "3");
            params.set("freeHoursPerDay", "2"); params.set("budgetLevel", "3"); params.set("furAllergy", "false"); params.set("experienceLevel", "3");
            params.set(pair[0], pair[1]);
            mvc.perform(post("/quiz").with(csrf()).params(params)).andExpect(status().isOk()).andExpect(model().attributeHasErrors("answers"));
        }
        mvc.perform(quiz().param("strategy", "unknown")).andExpect(status().isOk()).andExpect(model().attributeHasErrors("answers"));
        mvc.perform(post("/quiz")).andExpect(status().isForbidden());
        mvc.perform(get("/quiz/history")).andExpect(status().is3xxRedirection());
        assertThat(history.count()).isZero();
    }

    @Test void emptyResultsAndMissingSessionAreNavigable() throws Exception {
        mvc.perform(get("/quiz/results")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn chưa có kết quả")));
        db(() -> { pets.findAll().forEach(Pet::hide); return null; });
        var response = mvc.perform(quiz()).andExpect(redirectedUrl("/quiz/results")).andReturn();
        var session = (MockHttpSession) response.getRequest().getSession();
        mvc.perform(get("/quiz/results").session(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có thú cưng đáp ứng")));
    }

    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void navigationInBothHeaderAndHomeRespectsRoles(String role) throws Exception {
        var request = get("/");
        if (!role.equals("GUEST")) request.with(user(role.toLowerCase() + "@petcare.vn").roles(role));
        String html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String tag : new String[]{"header", "main"}) {
            var matcher = Pattern.compile("<" + tag + "\\b[^>]*>(.*?)</" + tag + ">", Pattern.DOTALL).matcher(html);
            assertThat(matcher.find()).isTrue(); String section = matcher.group(1);
            assertThat(section).contains("href=\"/quiz\"", "href=\"/quiz/results\"");
            assertThat(section.contains("href=\"/quiz/history\"")).isEqualTo(!role.equals("GUEST"));
            assertThat(section.contains("href=\"/staff/products\"")).isEqualTo(role.equals("STAFF") || role.equals("ADMIN"));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"STAFF", "ADMIN"})
    void managementCanEditProfileWithoutOverwritingBusinessStatus(String role) throws Exception {
        Long id = pets.findAll().getFirst().getId();
        db(() -> { ReflectionTestUtils.setField(pets.findById(id).orElseThrow(), "status", PetStatus.RESERVED); return null; });
        var form = products.get(id).toForm();
        String html = mvc.perform(get("/staff/products/" + id + "/edit").with(user("editor").roles(role)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"profile.energyLevel\"", "name=\"profile.version\"");
        mvc.perform(post("/staff/products/save").with(user("editor").roles(role)).with(csrf())
            .param("id", id.toString()).param("version", form.getVersion().toString()).param("type", "PET")
            .param("name", "Mèo nhỏ có dấu").param("profile.version", form.getProfile().getVersion().toString())
            .param("profile.energyLevel", "5").param("profile.childFriendly", "true").param("status", "AVAILABLE"))
            .andExpect(redirectedUrl("/staff/products"));
        var saved = products.get(id).toForm();
        assertThat(saved.getProfile().getEnergyLevel()).isEqualTo(5);
        assertThat(saved.getStatus()).isEqualTo(PetStatus.RESERVED);
        assertThatThrownBy(() -> products.save(id, form)).isInstanceOf(ProductVersionConflictException.class);
        mvc.perform(post("/staff/products/save").with(user("customer").roles("CUSTOMER")).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test void profileOnlyEditDetectsStaleFormAndValidationDoesNotPersist() throws Exception {
        Long id = pets.findAll().getFirst().getId();
        var first = products.get(id).toForm(); var stale = products.get(id).toForm();
        first.getProfile().setEnergyLevel(5); products.save(id, first);
        assertThatThrownBy(() -> products.save(id, stale)).isInstanceOf(ProductVersionConflictException.class);
        var current = products.get(id).toForm();
        mvc.perform(post("/staff/products/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("id", id.toString()).param("version", current.getVersion().toString()).param("name", "Tên không được lưu")
            .param("profile.version", current.getProfile().getVersion().toString()).param("profile.energyLevel", "9"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "profile.energyLevel"));
        assertThat(products.get(id).getName()).isNotEqualTo("Tên không được lưu");
    }

    @Test void legacyCatalogFormPreservesExistingProfile() throws Exception {
        Long id = pets.findAll().getFirst().getId();
        var first = products.get(id).toForm();
        first.getProfile().setEnergyLevel(5); products.save(id, first);
        var current = products.get(id).toForm();
        mvc.perform(post("/staff/products/save").with(user("editor").roles("STAFF")).with(csrf())
            .param("id", id.toString()).param("version", current.getVersion().toString())
            .param("type", "PET").param("name", "Sửa thông tin từ form cũ"))
            .andExpect(redirectedUrl("/staff/products"));
        var saved = products.get(id).toForm();
        assertThat(saved.getName()).isEqualTo("Sửa thông tin từ form cũ");
        assertThat(saved.getProfile().getEnergyLevel()).isEqualTo(5);
        assertThat(saved.getProfile().getVersion()).isEqualTo(current.getProfile().getVersion());
    }

    @Test void resultCardAndExplanationUseTheSameRoundingAtHalfCent() throws Exception {
        var pet = pets.findAll().getFirst();
        var result = new MatchResult(pet, 2.675, java.util.List.of("Kiểm tra làm tròn"),
            java.util.List.of(new MatchComponent("Thời gian", 0.02675, 1, 2.675)));
        var report = QuizReport.from(new QuizResult(null,
            MatchingStrategyTest.answers(HomeSize.LARGE, false, 3, 2, 3, false, 3),
            "weighted", "Cân bằng các tiêu chí", java.time.LocalDateTime.of(2026, 11, 17, 12, 0), java.util.List.of(result)));
        var session = new MockHttpSession(); session.setAttribute("latestQuiz", report);
        String html = mvc.perform(get("/quiz/results").session(session)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<span>2.68</span>% phù hợp", "= 2.68%.", "2.68 điểm %.");
    }
}
