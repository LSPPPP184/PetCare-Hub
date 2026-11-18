package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.petcare.dto.ProductForm;
@Entity @EntityListeners(com.petcare.event.ProductTimestampListener.class)
@Table(name="products") @Inheritance(strategy=InheritanceType.JOINED) @Getter public abstract class Product  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable=false,columnDefinition="bigint default 0") private long version;
    @Column(length = 100, updatable = false) private String seedKey;
    public void assignSeedKey(String key) {
        if (seedKey != null || id != null || key == null || key.isBlank() || key.length() > 100)
            throw new IllegalArgumentException("Khóa dữ liệu mẫu không hợp lệ.");
        seedKey = key;
    }
    @Column(nullable=false) protected String name;
    @Column(length=2000) protected String description;
    @Column(nullable=false,precision=19,scale=2) protected BigDecimal basePrice;
    protected String imageUrl;
    private boolean active=true;
    private LocalDateTime createdAt;
    public void recordCreatedAt(LocalDateTime time) {
        if (createdAt == null) createdAt = time;
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
    /** Mặc định sản phẩm không cấp gói; lớp gói tháng cung cấp quyền lợi bằng đa hình. */
    public java.util.Optional<MonthlyCarePackage> subscriptionPackage() { return java.util.Optional.empty(); }
    public void configureCoveredServices(java.util.Collection<ServiceItem> services) {}
}
