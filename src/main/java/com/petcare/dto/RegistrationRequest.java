package com.petcare.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 254, message = "Email tối đa 254 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 72, message = "Mật khẩu cần từ 8 đến 72 ký tự.")
    @Pattern(regexp = "[\\x20-\\x7E]*", message = "Mật khẩu chỉ dùng ký tự ASCII có thể in.")
    private String password;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    @Pattern(regexp = "^$|(?=(?:[^0-9]*[0-9]){8,20}[^0-9]*$)\\+?[0-9 ()-]+",
        message = "Số điện thoại không hợp lệ.")
    private String phone;

    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.strip().replaceAll("\\s+", " ");
    }
}
