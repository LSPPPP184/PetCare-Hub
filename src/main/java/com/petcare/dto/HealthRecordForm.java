package com.petcare.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class HealthRecordForm {
    @NotNull(message = "Vui lòng nhập ngày khám.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate date;
    @NotBlank(message = "Vui lòng nhập loại khám.") @Size(max = 100, message = "Loại khám tối đa 100 ký tự.") private String type;
    @NotBlank(message = "Vui lòng nhập kết quả khám.") @Size(max = 2000, message = "Kết quả khám tối đa 2000 ký tự.") private String description;
    @DecimalMin(value = "0", inclusive = false, message = "Cân nặng phải lớn hơn 0.")
    @Digits(integer = 5, fraction = 2, message = "Cân nặng tối đa 99999,99 kg, hai chữ số thập phân.") private BigDecimal weight;
    @Size(max = 100, message = "Tên bác sĩ tối đa 100 ký tự.") private String vetName;
}
