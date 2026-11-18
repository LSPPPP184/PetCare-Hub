package com.petcare.domain.subscription;

import com.petcare.domain.ServiceItem;
import com.petcare.domain.appointment.AppointmentServices;
import com.petcare.domain.purchase.PurchaseException;
import java.util.Collection;

/** Gói chỉ được bán/cấp khi còn ít nhất một dịch vụ đặt lịch đang hiển thị. */
public final class PackageCoverage {
    private PackageCoverage() {}
    public static boolean isValid(Collection<ServiceItem> services) {
        return services != null && services.stream().anyMatch(AppointmentServices::supports);
    }
    public static void requireValid(Collection<ServiceItem> services) {
        if (!isValid(services))
            throw new PurchaseException("Gói tạm ngừng bán: cần cấu hình ít nhất một dịch vụ áp dụng hợp lệ.");
    }
}
