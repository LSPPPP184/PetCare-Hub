package com.petcare.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Chỉ bật hạ tầng lập lịch khi tác vụ tiêm phòng được cho phép. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "petcare.vaccinations.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VaccinationSchedulingConfig {}
