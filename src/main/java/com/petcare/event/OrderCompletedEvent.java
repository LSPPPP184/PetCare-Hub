package com.petcare.event;

/** Observer đồng bộ trong giao dịch hoàn tất; lỗi tích điểm rollback cả trạng thái đơn để không mất điểm. */
public record OrderCompletedEvent(Long orderId) {}
