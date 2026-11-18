package com.petcare.dto;
/** Tóm tắt lịch sử, không tải đáp án hoặc nội dung gợi ý. */
public record QuizSummary(Long id, String strategyLabel, java.time.LocalDateTime createdAt, long matchCount) {}
