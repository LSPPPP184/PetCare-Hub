package com.petcare.dto;

import com.petcare.domain.Species;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter
public class OwnedPetForm {
    @NotBlank(message = "Vui lòng nhập tên thú cưng.") @Size(max = 100, message = "Tên tối đa 100 ký tự.") private String name;
    @NotNull(message = "Vui lòng chọn loài.") private Species species;
    @NotNull(message = "Vui lòng nhập ngày sinh.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate birthDate;
    @Size(max = 100, message = "Giống tối đa 100 ký tự.") private String breed;
    @Size(max = 30, message = "Giới tính tối đa 30 ký tự.") private String gender;
    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự.") private String note;
    @NotNull(message = "Vui lòng tải lại hồ sơ.") @Min(value = 0, message = "Phiên bản hồ sơ không hợp lệ.") private Long version = 0L;
}
