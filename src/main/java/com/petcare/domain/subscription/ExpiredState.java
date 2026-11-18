package com.petcare.domain.subscription;

import com.petcare.domain.Subscription;
import java.time.LocalDate;

/** Gói hết hạn có thể gia hạn hoặc đóng hẳn, không được dùng lượt. */
public final class ExpiredState extends AbstractSubscriptionState {
    public SubscriptionStatus renew(Subscription s, LocalDate today) { return SubscriptionStatus.ACTIVE; }
    public SubscriptionStatus cancel(Subscription s) { return SubscriptionStatus.CANCELLED; }
}
