package com.petcare.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduler gói độc lập với cấu hình bật/tắt scheduler tiêm phòng. */
@Configuration @EnableScheduling
@ConditionalOnProperty(name = "petcare.subscriptions.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionSchedulingConfig {}
