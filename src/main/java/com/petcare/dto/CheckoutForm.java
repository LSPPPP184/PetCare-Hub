package com.petcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutForm {
    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng.")
    @Size(max = 1000, message = "Địa chỉ giao hàng tối đa 1000 ký tự.")
    private String shippingAddress;
}
