package com.petcare.dto;

import com.petcare.domain.Pet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

public record MatchResult(Pet pet, double scorePercent, List<String> reasons, List<MatchComponent> components) {
    public MatchResult {
        if (!Double.isFinite(scorePercent) || scorePercent < 0 || scorePercent > 100)
            throw new IllegalArgumentException("Điểm ghép đôi phải hữu hạn và nằm trong khoảng 0 đến 100%.");
        reasons = List.copyOf(reasons); components = List.copyOf(components);
    }
    public String formula() {
        double totalPercent = components.stream().mapToDouble(MatchComponent::normalizedWeight).sum() * 100;
        var displayedScore = BigDecimal.valueOf(scorePercent).setScale(2, RoundingMode.HALF_UP);
        var displayedSum = components.stream().map(MatchComponent::displayedPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
        var correction = displayedScore.subtract(displayedSum);
        String terms = components.stream().map(c -> c.displayedPoints().toString()).collect(Collectors.joining(" + "));
        String rounding = correction.signum() == 0 ? "" : " " + (correction.signum() > 0 ? "+ " : "− ")
            + correction.abs().stripTrailingZeros() + " (hiệu chỉnh làm tròn)";
        return "Điểm = 100 × Σ(độ khớp × tỷ lệ trọng số). Tổng trọng số chuẩn hóa = "
            + MatchComponent.formatNumber(totalPercent) + "%.\nCộng điểm thành phần: "
            + terms + rounding + " = " + displayedScore + "%.";
    }
}
