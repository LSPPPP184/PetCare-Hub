package com.petcare.domain.purchase;

import com.petcare.domain.*;
import java.util.Map;

/** Registry chọn Adapter theo ProductType, không phân nhánh theo loại ở service. */
public final class PurchasePolicies {
    private PurchasePolicies() {}
    private static final Map<ProductType, ProductPurchasePolicy> POLICIES = Map.of(
        ProductType.PET, new PetPurchasePolicy(),
        ProductType.ACCESSORY, new AccessoryPurchasePolicy(),
        ProductType.GROOMING, new AppointmentPurchasePolicy(),
        ProductType.VET, new AppointmentPurchasePolicy(),
        ProductType.MONTHLY, new PackagePurchasePolicy());
    public static ProductPurchasePolicy forProduct(Product product) { return POLICIES.get(product.getType()); }
}
