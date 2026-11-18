package com.petcare.event;
import com.petcare.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Observer cấp gói trong giao dịch hoàn tất đơn; lỗi cấp gói rollback cả đơn và điểm. */
@Component @RequiredArgsConstructor
public class SubscriptionEventListener {
    private final SubscriptionService subscriptions;
    @EventListener @Order(-10) @Transactional(propagation = Propagation.MANDATORY)
    public void onOrder(OrderCompletedEvent event) { subscriptions.grantOrder(event.orderId()); }
}
