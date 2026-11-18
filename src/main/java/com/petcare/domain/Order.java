package com.petcare.domain;

import com.petcare.domain.order.*;
import com.petcare.domain.purchase.PurchaseException;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@EntityListeners(com.petcare.event.OrderTimestampListener.class)
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @Column(length = 100, updatable = false) private String seedKey;
    public void assignSeedKey(String key) {
        if (seedKey != null || id != null || key == null || key.isBlank() || key.length() > 100)
            throw new IllegalArgumentException("Khóa dữ liệu mẫu không hợp lệ.");
        seedKey = key;
    }
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false) private User customer;
    @ManyToOne(fetch = FetchType.LAZY) private Subscription renewalSubscription;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal totalAmount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal discountAmount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal finalAmount;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false, length = 1000) private String shippingAddress;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OrderStatus stateName = OrderStatus.PENDING;

    public static Order fromCart(Cart cart, String shippingAddress) {
        if (cart.getItems().isEmpty()) throw new PurchaseException("Giỏ hàng đang trống.");
        if (cart.getItems().stream().filter(i -> i.getProduct().subscriptionPackage().isPresent()).count() > 1)
            throw new PurchaseException("Mỗi đơn chỉ mua một loại gói tháng. Vui lòng tách các loại gói thành đơn riêng.");
        if (shippingAddress == null || shippingAddress.isBlank() || shippingAddress.strip().length() > 1000)
            throw new PurchaseException("Địa chỉ giao hàng bắt buộc và tối đa 1000 ký tự.");
        var order = new Order();
        order.customer = cart.getCustomer();
        order.shippingAddress = shippingAddress.strip();
        for (var item : cart.getItems()) order.items.add(new OrderItem(order, item.getProduct(), item.getQuantity()));
        order.totalAmount = OrderItem.money(order.items.stream().map(OrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        order.discountAmount = new BigDecimal("0.00");
        order.finalAmount = order.totalAmount.subtract(order.discountAmount);
        return order;
    }
    public void recordCreatedAt(LocalDateTime time) { if (createdAt == null) createdAt = time; }
    public void renewSubscription(Subscription subscription) {
        if (id != null || !Objects.equals(customer.getId(), subscription.getCustomer().getId()) || items.size() != 1
                || !Objects.equals(items.getFirst().getProduct().getId(), subscription.getCarePackage().getId()))
            throw new PurchaseException("Đơn gia hạn không hợp lệ.");
        renewalSubscription = subscription;
    }
    /** Chỉ chốt giảm giá khi tạo mới, hạng thay đổi không cập nhật lại đơn đã lưu. */
    public void snapshotDiscount(BigDecimal discount) {
        if (id != null || discount == null || discount.signum() < 0 || discount.compareTo(totalAmount) > 0)
            throw new PurchaseException("Khoản giảm giá không hợp lệ hoặc đơn đã được lưu.");
        discountAmount = OrderItem.money(discount);
        finalAmount = totalAmount.subtract(discountAmount);
    }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    @Transient public OrderState getState() { return OrderStateFactory.from(stateName); }
    public void confirm() { stateName = getState().confirm(this); }
    public void ship() { stateName = getState().ship(this); }
    public void complete() { stateName = getState().complete(this); }
    public boolean cancel() {
        var previous = getState();
        stateName = previous.cancel(this);
        return previous.restoresStockOnCancel();
    }
}
