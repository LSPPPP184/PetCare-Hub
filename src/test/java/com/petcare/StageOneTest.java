package com.petcare;
import com.petcare.domain.Role;
import com.petcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import jakarta.servlet.http.Cookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StageOneTest {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired PasswordEncoder encoder;
 @Test void registrationAndDuplicate() throws Exception {
  mvc.perform(post("/register").with(csrf()).param("fullName","Nguyễn An").param("email","New@petcare.vn").param("password","Password123").param("role","ADMIN"))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
  var saved=users.findByEmail("new@petcare.vn").orElseThrow();
  assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
  assertThat(encoder.matches("Password123",saved.getPassword())).isTrue();
  assertThat(saved.getCreatedAt()).isNotNull();
  mvc.perform(post("/register").with(csrf()).param("fullName","An").param("email","NEW@petcare.vn").param("password","Password123"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registration","email"));
 }
 @Test void invalidRegistrationAndCsrf() throws Exception {
  mvc.perform(post("/register").with(csrf()).param("fullName","").param("email","invalid").param("password","123"))
   .andExpect(model().attributeHasFieldErrors("registration","fullName","email","password"));
  mvc.perform(post("/register")).andExpect(status().isForbidden());
 }
 @Test void registrationValidatesAndNormalizesPhone() throws Exception {
  String email = "phone-check@petcare.vn";
  mvc.perform(post("/register").with(csrf()).param("fullName", "Khách hàng")
    .param("email", email).param("password", "Password123").param("phone", "--------"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registration", "phone"));
  assertThat(users.existsByEmail(email)).isFalse();

  mvc.perform(post("/register").with(csrf()).param("fullName", "Khách hàng")
    .param("email", email).param("password", "Password123").param("phone", "  +84  901 234 567  "))
   .andExpect(redirectedUrl("/login"));
  assertThat(users.findByEmail(email).orElseThrow().getPhone()).isEqualTo("+84 901 234 567");
 }
 @Test void rolesAndPublicPages() throws Exception {
  for(String path:new String[]{"/","/login","/register"}) mvc.perform(get(path)).andExpect(status().isOk());
  mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
  for(String role:new String[]{"ADMIN","STAFF","CUSTOMER"}) {
   mvc.perform(get("/admin").with(user("tester").roles(role)))
    .andExpect(status().is(role.equals("ADMIN")?200:403));
   mvc.perform(get("/staff").with(user("tester").roles(role)))
    .andExpect(status().is(role.equals("CUSTOMER")?403:200));
  }
  mvc.perform(get("/admin").with(user("customer").roles("CUSTOMER"))).andExpect(forwardedUrl("/403"));
  mvc.perform(get("/403")).andExpect(status().isForbidden()).andExpect(content().string(org.hamcrest.Matchers.containsString("Không có quyền truy cập")));
 }
 @Test void seededLoginAndMenus() throws Exception {
  String[][] accounts={{"admin","Admin@123","ADMIN"},{"staff","Staff@123","STAFF"},{"customer","Customer@123","CUSTOMER"}};
  for(var account:accounts) {
   var loginResult = mvc.perform(formLogin().user("email", account[0] + "@petcare.vn").password(account[1]))
    .andExpect(authenticated().withRoles(account[2])).andReturn();
   var session = (MockHttpSession) loginResult.getRequest().getSession(false);
   assertThat(session).isNotNull();
   String sessionId = session.getId();
   var result = mvc.perform(get("/").session(session))
    .andExpect(status().isOk()).andExpect(authenticated().withRoles(account[2]))
    .andReturn().getResponse().getContentAsString();
   assertThat(result.contains("href=\"/admin\"")).isEqualTo(account[2].equals("ADMIN"));
   assertThat(result.contains("href=\"/staff\"")).isEqualTo(!account[2].equals("CUSTOMER"));
   mvc.perform(get("/admin").session(session))
    .andExpect(status().is(account[2].equals("ADMIN") ? 200 : 403));
   mvc.perform(get("/staff").session(session))
    .andExpect(status().is(account[2].equals("CUSTOMER") ? 403 : 200));

   mvc.perform(post("/logout").session(session).with(csrf()))
    .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
   assertThat(session.isInvalid()).isTrue();
   // Session cũ đã bị hủy; cookie cũ không cấp lại quyền truy cập.
   for (String path : new String[]{"/admin", "/staff"}) {
    mvc.perform(get(path).cookie(new Cookie("JSESSIONID", sessionId)))
     .andExpect(unauthenticated()).andExpect(redirectedUrl("http://localhost/login"));
   }
  }
  mvc.perform(formLogin().user("email","admin@petcare.vn").password("wrong")).andExpect(redirectedUrl("/login?error"));
 }
}
