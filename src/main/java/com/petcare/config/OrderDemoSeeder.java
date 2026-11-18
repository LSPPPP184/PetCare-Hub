package com.petcare.config;

import com.petcare.domain.*;
import com.petcare.domain.purchase.PurchasePolicies;
import com.petcare.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.function.Consumer;

/** Đơn demo qua API State và policy tồn kho; chỉ tạo một lần khi bảng đơn còn trống. */
@Component @Profile("demo") @RequiredArgsConstructor
public class OrderDemoSeeder {
    private final OrderRepository orders;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final Clock clock;

    @Transactional
    public void seed() {
        if (orders.count() != 0) return;
        var customer = users.findByEmail("customer@petcare.vn").orElseThrow();
        var accessories = entityManager.createQuery("select a from Accessory a order by a.id", Accessory.class).getResultList();
        var current = YearMonth.now(clock);
        for (int month = 0; month < 6; month++) {
            var cart = new Cart(customer);
            for (int i = 0; i < 6; i++) cart.add(accessories.get(i), 1 + month % 2);
            var order = Order.fromCart(cart, "12 Nguyễn Trãi, Thành phố Hồ Chí Minh — đơn minh họa");
            order.assignSeedKey("DEMO:ORDER:MONTH:" + month);
            order.recordCreatedAt(current.minusMonths(month).atDay(1).atStartOfDay());
            confirm(order); order.ship(); order.complete(); orders.save(order);
        }
        List<Consumer<Order>> transitions = List.of(o -> {}, this::confirm,
            o -> { confirm(o); o.ship(); }, Order::cancel);
        for (int i = 0; i < transitions.size(); i++) {
            var cart = new Cart(customer); cart.add(accessories.get(6), 1);
            var order = Order.fromCart(cart, "24 Lê Lợi, Thành phố Hồ Chí Minh — đơn minh họa");
            order.assignSeedKey("DEMO:ORDER:STATE:" + i);
            transitions.get(i).accept(order); orders.save(order);
        }
    }

    private void confirm(Order order) {
        order.confirm();
        order.getItems().forEach(item -> PurchasePolicies.forProduct(item.getProduct())
            .consume(item.getProduct(), item.getQuantity()));
    }
}
