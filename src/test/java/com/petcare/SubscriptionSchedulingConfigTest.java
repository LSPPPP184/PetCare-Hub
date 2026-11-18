package com.petcare;

import com.petcare.config.SubscriptionSchedulingConfig;
import com.petcare.repository.SubscriptionRepository;
import com.petcare.scheduler.SubscriptionScheduler;
import com.petcare.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionSchedulingConfigTest {
    @Configuration(proxyBeanMethods = false) @Import({SubscriptionScheduler.class, SubscriptionSchedulingConfig.class})
    static class Config {
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
        @Bean SubscriptionRepository subscriptions() { return mock(SubscriptionRepository.class); }
        @Bean SubscriptionService service() { return mock(SubscriptionService.class); }
    }
    @Test void dailyTaskRunsIndependentlyOfVaccinationScheduler() {
        new ApplicationContextRunner().withUserConfiguration(Config.class).withPropertyValues("petcare.vaccinations.scheduler.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(SubscriptionScheduler.class);
            var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
            assertThat(tasks).hasSize(1);
            assertThat(((CronTask) tasks.iterator().next().getTask()).getExpression()).isEqualTo("0 10 1 * * *");
        });
    }
}
