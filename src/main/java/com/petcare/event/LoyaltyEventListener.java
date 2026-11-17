package com.petcare.event;

import com.petcare.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Observer chạy đồng bộ cùng giao dịch phát sự kiện; MANDATORY từ chối phát ngoài giao dịch.
 * Không dùng AFTER_COMMIT vì lỗi cộng điểm phải rollback cả việc hoàn tất đơn/lịch hẹn.
 */
@Component @RequiredArgsConstructor
public class LoyaltyEventListener {
    private final LoyaltyService loyalty;
    @EventListener @Transactional(propagation = Propagation.MANDATORY)
    public void onOrder(OrderCompletedEvent event) { loyalty.earnOrder(event.orderId()); }
    @EventListener @Transactional(propagation = Propagation.MANDATORY)
    public void onAppointment(AppointmentCompletedEvent event) { loyalty.earnAppointment(event.appointmentId()); }
}
