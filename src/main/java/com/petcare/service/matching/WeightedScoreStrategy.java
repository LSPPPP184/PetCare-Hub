package com.petcare.service.matching;

import com.petcare.config.MatchingProperties;
import org.springframework.stereotype.Component;

/** Strategy chấm điểm cân bằng theo trọng số cấu hình, dễ giải thích bằng từng tiêu chí. */
@Component
public class WeightedScoreStrategy extends AbstractScoreStrategy {
    public WeightedScoreStrategy(MatchingProperties properties) { super(properties); }
    @Override protected double weight(MatchCriterion criterion) { return properties.getWeights().get(criterion); }
    @Override public String key() { return "weighted"; }
    @Override public String label() { return "Cân bằng các tiêu chí"; }
}
