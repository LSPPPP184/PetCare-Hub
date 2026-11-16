package com.petcare.domain;

import com.petcare.dto.MatchResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.stream.Collectors;

/** Bản chụp kết quả giữ nguyên giải thích khi hồ sơ hoặc trọng số thay đổi. */
@Embeddable @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizMatchSnapshot {
    private Long petId;
    @Column(length = 255, nullable = false) private String petName;
    private double scorePercent;
    @Column(length = 2000, nullable = false) private String reasons;
    @Column(length = 3000, nullable = false) private String explanation;

    public QuizMatchSnapshot(MatchResult result) {
        petId = result.pet().getId(); petName = result.pet().getName(); scorePercent = result.scorePercent();
        reasons = String.join("\n", result.reasons());
        explanation = result.formula() + "\n" + result.components().stream()
            .map(c -> c.explanation()).collect(Collectors.joining("\n"));
    }
}
