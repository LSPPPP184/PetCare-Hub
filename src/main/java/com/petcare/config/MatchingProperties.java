package com.petcare.config;

import com.petcare.service.matching.MatchCriterion;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component @ConfigurationProperties(prefix = "petcare.matching") @Getter @Setter
public class MatchingProperties {
    private String defaultStrategy = "weighted";
    private double budgetMultiplier = 3;
    private Map<MatchCriterion, Double> weights = new EnumMap<>(Map.of(
        MatchCriterion.SIZE, 10.0, MatchCriterion.SPACE, 15.0, MatchCriterion.ACTIVITY, 20.0,
        MatchCriterion.TIME, 20.0, MatchCriterion.BUDGET, 20.0,
        MatchCriterion.EXPERIENCE, 10.0, MatchCriterion.SHEDDING, 5.0));

    @PostConstruct public void validate() {
        if (!Set.of("weighted", "budget-first").contains(defaultStrategy)
            || !Double.isFinite(budgetMultiplier) || budgetMultiplier <= 1
            || weights == null || weights.size() != MatchCriterion.values().length
            || weights.values().stream().anyMatch(w -> w == null || !Double.isFinite(w) || w <= 0))
            throw new IllegalArgumentException("Cấu hình ghép đôi không hợp lệ: đủ trọng số dương và hệ số ngân sách lớn hơn 1.");
        validateTotal(weights.values().stream().mapToDouble(Double::doubleValue).sum());
        validateTotal(weights.entrySet().stream().mapToDouble(e ->
            e.getValue() * (e.getKey() == MatchCriterion.BUDGET ? budgetMultiplier : 1)).sum());
    }

    private void validateTotal(double total) {
        if (!Double.isFinite(total) || total <= 0)
            throw new IllegalArgumentException("Tổng trọng số của mỗi chiến lược phải hữu hạn và lớn hơn 0.");
    }
}
