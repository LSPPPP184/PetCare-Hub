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
   .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR, jakarta.servlet.DispatcherType.FORWARD).permitAll()
   .requestMatchers("/quiz/history", "/quiz/history/**").authenticated()
   .requestMatchers("/quiz", "/quiz/results").permitAll()
   .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
   .requestMatchers("/membership", "/membership/**").hasRole("CUSTOMER")
   .requestMatchers("/subscriptions", "/subscriptions/**").hasRole("CUSTOMER")
   .requestMatchers("/staff", "/staff/**").hasAnyRole("STAFF","ADMIN")
   .requestMatchers("/my-pets", "/my-pets/**").hasRole("CUSTOMER")
   .requestMatchers("/appointments/services").permitAll()
   .requestMatchers("/appointments", "/appointments/**").hasRole("CUSTOMER")
   .requestMatchers("/cart", "/cart/**", "/checkout", "/orders", "/orders/**").hasRole("CUSTOMER")
   .requestMatchers("/","/login","/register","/403","/error").permitAll()
   .requestMatchers(org.springframework.http.HttpMethod.GET, "/**").permitAll()
   .anyRequest().denyAll())
   .csrf(org.springframework.security.config.Customizer.withDefaults())
   .formLogin(form->form.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/",true).permitAll())
   .logout(logout->logout.logoutSuccessUrl("/login?logout"))
   .exceptionHandling(ex->ex.accessDeniedPage("/403")).build();
 }
}
