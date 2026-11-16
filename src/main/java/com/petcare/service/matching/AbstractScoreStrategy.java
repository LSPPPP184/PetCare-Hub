package com.petcare.service.matching;

import com.petcare.config.MatchingProperties;
import com.petcare.domain.*;
import com.petcare.dto.*;
import java.util.*;

/** Phần chung của hai Strategy: lọc cứng, chuẩn hóa, giải thích và xếp hạng ổn định. */
public abstract class AbstractScoreStrategy implements MatchingStrategy {
    protected final MatchingProperties properties;
    protected AbstractScoreStrategy(MatchingProperties properties) { this.properties = properties; }
    protected abstract double weight(MatchCriterion criterion);

    @Override public List<MatchResult> match(QuizAnswers answers, List<Pet> candidates) {
        properties.validate();
        return candidates.stream().filter(p -> eligible(answers, p)).map(p -> score(answers, p))
            .sorted(Comparator.comparingDouble(MatchResult::scorePercent).reversed()
                .thenComparing(r -> r.pet().getId(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.pet().getName()))
            .toList();
    }

    private boolean eligible(QuizAnswers a, Pet p) {
        var f = p.getProfile();
        return p.isActive() && p.getStatus() == PetStatus.AVAILABLE && f != null
            && (!a.getHasChildren() || f.isChildFriendly())
            && (!a.getFurAllergy() || f.getSheddingLevel() <= 2)
            && (a.getHomeSize() != HomeSize.SMALL || f.getSpaceNeed() <= 2);
    }

    private MatchResult score(QuizAnswers a, Pet pet) {
        var profile = pet.getProfile();
        double total = Arrays.stream(MatchCriterion.values()).mapToDouble(this::weight).sum();
        var components = Arrays.stream(MatchCriterion.values()).map(c -> {
            double fit = c.fit(a, profile);
            // Chia trước khi nhân để tránh tràn số với trọng số lớn hoặc mất độ chính xác với trọng số nhỏ.
            double normalizedWeight = weight(c) / total;
            return new MatchComponent(c.getLabel(), fit, normalizedWeight, (normalizedWeight * 100) * fit);
        }).toList();
        var reasons = new ArrayList<String>();
        if (a.getHasChildren()) reasons.add("Đạt điều kiện gia đình có trẻ em: hồ sơ ghi nhận thân thiện với trẻ.");
        if (a.getFurAllergy()) reasons.add("Đạt ngưỡng rụng lông thấp (mức 1–2); đây không phải bảo đảm không gây dị ứng.");
        reasons.add("Nhà " + a.getHomeSize().getLabel().toLowerCase(Locale.ROOT)
            + "; nhu cầu không gian " + profile.getSpaceNeed() + "/5; kích thước " + profile.getSizeLevel() + "/5.");
        reasons.add("Vận động của bạn " + a.getActivityLevel() + "/5 so với năng lượng thú cưng " + profile.getEnergyLevel() + "/5.");
        reasons.add("Bạn rảnh " + a.getFreeHoursPerDay() + " giờ/ngày; thú cưng cần " + profile.getTimeNeedHoursPerDay() + " giờ/ngày.");
        reasons.add("Ngân sách " + a.getBudgetLevel() + "/5 so với chi phí chăm sóc " + profile.getCareCostLevel() + "/5.");
        reasons.add("Kinh nghiệm " + a.getExperienceLevel() + "/5 so với yêu cầu " + profile.getExperienceNeeded() + "/5.");
        double score = components.stream().mapToDouble(MatchComponent::points).sum();
        // Chỉ khử sai số cộng dấu phẩy động sát 100; điểm sai thực sự bị MatchResult từ chối.
        if (score > 100 && score <= 100 + components.size() * Math.ulp(100.0)) score = 100;
        return new MatchResult(pet, score, reasons, components);
    }
}
