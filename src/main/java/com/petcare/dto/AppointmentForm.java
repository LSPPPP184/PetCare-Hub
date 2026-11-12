package com.petcare.dto;

import com.petcare.domain.Species;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Getter @Setter
public class AppointmentForm {
    @NotNull(message = "Vui lòng chọn dịch vụ.") @Positive(message = "Dịch vụ không hợp lệ.") private Long serviceId;
    @NotNull(message = "Vui lòng chọn nhân viên.") @Positive(message = "Nhân viên không hợp lệ.") private Long staffId;
    @NotNull(message = "Vui lòng chọn giờ hẹn.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startTime;
    @NotBlank(message = "Vui lòng nhập tên thú cưng.") @Size(max = 100, message = "Tên thú cưng tối đa 100 ký tự.") private String petName;
    @NotNull(message = "Vui lòng chọn loài thú cưng.") private Species petSpecies;
    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.") private String note;
}
