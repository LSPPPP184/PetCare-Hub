package com.petcare.domain.purchase;

import com.petcare.domain.Product;

/** Adapter đa hình bổ sung quy tắc mua hàng mà không sửa các lớp sản phẩm GĐ2. */
public interface ProductPurchasePolicy {
    void validate(Product product, int quantity);
    void consume(Product product, int quantity);
    void restore(Product product, int quantity);
}
