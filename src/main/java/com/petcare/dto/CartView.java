package com.petcare.dto;

import com.petcare.domain.Cart;
import java.math.BigDecimal;
import java.util.List;

public record CartView(List<Line> items, BigDecimal total) {
    public record Line(Long productId, String name, BigDecimal unitPrice, int quantity,
                       BigDecimal total, boolean active, String problem) {}

    public static CartView empty() { return new CartView(List.of(), BigDecimal.ZERO); }
    public static CartView from(Cart cart) {
        var items = cart.getItems().stream().map(item -> {
            var product = item.getProduct();
            var price = product.calculatePrice();
            return new Line(product.getId(), product.getName(), price, item.getQuantity(),
                price.multiply(BigDecimal.valueOf(item.getQuantity())), product.isActive(),
                PurchaseAvailability.problem(product, item.getQuantity()));
        }).toList();
        return new CartView(items, items.stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    public boolean isCheckoutAllowed() {
        return !items.isEmpty() && items.stream().allMatch(item -> item.problem() == null);
    }
}
