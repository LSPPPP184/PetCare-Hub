package com.petcare.domain.discount;

import java.math.BigDecimal;

/** Strategy: mỗi hạng cung cấp chính sách giảm giá, bên đặt hàng không rẽ nhánh theo hạng. */
public interface DiscountStrategy {
    BigDecimal discountRate();
    /** Trả về số tiền phải trả sau giảm giá, làm tròn đến hai chữ số thập phân. */
    BigDecimal apply(BigDecimal amount);
}
