package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.domain.purchase.*;
import com.petcare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều phối State và tồn kho trong một transaction. Confirm đánh dấu Pet SOLD;
 * cancel từ CONFIRMED hoàn kho, từ PENDING không hoàn. SHIPPING không được hủy.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final CartRepository carts;
    private final CommerceAccess access;
    private final CommerceLocks locks;
    private final LoyaltyPricingService pricing;
    private final SubscriptionService subscriptions;
    private final org.springframework.context.ApplicationEventPublisher events;

    public Order placeOrderFromCart(Long customerId, String shippingAddress) {
        access.customer(customerId);
        locks.lock(User.class, customerId);
        var cart = carts.findByCustomerId(customerId)
            .orElseThrow(() -> new PurchaseException("Giỏ hàng đang trống."));
        locks.products(cart.getItems().stream().map(i -> i.getProduct().getId()).toList());
        var order = Order.fromCart(cart, shippingAddress);
        order.snapshotDiscount(pricing.snapshot(customerId, order.getTotalAmount(), true).discountAmount());
        orders.save(order);
        cart.clear();
        return order;
    }
    public Order confirm(Long orderId) {
        access.staff();
        var order = locks.lock(Order.class, orderId);
        order.confirm();
        lockProducts(order);
        // Kiểm tra cả đơn trước khi thay đổi bất kỳ dòng tồn kho nào.
        order.getItems().forEach(i -> PurchasePolicies.forProduct(i.getProduct()).validate(i.getProduct(), i.getQuantity()));
        order.getItems().forEach(i -> PurchasePolicies.forProduct(i.getProduct()).consume(i.getProduct(), i.getQuantity()));
        return order;
    }
    public Order ship(Long orderId) {
        access.staff();
        var order = locks.lock(Order.class, orderId);
        order.ship();
        return order;
    }
    public Order complete(Long orderId) {
        access.staff();
        var order = locks.lock(Order.class, orderId);
        order.complete();
        events.publishEvent(new com.petcare.event.OrderCompletedEvent(order.getId()));
        return order;
    }
    public Order cancel(Long orderId) {
        var order = locks.lock(Order.class, orderId);
        access.ownerOrStaff(order);
        var restoreStock = order.cancel();
        subscriptions.releaseRenewal(order);
        if (restoreStock) {
            lockProducts(order);
            order.getItems().forEach(i -> PurchasePolicies.forProduct(i.getProduct()).restore(i.getProduct(), i.getQuantity()));
        }
        return order;
    }
    private void lockProducts(Order order) {
        locks.products(order.getItems().stream().map(i -> i.getProduct().getId()).toList());
    }
}
