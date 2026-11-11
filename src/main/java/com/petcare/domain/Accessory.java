package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import com.petcare.dto.ProductForm;
/** Giá niêm yết; riêng chăm sóc ngoại hình cộng phụ phí kích thước đã nhập. Không cộng phụ phí thú cưng ở GĐ2. */
@Entity @Table(name="accessory_products") @Getter public class Accessory extends Product  {
    private String brand;
    private Integer stockQuantity;
    private String category;
    @Override public BigDecimal calculatePrice() {
        return basePrice;
    }
    @Override public java.util.Map<String,String> getDetails() {
        var details=super.getDetails();
        details.put("Thương hiệu",String.valueOf(brand));
        details.put("Tồn kho",String.valueOf(stockQuantity));
        details.put("Nhóm phụ kiện",String.valueOf(category));
        return details;
    }
    @Override public String getCategory() {
        return "ACCESSORY";
    }
    @Override public ProductType getType() {
        return ProductType.ACCESSORY;
    }
    @Override public void update(ProductForm f) {
        updateCatalog(f);
        stockQuantity=f.getStockQuantity();
    }
    /** Sửa thông tin phụ kiện, giữ nguyên tồn kho do đơn hàng quản lý. */
    @Override public void updateCatalog(ProductForm f) {
        super.update(f);
        brand=f.getBrand();
        category=f.getCategory();
    }
    @Override public ProductForm toForm() {
        var f=super.toForm();
        f.setBrand(brand);
        f.setStockQuantity(stockQuantity);
        f.setCategory(category);
        return f;
    }
}
