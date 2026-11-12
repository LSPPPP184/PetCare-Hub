package com.petcare.domain.appointment;

import com.petcare.domain.*;
import org.hibernate.Hibernate;
import java.util.Map;
import java.util.function.Function;

/** Registry dịch vụ đặt lịch, không phân nhánh theo lớp sản phẩm ở service/controller. */
public final class AppointmentServices {
    private AppointmentServices() {}
    private static final Map<ProductType, Function<Product, ServiceItem>> TYPES = Map.of(
        ProductType.GROOMING, p -> GroomingService.class.cast(Hibernate.unproxy(p)),
        ProductType.VET, p -> VetService.class.cast(Hibernate.unproxy(p)));
    public static boolean supports(Product product) { return product.isActive() && TYPES.containsKey(product.getType()); }
    public static ServiceItem require(Product product) {
        var converter = TYPES.get(product.getType());
        if (converter == null || !product.isActive())
            throw new AppointmentException("Chỉ có thể đặt lịch dịch vụ tắm tỉa hoặc thú y đang hiển thị.");
        return converter.apply(product);
    }
}
