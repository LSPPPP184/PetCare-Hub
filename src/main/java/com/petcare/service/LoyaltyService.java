package com.petcare.service;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.*;
import com.petcare.domain.loyalty.*;
import com.petcare.domain.order.OrderStatus;
import com.petcare.domain.appointment.AppointmentStatus;
import com.petcare.dto.LoyaltyView;
import com.petcare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class LoyaltyService {
    private final LoyaltyAccounts accounts;
    private final LoyaltyAccountRepository repository;
    private final PointTransactionRepository transactions;
    private final OrderRepository orders;
    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final LoyaltyProperties properties;
    private final CommerceAccess access;
    private final Clock clock;

    /** Khóa tài khoản trước kiểm tra nguồn và ghi điểm; sourceKey UNIQUE là lớp bảo vệ thứ hai. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void earnOrder(Long id) {
        var order = orders.findById(id).orElseThrow();
        if (order.getStateName() != OrderStatus.COMPLETED) throw new LoyaltyException("Đơn hàng chưa hoàn tất.");
        earn(order.getCustomer().getId(), order.getFinalAmount(), "EARN:ORDER:" + id, "Tích điểm đơn hàng #" + id, order, null);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void earnAppointment(Long id) {
        var appointment = appointments.findById(id).orElseThrow();
        if (appointment.getStateName() != AppointmentStatus.DONE) throw new LoyaltyException("Lịch hẹn chưa hoàn tất.");
        earn(appointment.getCustomer().getId(), appointment.getFinalAmount(), "EARN:APPOINTMENT:" + id,
            "Tích điểm lịch hẹn #" + id, null, appointment);
    }
    private void earn(Long customerId, BigDecimal amount, String key, String description, Order order, Appointment appointment) {
        var account = accounts.locked(customerId);
        if (transactions.existsBySourceKey(key)) return;
        long points = amount.divideToIntegralValue(new BigDecimal("10000")).longValueExact();
        account.credit(points, properties);
        transactions.save(new PointTransaction(account, PointTransactionType.EARN, points, description, order, appointment,
            LocalDateTime.now(clock), key));
    }
    public LoyaltyView mine() { return view(accounts.open(access.currentCustomer().getId()), true); }
    public void redeem() {
        var account = accounts.locked(access.currentCustomer().getId());
        account.redeem();
        record(account, PointTransactionType.REDEEM, -100, "Đổi 100 điểm lấy voucher 10.000đ cho đơn kế tiếp");
    }
    private void admin() {
        if (access.currentStaff().getRole() != Role.ADMIN) throw new AccessDeniedException("Chỉ quản trị viên được quản lý điểm.");
    }
    /**
     * Danh sách chỉ đọc thẻ đã mở, không tạo hàng loạt hay đồng bộ cột hạng bằng ghi DB.
     * Lọc theo khoảng totalPoints của cấu hình hiện hành, nên hạng mới có hiệu lực ngay,
     * kể cả khi cột tier còn là giá trị của giao dịch trước. Mỗi trang tối đa 20 khách.
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyView> members(MembershipTier tier, int page) {
        admin();
        var range = properties.rangeFor(tier);
        return repository.findMembers(range.lowerInclusive(), range.upperExclusive(), PageRequest.of(Math.max(0, page), 20))
            .map(a -> view(a, false));
    }
    @Transactional(readOnly = true)
    public LoyaltyView member(Long customerId) {
        admin();
        var customer = customer(customerId);
        return view(repository.findByCustomerId(customerId).orElseGet(() -> new LoyaltyAccount(customer)), true);
    }
    private User customer(Long id) {
        return users.findById(id).filter(u -> u.getRole() == Role.CUSTOMER)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng."));
    }
    public void adjust(Long customerId, long points, String reason) {
        admin();
        customer(customerId);
        if (points == 0 || reason == null || reason.isBlank() || reason.strip().length() > 800)
            throw new LoyaltyException("Nhập số điểm khác 0 và lý do từ 1 đến 800 ký tự.");
        var account = accounts.locked(customerId);
        account.adjust(points, properties);
        record(account, PointTransactionType.ADJUST, points, "Quản trị " + access.currentStaff().getEmail() + ": " + reason.strip());
    }
    private void record(LoyaltyAccount account, PointTransactionType type, long points, String description) {
        transactions.save(new PointTransaction(account, type, points, description, null, null,
            LocalDateTime.now(clock), type + ":" + UUID.randomUUID()));
    }
    public List<LoyaltyView.Benefit> benefits() {
        return Arrays.stream(MembershipTier.values()).map(t -> {
            var p = properties.tier(t);
            return new LoyaltyView.Benefit(p.getName(), p.getThreshold(), p.getRate().multiply(new BigDecimal("100")), p.getBenefits());
        }).toList();
    }
    private LoyaltyView view(LoyaltyAccount account, boolean history) {
        var tier = account.currentTier(properties);
        var next = Arrays.stream(MembershipTier.values()).filter(t -> t.ordinal() > tier.ordinal()).findFirst();
        long lower = properties.tier(tier).getThreshold();
        long remaining = next.map(t -> Math.max(0, properties.tier(t).getThreshold() - account.getTotalPoints())).orElse(0L);
        int progress = next.map(t -> (int) Math.min(100, Math.max(0, (account.getTotalPoints() - lower) * 100.0 /
            (properties.tier(t).getThreshold() - lower)))).orElse(100);
        var entries = history && account.getId() != null ? transactions.findByAccountIdOrderByCreatedAtDescIdDesc(account.getId()).stream()
            .map(t -> new LoyaltyView.Transaction(t.getType().getLabel(), t.getPoints(), t.getDescription(), t.getCreatedAt(),
                t.getRelatedOrder() == null ? null : t.getRelatedOrder().getId(),
                t.getRelatedAppointment() == null ? null : t.getRelatedAppointment().getId())).toList() : List.<LoyaltyView.Transaction>of();
        return new LoyaltyView(account.getCustomer().getId(), account.getCustomer().getFullName(), account.getCustomer().getEmail(),
            tier, properties.tier(tier).getName(), account.getTotalPoints(), account.getCurrentPoints(), account.isVoucherAvailable(),
            next.map(t -> properties.tier(t).getName()).orElse(null), remaining, progress, benefits(), entries);
    }
}
