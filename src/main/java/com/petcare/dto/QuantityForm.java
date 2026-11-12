package com.petcare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QuantityForm {
    @NotNull(message = "Vui lòng nhập số lượng.")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0.")
    private Integer quantity;
}
