package com.petcare.dto;

/** Thông tin xác nhận do service quyết định ưu tiên gói chăm sóc hoặc giảm giá thành viên. */
public record AppointmentConfirmation(AppointmentSelection selection, SubscriptionPreview subscriptionPreview,
                                      LoyaltyQuote quote) {}
