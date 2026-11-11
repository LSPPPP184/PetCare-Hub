package com.petcare.dto;
import java.math.BigDecimal;
import com.petcare.domain.Product;
public record ProductView(Long id,String name,String description,String imageUrl,BigDecimal price,String category,boolean active,java.util.Map<String,String> details) {
    public boolean isImageAvailable() {
        return imageUrl != null && !imageUrl.isEmpty() && ProductForm.isAllowedImageUrl(imageUrl);
    }
    public static ProductView from(Product p) {
        return new ProductView(p.getId(),p.getName(),p.getDescription(),p.getImageUrl(),p.calculatePrice(),p.getCategory(),p.isActive(),p.getDetails());
    }
}
