package com.petcare.domain.purchase;

import com.petcare.domain.Product;
import org.hibernate.Hibernate;

/** Adapter có kiểu, hỗ trợ cả entity và proxy Hibernate khi áp dụng quy tắc. */
public abstract class TypedPurchasePolicy<T extends Product> implements ProductPurchasePolicy {
    private final Class<T> type;
    protected TypedPurchasePolicy(Class<T> type) { this.type = type; }
    protected T entity(Product product) { return type.cast(Hibernate.unproxy(product)); }
    @Override public final void validate(Product product, int quantity) {
        if (quantity <= 0) throw new PurchaseException("Số lượng phải lớn hơn 0.");
        if (!product.isActive()) throw new PurchaseException("Sản phẩm đã ẩn, không thể mua.");
        validateQuantity(entity(product), quantity);
    }
    protected abstract void validateQuantity(T product, int quantity);
}
