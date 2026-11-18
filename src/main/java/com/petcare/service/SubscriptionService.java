package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.domain.subscription.*;
import com.petcare.domain.order.*;
import com.petcare.domain.purchase.PurchaseException;
import com.petcare.dto.SubscriptionView;
import com.petcare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class SubscriptionService {
    private final SubscriptionRepository subscriptions;
    private final SubscriptionGrantRepository grants;
    private final OrderRepository orders;
    private final CommerceLocks locks;
    private final CommerceAccess access;
    private final LoyaltyPricingService pricing;
    private final Clock clock;

    /**
     * Khóa đơn trước kiểm tra/cấp gói, sourceKey UNIQUE theo dòng và đơn vị bảo vệ phát lại đồng thời.
     * Đơn cũ có nhiều loại gói vẫn cấp đủ từng đơn vị; giới hạn một loại chỉ áp dụng lúc tạo đơn mới.
     * Khóa ORDER:id:ITEM:itemId (thêm :UNIT:n khi quantity > 1) nhận diện quyền lợi mua mới.
     * Biên nhận chỉ ghi một lần sau cả lô, cùng giao dịch hoàn tất đơn và cộng điểm.
     * Giả định dữ liệu phiên bản trước được ghi nguyên tử: biên nhận cũ hoặc sourceKey ORDER:id
     * nghĩa là toàn bộ quyền lợi của đơn đã cấp (có thể gộp nhiều suất). Giữ nguyên và không cấp lại,
     * không xóa/chia lại Subscription cũ đang được lịch hẹn tham chiếu. Không hỗ trợ dữ liệu cũ bị sửa tay dở dang.
     * Dòng chưa có snapshot dùng cấu hình hiện hành; gia hạn cập nhật gói đích từ snapshot của dòng gia hạn.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void grantOrder(Long orderId) {
        var order = locks.lock(Order.class, orderId);
        if (order.getStateName() != OrderStatus.COMPLETED) throw new PurchaseException("Đơn hàng chưa hoàn tất.");
        if (grants.existsById(orderId) || subscriptions.existsBySourceKey("ORDER:" + orderId)) return;
        locks.lock(User.class, order.getCustomer().getId());
        Subscription firstGranted = null;
        var today = LocalDate.now(clock);
        for (var item : order.getItems()) {
            var benefits = item.subscriptionBenefits();
            if (benefits.isEmpty()) continue;
            if (order.getRenewalSubscription() != null) {
                var subscription = locks.lock(Subscription.class, order.getRenewalSubscription().getId());
                subscription.renew(today, item.getQuantity(), benefits.get());
                firstGranted = subscription;
            } else {
                for (int unit = 0; unit < item.getQuantity(); unit++) {
                    var key = "ORDER:" + orderId + ":ITEM:" + item.getId()
                        + (item.getQuantity() > 1 ? ":UNIT:" + (unit + 1) : "");
                    var subscription = subscriptions.findBySourceKey(key).orElseGet(() -> subscriptions.save(
                        Subscription.createFromBenefits(order.getCustomer(), null, benefits.get(), today, 1, key)));
                    if (firstGranted == null) firstGranted = subscription;
                }
            }
        }
        if (firstGranted != null) grants.saveAndFlush(new SubscriptionGrant(order, firstGranted));
    }
    /**
     * Chọn gói chung (ownedPet trống), còn hạn hôm nay và ngày hẹn, chứa đúng ID dịch vụ;
     * ưu tiên endDate sớm nhất rồi ID nhỏ nhất. Lịch cũ chỉ có tên thú nên không dùng gói gắn riêng hồ sơ.
     * Khóa khách trước mọi thao tác; khóa từng gói và đọc lại trước trừ, trong giao dịch tạo lịch.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Subscription> useOnce(User customer, ServiceItem service, LocalDate appointmentDate) {
        locks.lock(User.class, customer.getId());
        var today = LocalDate.now(clock);
        for (var candidate : subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customer.getId())) {
            var s = locks.lock(Subscription.class, candidate.getId());
            if (s.getStateName() == SubscriptionStatus.ACTIVE && s.isDue(today)) s.expire(today);
            if (appliesTo(s, service.getId(), appointmentDate, today)) {
                s.useOnce(today); return Optional.of(s);
            }
        }
        return Optional.empty();
    }
    private boolean appliesTo(Subscription s, Long serviceId, LocalDate appointmentDate, LocalDate today) {
        return s.getOwnedPet() == null && s.isUsableOn(today) && s.isUsableOn(appointmentDate) && s.covers(serviceId);
    }
    /** Cùng điều kiện/thứ tự với useOnce: hạn sớm nhất rồi ID; không khóa, đổi trạng thái hay trừ lượt. */
    @Transactional(readOnly = true)
    public Optional<com.petcare.dto.SubscriptionPreview> preview(Long serviceId, LocalDate appointmentDate) {
        var today = LocalDate.now(clock);
        return subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(access.currentCustomer().getId()).stream()
            .filter(s -> appliesTo(s, serviceId, appointmentDate, today)).findFirst()
            .map(s -> new com.petcare.dto.SubscriptionPreview(s.getId(), s.getRemainingUses()));
    }
    /** Lịch đã hủy chỉ hoàn một lần nhờ cờ lưu trên lịch và khóa lịch của AppointmentService. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreUse(Appointment appointment) {
        if (appointment.getSubscription() == null || appointment.isSubscriptionUseRefunded()) return;
        if (appointment.getStateName() != com.petcare.domain.appointment.AppointmentStatus.CANCELLED)
            throw new PurchaseException("Chỉ hoàn lượt khi lịch hẹn đã hủy.");
        locks.lock(Subscription.class, appointment.getSubscription().getId()).restoreUse(LocalDate.now(clock));
        appointment.markSubscriptionUseRefunded();
    }
    private Subscription ownedLocked(Long id) {
        var customer = access.currentCustomer();
        locks.lock(User.class, customer.getId());
        var s = locks.lock(Subscription.class, id);
        access.customer(s.getCustomer().getId());
        return s;
    }
    public void cancel(Long id) {
        var s = ownedLocked(id);
        if (s.isRenewalPending()) throw new PurchaseException("Vui lòng xử lý đơn gia hạn đang chờ trước khi hủy gói.");
        s.cancel();
    }
    /** Cùng thứ tự khóa đơn -> khách -> gói với hoàn tất đơn; hủy đơn mở lại quyền yêu cầu gia hạn. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseRenewal(Order order) {
        if (order.getRenewalSubscription() == null) return;
        locks.lock(User.class, order.getCustomer().getId());
        locks.lock(Subscription.class, order.getRenewalSubscription().getId()).releaseRenewal();
    }
    /** Gia hạn tạo đơn mô phỏng riêng, giữ nguyên giỏ; quyền lợi chỉ cộng khi nhân viên hoàn tất đơn. */
    public Long renew(Long id) {
        var s = ownedLocked(id);
        s.requestRenewal(LocalDate.now(clock));
        var pack = locks.lock(Product.class, s.getCarePackage().getId());
        var cart = new Cart(s.getCustomer()); cart.add(pack, 1);
        var order = Order.fromCart(cart, "Gia hạn gói chăm sóc — thanh toán mô phỏng");
        order.renewSubscription(s);
        order.snapshotDiscount(pricing.snapshot(s.getCustomer().getId(), order.getTotalAmount(), true).discountAmount());
        return orders.save(order).getId();
    }
    @Transactional(readOnly = true)
    public List<SubscriptionView> mine() { return mine(org.springframework.data.domain.Pageable.unpaged()).getContent(); }
    public org.springframework.data.domain.Page<SubscriptionView> mine(org.springframework.data.domain.Pageable pageable) {
        return list(access.currentCustomer().getId(), false, pageable);
    }
    @Transactional(readOnly = true)
    public List<SubscriptionView> active() { return active(org.springframework.data.domain.Pageable.unpaged()).getContent(); }
    public org.springframework.data.domain.Page<SubscriptionView> active(org.springframework.data.domain.Pageable pageable) {
        if (access.currentStaff().getRole() != Role.ADMIN) throw new AccessDeniedException("Chỉ quản trị viên được xem các gói đang hoạt động.");
        return list(null, true, pageable);
    }
    private org.springframework.data.domain.Page<SubscriptionView> list(Long customerId, boolean activeOnly, org.springframework.data.domain.Pageable pageable) {
        var today = LocalDate.now(clock);
        var ids = subscriptions.findListIds(customerId, activeOnly, today, pageable);
        var rows = ids.isEmpty() ? List.<SubscriptionView>of() : subscriptions.fetchList(ids.getContent()).stream()
            .map(s -> SubscriptionView.from(s, today)).toList();
        return new org.springframework.data.domain.PageImpl<>(rows, pageable, ids.getTotalElements());
    }
    /** Mỗi gói một giao dịch ngắn; đọc lại dưới khóa để không ghi đè gia hạn hoặc hoàn lượt. */
    public void expireIfDue(Long id, LocalDate today) {
        var s = locks.lock(Subscription.class, id);
        if (s.getStateName() == SubscriptionStatus.ACTIVE && s.isDue(today)) s.expire(today);
    }
}
