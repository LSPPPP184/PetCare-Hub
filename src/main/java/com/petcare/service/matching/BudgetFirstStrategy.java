package com.petcare.service.matching;

import com.petcare.config.MatchingProperties;
import org.springframework.stereotype.Component;

/** Strategy ưu tiên chi phí: tăng trọng số ngân sách, giữ nguyên lọc cứng và cách chuẩn hóa. */
@Component
public class BudgetFirstStrategy extends AbstractScoreStrategy {
    public BudgetFirstStrategy(MatchingProperties properties) { super(properties); }
    @Override protected double weight(MatchCriterion criterion) {
        return properties.getWeights().get(criterion) * (criterion == MatchCriterion.BUDGET ? properties.getBudgetMultiplier() : 1);
    }
    @Override public String key() { return "budget-first"; }
    @Override public String label() { return "Ưu tiên ngân sách"; }
}
