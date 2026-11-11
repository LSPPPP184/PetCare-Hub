package com.petcare.domain.purchase;

import com.petcare.domain.*;

/**
 * Adapter thú cưng: xác nhận đánh dấu SOLD ngay; hủy đơn đã xác nhận trả AVAILABLE.
 * Đơn PENDING không giữ chỗ; trạng thái được kiểm tra lại dưới khóa tại confirm.
 */
public final class PetPurchasePolicy extends TypedPurchasePolicy<Pet> {
    public PetPurchasePolicy() { super(Pet.class); }
    @Override protected void validateQuantity(Pet pet, int quantity) {
        if (quantity != 1) throw new PurchaseException("Mỗi thú cưng chỉ được mua một con.");
        if (pet.getStatus() != PetStatus.AVAILABLE) throw new PurchaseException("Thú cưng không còn sẵn sàng để mua.");
    }
    @Override public void consume(Product product, int quantity) {
        validate(product, quantity);
        var pet = entity(product);
        var form = pet.toForm();
        form.setStatus(PetStatus.SOLD);
        pet.update(form);
    }
    @Override public void restore(Product product, int quantity) {
        var pet = entity(product);
        if (quantity != 1 || pet.getStatus() != PetStatus.SOLD)
            throw new PurchaseException("Trạng thái thú cưng không hợp lệ để hoàn kho.");
        var form = pet.toForm();
        form.setStatus(PetStatus.AVAILABLE);
        pet.update(form);
    }
}
