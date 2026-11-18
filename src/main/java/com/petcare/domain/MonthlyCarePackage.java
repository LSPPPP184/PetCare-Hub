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
    @ManyToMany @JoinTable(name = "monthly_package_services", joinColumns = @JoinColumn(name = "package_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id"))
    private java.util.Set<ServiceItem> coveredServices = new java.util.HashSet<>();
    public java.util.Set<ServiceItem> getCoveredServices() { return java.util.Collections.unmodifiableSet(coveredServices); }
    @Override public java.util.Optional<MonthlyCarePackage> subscriptionPackage() { return java.util.Optional.of(this); }
    @Override public void configureCoveredServices(java.util.Collection<ServiceItem> services) {
        if (services == null || services.isEmpty())
            throw new com.petcare.domain.appointment.AppointmentException("Gói tháng phải có ít nhất một dịch vụ áp dụng hợp lệ.");
        services.forEach(com.petcare.domain.appointment.AppointmentServices::require);
        coveredServices.clear(); coveredServices.addAll(services);
    }
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
        f.setCoveredServiceIds(coveredServices.stream().map(ServiceItem::getId).toList());
        return f;
    }
}
