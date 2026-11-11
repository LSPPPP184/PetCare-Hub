package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import com.petcare.dto.ProductForm;
/** Giá niêm yết; riêng chăm sóc ngoại hình cộng phụ phí kích thước đã nhập. Không cộng phụ phí thú cưng ở GĐ2. */
@Entity @Table(name="monthlycarepackage_products") @Getter public class MonthlyCarePackage extends ServiceItem  {
    private Integer durationMonths;
    private Integer includedUses;
    private String includedServices;
    @Override public BigDecimal calculatePrice() {
        return basePrice;
    }
    @Override public java.util.Map<String,String> getDetails() {
        var details=super.getDetails();
        details.put("Số tháng",String.valueOf(durationMonths));
        details.put("Số lượt",String.valueOf(includedUses));
        details.put("Dịch vụ trong gói",String.valueOf(includedServices));
        return details;
    }
    @Override public String getCategory() {
        return "SERVICE";
    }
    @Override public ProductType getType() {
        return ProductType.MONTHLY;
    }
    @Override public void update(ProductForm f) {
        super.update(f);
        durationMonths=f.getDurationMonths();
        includedUses=f.getIncludedUses();
        includedServices=f.getIncludedServices();
    }
    @Override public ProductForm toForm() {
        var f=super.toForm();
        f.setDurationMonths(durationMonths);
        f.setIncludedUses(includedUses);
        f.setIncludedServices(includedServices);
        return f;
    }
}
