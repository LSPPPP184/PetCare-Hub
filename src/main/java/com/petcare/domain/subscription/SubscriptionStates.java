package com.petcare.domain.subscription;

import java.util.Map;

/** Ánh xạ trạng thái lưu trong DB sang đối tượng State đa hình. */
public final class SubscriptionStates {
    private SubscriptionStates() {}
    private static final Map<SubscriptionStatus, SubscriptionState> STATES = Map.of(
        SubscriptionStatus.ACTIVE, new ActiveState(), SubscriptionStatus.EXPIRED, new ExpiredState(),
        SubscriptionStatus.CANCELLED, new CancelledState());
    public static SubscriptionState from(SubscriptionStatus status) { return STATES.get(status); }
}
