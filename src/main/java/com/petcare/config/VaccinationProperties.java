package com.petcare.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "petcare.vaccinations")
@Validated
@Getter @Setter
public class VaccinationProperties {
    @Min(value = 0, message = "Ngưỡng sắp đến hạn không được âm.")
    @Max(value = 365, message = "Ngưỡng sắp đến hạn tối đa 365 ngày.")
    private int dueSoonDays = 7;
    @Min(value = 1, message = "Phải sinh ít nhất một năm nhắc lại.")
    @Max(value = 20, message = "Chỉ sinh tối đa 20 năm nhắc lại.")
    private int annualBoosterYears = 3;
}
