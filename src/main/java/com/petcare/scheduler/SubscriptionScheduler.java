package com.petcare.scheduler;
import com.petcare.repository.SubscriptionRepository;
import com.petcare.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;

/** Chạy hằng ngày theo giờ Việt Nam; Clock inject giúp kiểm chứng đúng ngày và sau ngày hết hạn. */
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "petcare.subscriptions.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionScheduler {
    private final SubscriptionRepository subscriptions;
    private final SubscriptionService service;
    private final Clock clock;
    @Scheduled(cron = "${petcare.subscriptions.scheduler.cron:0 10 1 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void expireSubscriptions() {
        var today = LocalDate.now(clock);
        subscriptions.findDueIds(today).forEach(id -> service.expireIfDue(id, today));
    }
}
