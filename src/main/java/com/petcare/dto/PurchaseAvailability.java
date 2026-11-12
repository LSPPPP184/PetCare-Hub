package com.petcare.dto;

import com.petcare.domain.Product;
import com.petcare.domain.purchase.PurchaseException;
import com.petcare.domain.purchase.PurchasePolicies;

/** Dùng chính policy mua hàng để giải thích lý do sản phẩm chưa thể mua trên giao diện. */
public final class PurchaseAvailability {
    private PurchaseAvailability() {}
    public static String problem(Product product, int quantity) {
        try {
            PurchasePolicies.forProduct(product).validate(product, quantity);
            return null;
        } catch (PurchaseException ex) {
            return ex.getMessage();
        }
    }
}
