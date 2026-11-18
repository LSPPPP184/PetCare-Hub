package com.petcare.service;

import com.petcare.config.LoyaltyProperties;
import com.petcare.config.VaccinationProperties;
import com.petcare.domain.appointment.AppointmentStatus;
import com.petcare.domain.loyalty.MembershipTier;
import com.petcare.domain.order.OrderStatus;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.dto.DashboardView;
import com.petcare.repository.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Tổng hợp tại repository; bổ sung tháng/hạng chưa có dữ liệu để bảng luôn đầy đủ. */
@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final OrderRepository orders;
    private final AppointmentRepository appointments;
    private final VaccinationRecordRepository vaccinations;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyProperties loyalty;
    private final VaccinationProperties vaccinationProperties;
    private final Clock clock;
    private final Clock healthClock;

    public DashboardService(OrderRepository orders, AppointmentRepository appointments,
            VaccinationRecordRepository vaccinations, LoyaltyAccountRepository accounts,
            LoyaltyProperties loyalty, VaccinationProperties vaccinationProperties,
            Clock clock, @Qualifier("healthClock") Clock healthClock) {
        this.orders = orders; this.appointments = appointments; this.vaccinations = vaccinations;
        this.accounts = accounts; this.loyalty = loyalty; this.vaccinationProperties = vaccinationProperties;
        this.clock = clock; this.healthClock = healthClock;
    }

    /** Nhân viên xử lý toàn cửa hàng; chỉ trả số liệu công việc, không truy vấn doanh thu/thành viên.
     * Đơn chờ xử lý là PENDING; mũi tiêm đếm theo bản ghi chưa hoàn tất và ngày sức khỏe.
     */
    public record WorkOverview(long pendingOrders, long appointmentsToday, long dueSoon, long overdue) {}
    public WorkOverview workOverview() {
        var today = LocalDate.now(clock);
        var healthDate = LocalDate.now(healthClock);
        return new WorkOverview(orders.countByStateName(OrderStatus.PENDING),
            appointments.countByStartTimeGreaterThanEqualAndStartTimeLessThanAndStateNameNot(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), AppointmentStatus.CANCELLED),
            vaccinations.countByStateNameNotAndScheduledDateBetween(VaccinationStatus.COMPLETED,
                healthDate, healthDate.plusDays(vaccinationProperties.getDueSoonDays())),
            vaccinations.countByStateNameNotAndScheduledDateBefore(VaccinationStatus.COMPLETED, healthDate));
    }
    public DashboardView overview() {
        var today = LocalDate.now(clock);
        var healthDate = LocalDate.now(healthClock);
        var currentMonth = YearMonth.from(today);
        var firstMonth = currentMonth.minusMonths(11);
        var totals = new HashMap<YearMonth, BigDecimal>();
        orders.monthlyRevenue(OrderStatus.COMPLETED, firstMonth.atDay(1).atStartOfDay(),
            currentMonth.plusMonths(1).atDay(1).atStartOfDay()).forEach(row ->
                totals.put(YearMonth.of(row.getYear(), row.getMonth()), row.getAmount()));
        var revenue = new ArrayList<DashboardView.Revenue>();
        for (int i = 0; i < 12; i++) {
            var month = firstMonth.plusMonths(i);
            revenue.add(new DashboardView.Revenue(month, totals.getOrDefault(month, BigDecimal.ZERO)));
        }
        var members = new ArrayList<DashboardView.Members>();
        var tiers = MembershipTier.values();
        for (int i = 0; i < tiers.length; i++) {
            var tier = loyalty.tier(tiers[i]);
            Long upper = i + 1 < tiers.length ? loyalty.tier(tiers[i + 1]).getThreshold() : null;
            members.add(new DashboardView.Members(tier.getName(), accounts.countMembers(tier.getThreshold(), upper)));
        }
        var bestSellers = orders.bestSellers(OrderStatus.COMPLETED, PageRequest.of(0, 5)).stream()
            .map(row -> new DashboardView.BestSeller(row.getProductId(), row.getName(), row.getQuantity())).toList();
        return new DashboardView(today, healthDate,
            appointments.countByStartTimeGreaterThanEqualAndStartTimeLessThanAndStateNameNot(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), AppointmentStatus.CANCELLED),
            vaccinations.countPetsDue(VaccinationStatus.COMPLETED, healthDate,
                healthDate.plusDays(vaccinationProperties.getDueSoonDays())),
            vaccinations.countPetsOverdue(VaccinationStatus.COMPLETED, healthDate),
            List.copyOf(revenue), bestSellers, List.copyOf(members));
    }
}
