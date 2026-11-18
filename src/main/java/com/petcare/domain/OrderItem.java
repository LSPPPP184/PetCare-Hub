package com.petcare.domain;

import com.petcare.domain.purchase.*;
import com.petcare.domain.subscription.SubscriptionBenefits;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.util.*;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal unitPrice;
    // Cột nullable để ddl-auto=update giữ nguyên dòng hàng cũ; không có setter chỉnh quyền lợi đã chốt.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_package_id", updatable = false) private MonthlyCarePackage snapshotPackage;
    @Column(name = "snapshot_included_uses", updatable = false) private Integer snapshotIncludedUses;
    @Column(name = "snapshot_duration_months", updatable = false) private Integer snapshotDurationMonths;
    @ManyToMany @JoinTable(name = "order_item_snapshot_services", joinColumns = @JoinColumn(name = "order_item_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceItem> snapshotServices = new HashSet<>();

    OrderItem(Order order, Product product, int quantity) {
        PurchasePolicies.forProduct(product).validate(product, quantity);
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = money(product.calculatePrice());
        product.subscriptionPackage().map(SubscriptionBenefits::capture).ifPresent(benefits -> {
            snapshotPackage = benefits.carePackage();
            snapshotIncludedUses = benefits.includedUses();
            snapshotDurationMonths = benefits.durationMonths();
            snapshotServices.addAll(benefits.coveredServices());
        });
    }
    public Set<ServiceItem> getSnapshotServices() { return Collections.unmodifiableSet(snapshotServices); }
    /**
     * Đơn mua và gia hạn mới chụp quyền lợi cùng giá trong constructor. snapshotPackage là dấu có
     * bản chụp; bản chụp cũ có dịch vụ rỗng bị từ chối cấp, không tự lấy danh sách mới thay thế.
     * Dòng cũ có các cột snapshot NULL dùng cấu hình gói
     * hiện hành lúc hoàn tất vì không thể khôi phục cấu hình lịch sử; không sửa/xóa dữ liệu cũ.
     */
    public Optional<SubscriptionBenefits> subscriptionBenefits() {
        if (snapshotPackage != null)
            return Optional.of(new SubscriptionBenefits(snapshotPackage, snapshotIncludedUses,
                snapshotDurationMonths, snapshotServices));
        return product.subscriptionPackage().map(SubscriptionBenefits::capture);
    }
    static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new PurchaseException("Số tiền không hợp lệ.");
        try {
            var result = value.setScale(2, RoundingMode.UNNECESSARY);
            if (result.precision() > 19) throw new PurchaseException("Số tiền vượt giới hạn lưu trữ.");
            return result;
        } catch (ArithmeticException ex) { throw new PurchaseException("Số tiền chỉ được có tối đa hai chữ số thập phân."); }
    }
    public BigDecimal getLineTotal() { return money(unitPrice.multiply(BigDecimal.valueOf(quantity))); }
}
