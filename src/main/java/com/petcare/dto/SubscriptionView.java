package com.petcare.dto;

import com.petcare.domain.Subscription;
import com.petcare.domain.subscription.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;

/** Dữ liệu view được chụp trong giao dịch, không tải lười ở Thymeleaf. */
public record SubscriptionView(Long id, String customerName, String packageName, Long packageId,
        LocalDate startDate, LocalDate endDate, int remainingUses, String stateLabel,
        boolean renewable, boolean cancellable, List<String> services) {
    public static SubscriptionView from(Subscription s, LocalDate today) {
        var effective = s.getStateName() == SubscriptionStatus.ACTIVE && s.isDue(today) ? SubscriptionStatus.EXPIRED : s.getStateName();
        return new SubscriptionView(s.getId(), s.getCustomer().getFullName(), s.getCarePackage().getName(), s.getCarePackage().getId(),
            s.getStartDate(), s.getEndDate(), s.getRemainingUses(), effective.getLabel(), s.canRenew(today) && !s.isRenewalPending(),
            s.getStateName() != SubscriptionStatus.CANCELLED && !s.isRenewalPending(), s.getCoveredServices().stream().map(p -> p.getName()).sorted().toList());
    }
}
