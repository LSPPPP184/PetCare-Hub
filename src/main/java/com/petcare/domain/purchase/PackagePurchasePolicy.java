package com.petcare.domain.purchase;

import com.petcare.domain.*;

/** Gói tháng không có tồn kho vật lý; chặn số lượt cộng dồn vượt giới hạn trước khi tạo đơn. */
public final class PackagePurchasePolicy extends TypedPurchasePolicy<MonthlyCarePackage> {
    public PackagePurchasePolicy() { super(MonthlyCarePackage.class); }
    @Override protected void validateQuantity(MonthlyCarePackage product, int quantity) {
        com.petcare.domain.subscription.PackageCoverage.requireValid(product.getCoveredServices());
        if (product.getIncludedUses() == null || product.getIncludedUses() <= 0) return;
        try { Math.multiplyExact(product.getIncludedUses(), quantity); }
        catch (ArithmeticException ex) { throw new PurchaseException("Số lượt của gói vượt giới hạn cho phép."); }
    }
    @Override public void consume(Product product, int quantity) { validate(product, quantity); }
    @Override public void restore(Product product, int quantity) {}
}
