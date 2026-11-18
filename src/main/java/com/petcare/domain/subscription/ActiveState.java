package com.petcare.domain.subscription;

import com.petcare.domain.Subscription;
import java.time.LocalDate;

/** Gói có hiệu lực hết ngày endDate; lượt cuối chuyển hết hạn ngay. */
public final class ActiveState extends AbstractSubscriptionState {
    public SubscriptionStatus useOnce(Subscription s, LocalDate today) {
        if (!s.isUsableOn(today)) throw invalid();
        return s.getRemainingUses() == 1 ? SubscriptionStatus.EXPIRED : SubscriptionStatus.ACTIVE;
    }
    public SubscriptionStatus expire(Subscription s, LocalDate today) {
        if (!s.isDue(today)) throw invalid();
        return SubscriptionStatus.EXPIRED;
    }
    public SubscriptionStatus cancel(Subscription s) { return SubscriptionStatus.CANCELLED; }
    public SubscriptionStatus renew(Subscription s, LocalDate today) {
        if (!s.canRenew(today) && !s.isRenewalPending()) throw invalid();
        return SubscriptionStatus.ACTIVE;
    }
}
