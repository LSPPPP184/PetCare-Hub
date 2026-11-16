package com.petcare.service.matching;

import com.petcare.domain.PetProfile;
import com.petcare.dto.QuizAnswers;
import java.util.function.ToDoubleBiFunction;

/** Mỗi tiêu chí tự tính độ khớp; không phân nhánh theo loại thú cưng. */
public enum MatchCriterion {
    SIZE("Kích thước", (a, p) -> capacity(a.getHomeSize().getCapacity(), p.getSizeLevel())),
    SPACE("Không gian", (a, p) -> capacity(a.getHomeSize().getCapacity(), p.getSpaceNeed())),
    ACTIVITY("Vận động", (a, p) -> 1 - Math.abs(a.getActivityLevel() - p.getEnergyLevel()) / 4.0),
    TIME("Thời gian", (a, p) -> Math.min(1, a.getFreeHoursPerDay() / p.getTimeNeedHoursPerDay())),
    BUDGET("Ngân sách", (a, p) -> capacity(a.getBudgetLevel(), p.getCareCostLevel())),
    EXPERIENCE("Kinh nghiệm", (a, p) -> capacity(a.getExperienceLevel(), p.getExperienceNeeded())),
    SHEDDING("Rụng lông", (a, p) -> a.getFurAllergy() ? 1 - (p.getSheddingLevel() - 1) / 4.0 : 1);

    private final String label;
    private final ToDoubleBiFunction<QuizAnswers, PetProfile> fit;
    MatchCriterion(String label, ToDoubleBiFunction<QuizAnswers, PetProfile> fit) { this.label = label; this.fit = fit; }
    public String getLabel() { return label; }
    public double fit(QuizAnswers answers, PetProfile profile) { return fit.applyAsDouble(answers, profile); }
    private static double capacity(int available, int needed) { return 1 - Math.max(0, needed - available) / 4.0; }
}
