package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import com.petcare.dto.ProductForm;
/** Giá niêm yết; riêng chăm sóc ngoại hình cộng phụ phí kích thước đã nhập. Không cộng phụ phí thú cưng ở GĐ2. */
@Entity @Table(name="vetservice_products") @Getter public class VetService extends ServiceItem  {
    private String doctorName;
    @Override public BigDecimal calculatePrice() {
        return basePrice;
    }
    @Override public java.util.Map<String,String> getDetails() {
        var details=super.getDetails();
        details.put("Bác sĩ phụ trách",String.valueOf(doctorName));
        return details;
    }
    @Override public String getCategory() {
        return "SERVICE";
    }
    @Override public ProductType getType() {
        return ProductType.VET;
    }
    @Override public void update(ProductForm f) {
        super.update(f);
        doctorName=f.getDoctorName();
    }
    @Override public ProductForm toForm() {
        var f=super.toForm();
        f.setDoctorName(doctorName);
        return f;
    }
}
