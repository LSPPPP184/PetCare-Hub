package com.petcare.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(auth->auth
   .requestMatchers("/admin/**").hasRole("ADMIN")
   .requestMatchers("/staff/**").hasAnyRole("STAFF","ADMIN")
   .requestMatchers("/cart", "/cart/**", "/checkout", "/orders", "/orders/**").hasRole("CUSTOMER")
   .requestMatchers("/","/login","/register","/403","/error").permitAll()
   .anyRequest().authenticated())
   .formLogin(form->form.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/",true).permitAll())
   .logout(logout->logout.logoutSuccessUrl("/login?logout"))
   .exceptionHandling(ex->ex.accessDeniedPage("/403")).build();
 }
}
