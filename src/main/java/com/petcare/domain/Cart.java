package com.petcare.domain;

import com.petcare.domain.purchase.*;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(columnNames = "customer_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false) private User customer;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart(User customer) {
        if (customer == null || customer.getRole() != Role.CUSTOMER)
            throw new PurchaseException("Giỏ hàng phải thuộc về khách hàng.");
        this.customer = customer;
    }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }

    public void add(Product product, int quantity) {
        if (quantity <= 0) throw new PurchaseException("Số lượng phải lớn hơn 0.");
        var existing = items.stream().filter(i -> i.getProduct().getId().equals(product.getId())).findFirst();
        int total = quantity;
        try { if (existing.isPresent()) total = Math.addExact(existing.get().getQuantity(), quantity); }
        catch (ArithmeticException ex) { throw new PurchaseException("Số lượng quá lớn."); }
        PurchasePolicies.forProduct(product).validate(product, total);
        if (existing.isPresent()) existing.get().changeQuantity(total);
        else items.add(new CartItem(this, product, total));
    }
    public void setQuantity(Long productId, int quantity) {
        var item = items.stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst()
            .orElseThrow(() -> new PurchaseException("Sản phẩm không có trong giỏ."));
        PurchasePolicies.forProduct(item.getProduct()).validate(item.getProduct(), quantity);
        item.changeQuantity(quantity);
    }
    public void remove(Long productId) { items.removeIf(i -> i.getProduct().getId().equals(productId)); }
    public void clear() { items.clear(); }
}
