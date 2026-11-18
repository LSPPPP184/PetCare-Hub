package com.petcare.dto;
import java.math.BigDecimal;
import com.petcare.domain.Product;
public record ProductView(Long id,String name,String description,String imageUrl,BigDecimal price,String category,boolean active,java.util.Map<String,String> details,String purchaseProblem,boolean appointmentService,boolean packageSuspended) {
    public boolean isPurchasable() { return purchaseProblem == null; }
    public boolean isImageAvailable() {
        return imageUrl != null && !imageUrl.isEmpty() && ProductForm.isAllowedImageUrl(imageUrl);
    }
    public static ProductView summary(Product p) { return create(p, java.util.Map.of()); }
    public static ProductView from(Product p) { return create(p, p.getDetails()); }
    private static ProductView create(Product p, java.util.Map<String,String> details) {
        return new ProductView(p.getId(),p.getName(),p.getDescription(),p.getImageUrl(),p.calculatePrice(),p.getCategory(),p.isActive(),details,PurchaseAvailability.problem(p,1),com.petcare.domain.appointment.AppointmentServices.supports(p),
            p.subscriptionPackage().map(pack -> !com.petcare.domain.subscription.PackageCoverage.isValid(pack.getCoveredServices())).orElse(false));
    }
}
