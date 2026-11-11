package com.petcare.domain.purchase;

import com.petcare.domain.*;

/** Adapter tồn kho phụ kiện; dùng API cập nhật có sẵn và bảo toàn các trường khác. */
public final class AccessoryPurchasePolicy extends TypedPurchasePolicy<Accessory> {
    public AccessoryPurchasePolicy() { super(Accessory.class); }
    @Override protected void validateQuantity(Accessory product, int quantity) {
        if (product.getStockQuantity() == null || quantity > product.getStockQuantity())
            throw new PurchaseException("Phụ kiện không đủ tồn kho.");
    }
    @Override public void consume(Product product, int quantity) {
        validate(product, quantity);
        var accessory = entity(product);
        var form = accessory.toForm();
        form.setStockQuantity(accessory.getStockQuantity() - quantity);
        accessory.update(form);
    }
    @Override public void restore(Product product, int quantity) {
        var accessory = entity(product);
        if (quantity <= 0 || accessory.getStockQuantity() == null ||
            (long) accessory.getStockQuantity() + quantity > Integer.MAX_VALUE)
            throw new PurchaseException("Số lượng hoàn kho không hợp lệ.");
        var form = accessory.toForm();
        form.setStockQuantity(accessory.getStockQuantity() + quantity);
        accessory.update(form);
    }
}
