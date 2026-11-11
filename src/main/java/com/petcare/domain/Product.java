package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.petcare.dto.ProductForm;
@Entity @Table(name="products") @Inheritance(strategy=InheritanceType.JOINED) @Getter public abstract class Product  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable=false,columnDefinition="bigint default 0") private long version;
    @Column(nullable=false) protected String name;
    @Column(length=2000) protected String description;
    @Column(nullable=false,precision=19,scale=2) protected BigDecimal basePrice;
    protected String imageUrl;
    private boolean active=true;
    private LocalDateTime createdAt;
    @PrePersist void created() {
        createdAt=LocalDateTime.now();
    }
    public void hide() {
        active=false;
    }
    public void update(ProductForm f) {
        name=f.getName();
        description=f.getDescription();
        basePrice=f.getBasePrice();
        imageUrl=f.getImageUrl();
    }
    /**
     * Cập nhật thông tin từ trang quản lý. Sau khi tạo, tồn kho và trạng thái Pet
     * chỉ đổi qua confirm/cancel dưới CommerceLocks; lớp con bảo vệ các trường đó.
     * Không bổ sung thao tác điều chỉnh tồn kho riêng ở giai đoạn này.
     */
    public void updateCatalog(ProductForm f) {
        update(f);
    }
    public ProductForm toForm() {
        var f=new ProductForm();
        f.setVersion(version);
        f.setType(getType());
        f.setName(name);
        f.setDescription(description);
        f.setBasePrice(basePrice);
        f.setImageUrl(imageUrl);
        return f;
    }
    public abstract BigDecimal calculatePrice();
    public java.util.Map<String,String> getDetails() {
        return new java.util.LinkedHashMap<>();
    }
    public abstract String getCategory();
    public abstract ProductType getType();
}
