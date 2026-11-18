package com.petcare.dto;

/** Dự tính chỉ đọc; quyền dùng lượt được kiểm tra lại dưới khóa khi xác nhận lịch. */
public record SubscriptionPreview(Long id, int remainingUses) {}
