package com.petcare.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice @RequiredArgsConstructor
public class HealthNavigationAdvice {
    private final Environment environment;
    @ModelAttribute("healthDemoEnabled") boolean demo() { return environment.acceptsProfiles(Profiles.of("demo")); }
}
