package com.petcare;
import com.petcare.domain.Role;
import com.petcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
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
 @Test void rolesAndPublicPages() throws Exception {
  for(String path:new String[]{"/","/login","/register"}) mvc.perform(get(path)).andExpect(status().isOk());
  mvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection());
  for(String role:new String[]{"ADMIN","STAFF","CUSTOMER"}) {
   mvc.perform(get("/admin/dashboard").with(user("tester").roles(role)))
    .andExpect(status().is(role.equals("ADMIN")?200:403));
   mvc.perform(get("/staff/dashboard").with(user("tester").roles(role)))
    .andExpect(status().is(role.equals("CUSTOMER")?403:200));
  }
  mvc.perform(get("/admin/dashboard").with(user("customer").roles("CUSTOMER"))).andExpect(forwardedUrl("/403"));
  mvc.perform(get("/403")).andExpect(status().isForbidden()).andExpect(content().string(org.hamcrest.Matchers.containsString("Không có quyền truy cập")));
 }
 @Test void seededLoginAndMenus() throws Exception {
  String[][] accounts={{"admin","Admin@123","ADMIN"},{"staff","Staff@123","STAFF"},{"customer","Customer@123","CUSTOMER"}};
  for(var account:accounts) {
   mvc.perform(formLogin().user("email",account[0]+"@petcare.vn").password(account[1])).andExpect(authenticated().withRoles(account[2]));
   var result=mvc.perform(get("/").with(user(account[0]).roles(account[2]))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertThat(result.contains("href=\"/admin/dashboard\"")).isEqualTo(account[2].equals("ADMIN"));
   assertThat(result.contains("href=\"/staff/dashboard\"")).isEqualTo(!account[2].equals("CUSTOMER"));
  }
  mvc.perform(formLogin().user("email","admin@petcare.vn").password("wrong")).andExpect(redirectedUrl("/login?error"));
  mvc.perform(post("/logout").with(user("customer")).with(csrf())).andExpect(redirectedUrl("/login?logout"));
 }
}
