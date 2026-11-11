package com.petcare.domain;

import com.petcare.domain.purchase.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;

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

    OrderItem(Order order, Product product, int quantity) {
        PurchasePolicies.forProduct(product).validate(product, quantity);
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = money(product.calculatePrice());
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
