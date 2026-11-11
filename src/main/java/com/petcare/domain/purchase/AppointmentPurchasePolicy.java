package com.petcare.domain.purchase;

import com.petcare.domain.Product;

/** Dịch vụ cần lịch hẹn không được đặt qua giỏ; luồng đặt lịch thuộc GĐ4. */
public final class AppointmentPurchasePolicy implements ProductPurchasePolicy {
    private PurchaseException rejected() { return new PurchaseException("Dịch vụ này cần đặt lịch hẹn, không thể thêm vào giỏ."); }
    @Override public void validate(Product product, int quantity) { throw rejected(); }
    @Override public void consume(Product product, int quantity) { throw rejected(); }
    @Override public void restore(Product product, int quantity) { throw rejected(); }
}
