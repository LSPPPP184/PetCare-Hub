package com.petcare.dto;

import com.petcare.domain.QuizResult;
import java.time.LocalDateTime;
import java.util.List;

/** DTO độc lập persistence context, dùng được khi open-in-view bị tắt và lưu phiên khách. */
public record QuizReport(Long id, String strategyLabel, LocalDateTime createdAt, QuizAnswers answers, List<Entry> matches) {
    public record Entry(Long petId, String petName, double scorePercent, List<String> reasons, String explanation) {}
    public static QuizReport from(QuizResult result) {
        return new QuizReport(result.getId(), result.getStrategyLabel(), result.getCreatedAt(), result.getAnswers().copy(),
            result.getMatches().stream().map(m -> new Entry(m.getPetId(), m.getPetName(), m.getScorePercent(),
                List.of(m.getReasons().split("\n")), m.getExplanation())).toList());
    }
}
