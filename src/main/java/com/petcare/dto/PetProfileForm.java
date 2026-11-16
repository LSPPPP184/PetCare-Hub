package com.petcare.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PetProfileForm {
    private Long version;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer sizeLevel = 3;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer energyLevel = 3;
    private boolean childFriendly;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer sheddingLevel = 3;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer spaceNeed = 3;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer careCostLevel = 3;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer experienceNeeded = 3;
    @NotNull(message = "Vui lòng nhập đầy đủ thông tin.") @DecimalMin(value = "0.25", message = "Thời gian cần từ 0,25 đến 24 giờ.") @DecimalMax(value = "24", message = "Thời gian không quá 24 giờ.")
    private Double timeNeedHoursPerDay = 2.0;
}
