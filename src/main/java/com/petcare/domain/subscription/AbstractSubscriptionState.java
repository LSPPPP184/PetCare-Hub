package com.petcare.domain.subscription;

import com.petcare.domain.Subscription;
import com.petcare.domain.order.InvalidStateTransitionException;
import java.time.LocalDate;

/** Mặc định từ chối; mỗi State chỉ mở các thao tác được phép. */
public abstract class AbstractSubscriptionState implements SubscriptionState {
    protected InvalidStateTransitionException invalid() {
        return new InvalidStateTransitionException("Thao tác không hợp lệ với trạng thái hoặc thời hạn của gói.");
    }
    public SubscriptionStatus useOnce(Subscription s, LocalDate today) { throw invalid(); }
    public SubscriptionStatus expire(Subscription s, LocalDate today) { throw invalid(); }
    public SubscriptionStatus cancel(Subscription s) { throw invalid(); }
    public SubscriptionStatus renew(Subscription s, LocalDate today) { throw invalid(); }
}
