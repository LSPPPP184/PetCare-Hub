package com.petcare.domain.purchase;

import com.petcare.domain.*;

/** Gói tháng có thể mua, không có tồn kho vật lý; quyền lợi thuê bao thuộc GĐ8. */
public final class PackagePurchasePolicy extends TypedPurchasePolicy<MonthlyCarePackage> {
    public PackagePurchasePolicy() { super(MonthlyCarePackage.class); }
    @Override protected void validateQuantity(MonthlyCarePackage product, int quantity) {}
    @Override public void consume(Product product, int quantity) { validate(product, quantity); }
    @Override public void restore(Product product, int quantity) {}
}
