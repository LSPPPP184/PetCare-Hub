package com.petcare;

import com.petcare.config.VaccinationProperties;
import com.petcare.config.VaccinationSchedulingConfig;
import com.petcare.repository.VaccinationRecordRepository;
import com.petcare.scheduler.VaccinationScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import java.time.Clock;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VaccinationSchedulingConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Configuration(proxyBeanMethods = false)
    @Import({VaccinationScheduler.class, VaccinationSchedulingConfig.class})
    @EnableConfigurationProperties(VaccinationProperties.class)
    static class Config {
        @Bean Clock healthClock() { return Clock.fixed(java.time.Instant.parse("2026-11-13T03:00:00Z"), java.time.ZoneOffset.UTC); }
        @Bean VaccinationRecordRepository records() { return mock(VaccinationRecordRepository.class); }
    }

    @Test void defaultsEnableDailyTaskAndCanBindThreshold() {
        runner.withPropertyValues("petcare.vaccinations.due-soon-days=10", "petcare.vaccinations.annual-booster-years=2")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(VaccinationScheduler.class);
                var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
                assertThat(tasks).hasSize(1);
                assertThat(((CronTask) tasks.iterator().next().getTask()).getExpression()).isEqualTo("0 0 1 * * *");
                assertThat(context.getBean(VaccinationProperties.class).getDueSoonDays()).isEqualTo(10);
                assertThat(context.getBean(VaccinationProperties.class).getAnnualBoosterYears()).isEqualTo(2);
            });
    }

    @Test void disabledPropertyRemovesSchedulerAndSchedulingInfrastructure() {
        runner.withPropertyValues("petcare.vaccinations.scheduler.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(VaccinationScheduler.class)
                .doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            verifyNoInteractions(context.getBean(VaccinationRecordRepository.class));
        });
    }

    @Test void cronCanBeConfigured() {
        runner.withPropertyValues("petcare.vaccinations.scheduler.cron=0 30 2 * * *").run(context -> {
            var task = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks().iterator().next();
            assertThat(((CronTask) task.getTask()).getExpression()).isEqualTo("0 30 2 * * *");
        });
    }

    @Test void invalidThresholdFailsFast() {
        runner.withPropertyValues("petcare.vaccinations.due-soon-days=-1")
            .run(context -> assertThat(context).hasFailed());
    }
}
