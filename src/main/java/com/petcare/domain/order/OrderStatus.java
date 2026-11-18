package com.petcare.domain.order;

/** Mã lưu trữ cho pattern State; hành vi được ủy quyền cho từng lớp trạng thái. */
public enum OrderStatus { PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED }
