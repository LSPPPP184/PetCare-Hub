package com.petcare.domain;

import com.petcare.domain.subscription.*;
import com.petcare.domain.purchase.PurchaseException;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.*;

/** Quyền lợi lấy từ bản chụp dòng hàng; sửa danh mục không thay đổi quyền lợi đã chốt. */
@Entity @Table(name = "subscriptions", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_source", columnNames = "source_key"))
@org.hibernate.annotations.Check(constraints = "remaining_uses >= 0 and remaining_uses <= total_uses")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User customer;
    @ManyToOne(fetch = FetchType.LAZY) private OwnedPet ownedPet;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private MonthlyCarePackage carePackage;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false) private int remainingUses;
    @Column(nullable = false) private int totalUses;
    @Column(nullable = false, columnDefinition = "bit default 0") private boolean renewalPending;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SubscriptionStatus stateName;
    @Column(name = "source_key", nullable = false, length = 100, updatable = false) private String sourceKey;
    @ManyToMany @JoinTable(name = "subscription_services", joinColumns = @JoinColumn(name = "subscription_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceItem> coveredServices = new HashSet<>();

    public static Subscription create(User customer, OwnedPet pet, MonthlyCarePackage pack, LocalDate start, int quantity, String key) {
        return createFromBenefits(customer, pet, SubscriptionBenefits.capture(pack), start, quantity, key);
    }
    public static Subscription createFromBenefits(User customer, OwnedPet pet, SubscriptionBenefits benefits,
                                                  LocalDate start, int quantity, String key) {
        if (customer == null || customer.getRole() != Role.CUSTOMER || benefits == null || start == null
                || key == null || key.isBlank() || key.length() > 100 || quantity <= 0)
            throw new PurchaseException("Thông tin gói chăm sóc không hợp lệ.");
        if (pet != null && !Objects.equals(pet.getOwner().getId(), customer.getId()))
            throw new PurchaseException("Thú cưng không thuộc khách hàng.");
        var s = new Subscription();
        s.customer = customer; s.ownedPet = pet; s.carePackage = benefits.carePackage(); s.sourceKey = key;
        s.startDate = start; s.endDate = start.plusMonths(benefits.durationMonths()).minusDays(1);
        s.totalUses = Math.multiplyExact(benefits.includedUses(), quantity); s.remainingUses = s.totalUses;
        s.coveredServices.addAll(benefits.coveredServices()); s.stateName = SubscriptionStatus.ACTIVE;
        return s;
    }
    public Set<ServiceItem> getCoveredServices() { return Collections.unmodifiableSet(coveredServices); }
    @Transient public SubscriptionState getState() { return SubscriptionStates.from(stateName); }
    public boolean isDue(LocalDate today) { return today.isAfter(endDate) || remainingUses == 0; }
    public boolean isUsableOn(LocalDate date) {
        return stateName == SubscriptionStatus.ACTIVE && remainingUses > 0 && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    public boolean covers(Long serviceId) { return coveredServices.stream().anyMatch(s -> Objects.equals(s.getId(), serviceId)); }
    public boolean canRenew(LocalDate today) {
        return stateName == SubscriptionStatus.EXPIRED || (stateName == SubscriptionStatus.ACTIVE && !today.isBefore(endDate.minusDays(7)));
    }
    public void useOnce(LocalDate today) { stateName = getState().useOnce(this, today); remainingUses--; }
    public void expire(LocalDate today) { stateName = getState().expire(this, today); }
    public void cancel() {
        if (renewalPending) throw new com.petcare.domain.order.InvalidStateTransitionException("Gói đang chờ xử lý đơn gia hạn.");
        stateName = getState().cancel(this);
    }
    /** Chốt điều kiện tại lúc khách đặt đơn; hoàn lượt sau đó không làm kẹt đơn gia hạn đã được nhận. */
    public void requestRenewal(LocalDate today) {
        if (renewalPending || !canRenew(today))
            throw new com.petcare.domain.order.InvalidStateTransitionException("Gói chưa đến hạn gia hạn hoặc đã có đơn gia hạn đang chờ.");
        renewalPending = true;
    }
    public void releaseRenewal() { renewalPending = false; }
    /** Gia hạn nối tiếp nếu còn hạn, cộng lượt; hết hạn theo ngày thì bắt đầu chu kỳ mới và bỏ lượt cũ. */
    public void renew(LocalDate today, int quantity) {
        renew(today, quantity, SubscriptionBenefits.capture(carePackage));
    }
    /**
     * Luồng đơn hàng truyền quyền lợi đã chốt, không đọc lại số lượt/tháng/dịch vụ từ danh mục.
     * Giả định mỗi lần gia hạn thay toàn bộ dịch vụ bằng bản chụp mới, áp dụng cả cho lượt còn lại.
     * Nếu còn hạn thì cộng lượt và nối thời hạn; quá ngày hết hạn thì bỏ lượt cũ, bắt đầu chu kỳ mới.
     * Không gộp dịch vụ của các chu kỳ trước.
     */
    public void renew(LocalDate today, int quantity, SubscriptionBenefits benefits) {
        var next = getState().renew(this, today);
        if (quantity <= 0 || benefits == null || !Objects.equals(carePackage.getId(), benefits.carePackage().getId()))
            throw new PurchaseException("Quyền lợi gia hạn không hợp lệ.");
        int added = Math.multiplyExact(benefits.includedUses(), quantity);
        boolean elapsed = today.isAfter(endDate);
        remainingUses = Math.addExact(elapsed ? 0 : remainingUses, added);
        totalUses = Math.addExact(totalUses, added);
        if (elapsed) startDate = today;
        endDate = (elapsed ? today : endDate.plusDays(1)).plusMonths(benefits.durationMonths()).minusDays(1);
        coveredServices.clear();
        coveredServices.addAll(benefits.coveredServices()); stateName = next; renewalPending = false;
    }
    /** Chỉ service hoàn lịch được gọi: gói hủy/quá ngày không được hồi sinh. */
    public void restoreUse(LocalDate today) {
        if (remainingUses >= totalUses) throw new PurchaseException("Không thể hoàn vượt số lượt của gói.");
        remainingUses++;
        if (stateName == SubscriptionStatus.EXPIRED && !today.isAfter(endDate)) stateName = SubscriptionStatus.ACTIVE;
    }
}
