package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import com.petcare.dto.ProductForm;
/** Giá niêm yết; riêng chăm sóc ngoại hình cộng phụ phí kích thước đã nhập. Không cộng phụ phí thú cưng ở GĐ2. */
@Entity @Table(name="groomingservice_products") @Getter public class GroomingService extends ServiceItem  {
    private BigDecimal petSizeSurcharge;
    @Override public BigDecimal calculatePrice() {
        return basePrice.add(petSizeSurcharge);
    }
    @Override public java.util.Map<String,String> getDetails() {
        var details=super.getDetails();
        details.put("Phụ phí kích thước",String.valueOf(petSizeSurcharge));
        return details;
    }
    @Override public String getCategory() {
        return "SERVICE";
    }
    @Override public ProductType getType() {
        return ProductType.GROOMING;
    }
    @Override public void update(ProductForm f) {
        super.update(f);
        petSizeSurcharge=f.getPetSizeSurcharge();
    }
    @Override public ProductForm toForm() {
        var f=super.toForm();
        f.setPetSizeSurcharge(petSizeSurcharge);
        return f;
    }
}
