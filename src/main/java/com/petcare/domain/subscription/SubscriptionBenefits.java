package com.petcare.domain.subscription;

import com.petcare.domain.MonthlyCarePackage;
import com.petcare.domain.ServiceItem;
import com.petcare.domain.purchase.PurchaseException;
import java.util.Set;

/**
 * Giá trị quyền lợi bất biến; giữ gói/dịch vụ theo ID, không đọc lại danh sách của gói khi cấp.
 * Giả định phải còn ít nhất một dịch vụ trong bản chụp đang nhận lịch; bản chụp rỗng bị từ chối.
 */
public record SubscriptionBenefits(MonthlyCarePackage carePackage, int includedUses, int durationMonths,
                                   Set<ServiceItem> coveredServices) {
    public SubscriptionBenefits {
        if (carePackage == null || includedUses <= 0 || durationMonths <= 0 || coveredServices == null)
            throw new PurchaseException("Thông tin quyền lợi gói chăm sóc không hợp lệ.");
        coveredServices = Set.copyOf(coveredServices);
        PackageCoverage.requireValid(coveredServices);
    }
    /** Dùng khi chốt đơn hoặc làm phương án tương thích cho dòng hàng cũ chưa có bản chụp. */
    public static SubscriptionBenefits capture(MonthlyCarePackage pack) {
        if (pack == null || pack.getIncludedUses() == null || pack.getDurationMonths() == null)
            throw new PurchaseException("Thông tin quyền lợi gói chăm sóc không hợp lệ.");
        return new SubscriptionBenefits(pack, pack.getIncludedUses(), pack.getDurationMonths(), pack.getCoveredServices());
    }
}
