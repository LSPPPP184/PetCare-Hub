package com.petcare.config;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration public class ProductSecurityConfig  {
    @Bean @Order(1) SecurityFilterChain publicProducts(HttpSecurity http)throws Exception {
        return http.securityMatcher("/products","/products/**")
            .authorizeHttpRequests(a->a.requestMatchers(org.springframework.http.HttpMethod.GET, "/**").permitAll().anyRequest().denyAll())
            .csrf(org.springframework.security.config.Customizer.withDefaults()).build();
    }
}
