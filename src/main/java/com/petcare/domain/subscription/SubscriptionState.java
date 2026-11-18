package com.petcare.domain.subscription;

import com.petcare.domain.Subscription;
import java.time.LocalDate;

/** State tách quy tắc chuyển trạng thái gói, tránh phân nhánh nghiệp vụ trong service. */
public interface SubscriptionState {
    SubscriptionStatus useOnce(Subscription subscription, LocalDate today);
    SubscriptionStatus expire(Subscription subscription, LocalDate today);
    SubscriptionStatus cancel(Subscription subscription);
    SubscriptionStatus renew(Subscription subscription, LocalDate today);
}
