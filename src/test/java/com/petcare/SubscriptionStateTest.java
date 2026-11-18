package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.subscription.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class SubscriptionStateTest {
    static final LocalDate TODAY = LocalDate.of(2026, 1, 5);
    static Subscription subscription(int uses) {
        var form = ProductFactoryTest.form(ProductType.MONTHLY, "Gói chăm sóc toàn diện", "100000");
        form.setIncludedUses(uses);
        var pack = (MonthlyCarePackage) new ProductFactory().create(form);
        pack.configureCoveredServices(java.util.List.of((ServiceItem) new ProductFactory().create(
            ProductFactoryTest.form(ProductType.GROOMING, "Tắm tỉa", "100000"))));
        return Subscription.create(new User("Khách", "test@example.test", "hash", "", Role.CUSTOMER), null,
            pack, TODAY, 1, "TEST");
    }
    @ParameterizedTest @CsvSource({
        "ACTIVE,use,ACTIVE", "ACTIVE,expire,INVALID", "ACTIVE,cancel,CANCELLED", "ACTIVE,renew,INVALID",
        "EXPIRED,use,INVALID", "EXPIRED,expire,INVALID", "EXPIRED,cancel,CANCELLED", "EXPIRED,renew,ACTIVE",
        "CANCELLED,use,INVALID", "CANCELLED,expire,INVALID", "CANCELLED,cancel,INVALID", "CANCELLED,renew,INVALID"})
    void transitionTable(String initial, String action, String expected) {
        var s = subscription(2);
        if (initial.equals("EXPIRED")) { s.useOnce(TODAY); s.useOnce(TODAY); }
        if (initial.equals("CANCELLED")) s.cancel();
        Runnable work = java.util.Map.<String, Runnable>of("use", () -> s.useOnce(TODAY), "expire", () -> s.expire(TODAY),
            "cancel", s::cancel, "renew", () -> s.renew(TODAY, 1)).get(action);
        if (expected.equals("INVALID")) assertThatThrownBy(work::run).isInstanceOf(InvalidStateTransitionException.class);
        else { work.run(); assertThat(s.getStateName()).isEqualTo(SubscriptionStatus.valueOf(expected)); }
    }
    @Test void inclusiveExpiryAndLastUseAndRefund() {
        var s = subscription(1);
        assertThat(s.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 4));
        assertThat(s.isUsableOn(s.getEndDate())).isTrue();
        assertThatThrownBy(() -> s.expire(s.getEndDate())).isInstanceOf(InvalidStateTransitionException.class);
        s.useOnce(s.getEndDate());
        assertThat(s.getRemainingUses()).isZero(); assertThat(s.getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThatThrownBy(() -> s.useOnce(TODAY)).isInstanceOf(InvalidStateTransitionException.class);
        s.restoreUse(s.getEndDate()); assertThat(s.getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
        s.expire(s.getEndDate().plusDays(1));
        assertThat(s.getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
    }
    @Test void refundNeverRevivesCancelledOrTimeExpiredPackage() {
        var s = subscription(2); s.useOnce(TODAY); s.cancel(); s.restoreUse(TODAY);
        assertThat(s.getRemainingUses()).isEqualTo(2); assertThat(s.getStateName()).isEqualTo(SubscriptionStatus.CANCELLED);
        var elapsed = subscription(1); elapsed.useOnce(TODAY); elapsed.restoreUse(elapsed.getEndDate().plusDays(1));
        assertThat(elapsed.getRemainingUses()).isOne(); assertThat(elapsed.getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
    }
    @Test void renewalBoundaryPreservesUnusedRightsAndUsesCalendarMonths() {
        var s = subscription(4);
        assertThat(s.canRenew(s.getEndDate().minusDays(8))).isFalse();
        assertThat(s.canRenew(s.getEndDate().minusDays(7))).isTrue();
        s.renew(s.getEndDate().minusDays(7), 1);
        assertThat(s.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThat(s.getRemainingUses()).isEqualTo(8);
        var next = s.getEndDate().plusDays(1); s.expire(next); s.renew(next, 1);
        assertThat(s.getRemainingUses()).isEqualTo(4); assertThat(s.getStartDate()).isEqualTo(next);
    }
}
