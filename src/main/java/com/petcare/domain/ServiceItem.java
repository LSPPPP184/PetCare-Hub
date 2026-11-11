package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import com.petcare.dto.ProductForm;
@Entity @Table(name="service_items") @Getter public abstract class ServiceItem extends Product  {
    protected Integer durationMinutes;
    @Override public java.util.Map<String,String> getDetails() {
        var details=super.getDetails();
        details.put("Thời lượng (phút)",String.valueOf(durationMinutes));
        return details;
    }
    @Override public void update(ProductForm f) {
        super.update(f);
        durationMinutes=f.getDurationMinutes();
    }
    @Override public ProductForm toForm() {
        var f=super.toForm();
        f.setDurationMinutes(durationMinutes);
        return f;
    }
}
