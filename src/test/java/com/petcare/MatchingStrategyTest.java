package com.petcare;

import com.petcare.config.MatchingProperties;
import com.petcare.domain.*;
import com.petcare.dto.*;
import com.petcare.service.matching.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MatchingStrategyTest {
    private final MatchingProperties properties = new MatchingProperties();
    private final MatchingStrategy weighted = new WeightedScoreStrategy(properties);
    private final MatchingStrategy budget = new BudgetFirstStrategy(properties);

    static QuizAnswers answers(HomeSize home, boolean children, int activity, double hours, int cost, boolean allergy, int experience) {
        var a = new QuizAnswers();
        a.setHomeSize(home); a.setHasChildren(children); a.setActivityLevel(activity); a.setFreeHoursPerDay(hours);
        a.setBudgetLevel(cost); a.setFurAllergy(allergy); a.setExperienceLevel(experience);
        return a;
    }
    static Pet pet(long id, int size, int energy, boolean friendly, int shedding, int space, int cost, int experience, double hours) {
        var f = new ProductForm(); f.setName("Thú cưng " + id);
        f.setProfile(new PetProfileForm());
        var p = f.getProfile();
        p.setSizeLevel(size); p.setEnergyLevel(energy); p.setChildFriendly(friendly); p.setSheddingLevel(shedding);
        p.setSpaceNeed(space); p.setCareCostLevel(cost); p.setExperienceNeeded(experience); p.setTimeNeedHoursPerDay(hours);
        var pet = new Pet(); pet.update(f); ReflectionTestUtils.setField(pet, "id", id);
        return pet;
    }

    @Test void familyWithChildrenFiltersBeforeRanking() {
        var activeFriendly = pet(1, 3, 4, true, 4, 3, 3, 2, 2);
        var quietFriendly = pet(2, 2, 1, true, 3, 2, 2, 1, 1);
        var unsuitable = pet(3, 2, 4, false, 1, 2, 2, 1, 1);
        var a = answers(HomeSize.LARGE, true, 4, 3, 4, false, 4);
        for (var strategy : List.of(weighted, budget))
            assertThat(strategy.match(a, List.of(unsuitable, quietFriendly, activeFriendly)))
                .extracting(r -> r.pet().getId()).containsExactly(1L, 2L);
    }

    @Test void personInSmallApartmentGetsQuietSmallPetsFirst() {
        var quiet = pet(1, 1, 1, false, 1, 1, 1, 1, 0.5);
        var active = pet(2, 2, 5, false, 2, 2, 2, 2, 2);
        var needsSpace = pet(3, 3, 1, true, 1, 3, 1, 1, 0.5);
        var a = answers(HomeSize.SMALL, false, 1, 2, 2, false, 2);
        assertThat(weighted.match(a, List.of(active, needsSpace, quiet))).extracting(r -> r.pet().getId()).containsExactly(1L, 2L);
    }

    @Test void littleFreeTimePenalizesDemandingPetsAndZeroHoursIsFinite() {
        var easy = pet(1, 1, 2, true, 1, 1, 1, 1, 0.5);
        var demanding = pet(2, 1, 2, true, 1, 1, 1, 1, 4);
        var a = answers(HomeSize.MEDIUM, false, 2, 0.5, 3, false, 3);
        var result = weighted.match(a, List.of(demanding, easy));
        assertThat(result).extracting(r -> r.pet().getId()).containsExactly(1L, 2L);
        assertThat(result.getFirst().scorePercent()).isEqualTo(100);
        assertThat(result.getLast().scorePercent()).isEqualTo(82.5);
        a.setFreeHoursPerDay(0.0);
        assertThat(weighted.match(a, List.of(easy, demanding))).allSatisfy(r -> assertThat(r.scorePercent()).isEqualTo(80));
    }

    @Test void strategiesReverseRankingForSameAnswers() {
        var energeticExpensive = pet(1, 2, 5, true, 1, 2, 5, 1, 2);
        var quietEconomical = pet(2, 2, 1, true, 1, 2, 1, 1, 8);
        var a = answers(HomeSize.LARGE, false, 5, 2, 1, false, 5);
        var candidates = List.of(quietEconomical, energeticExpensive);
        assertThat(weighted.match(a, candidates)).extracting(r -> r.pet().getId()).containsExactly(1L, 2L);
        assertThat(budget.match(a, candidates)).extracting(r -> r.pet().getId()).containsExactly(2L, 1L);
        assertThat(weighted.match(a, candidates).getFirst().scorePercent()).isEqualTo(80);
        assertThat(budget.match(a, candidates).getFirst().scorePercent()).isCloseTo(75, within(0.00001));
    }

    @Test void hardFilterThresholdsAndUnavailableHiddenMissingProfiles() {
        var boundary = pet(1, 2, 3, true, 2, 2, 1, 1, 1);
        var shedding = pet(2, 2, 3, true, 3, 2, 1, 1, 1);
        var space = pet(3, 2, 3, true, 1, 3, 1, 1, 1);
        var hidden = pet(4, 2, 3, true, 1, 2, 1, 1, 1); hidden.hide();
        var sold = pet(5, 2, 3, true, 1, 2, 1, 1, 1); ReflectionTestUtils.setField(sold, "status", PetStatus.SOLD);
        var missing = pet(6, 2, 3, true, 1, 2, 1, 1, 1); ReflectionTestUtils.setField(missing, "profile", null);
        var a = answers(HomeSize.SMALL, true, 3, 2, 3, true, 3);
        for (var strategy : List.of(weighted, budget)) {
            assertThat(strategy.match(a, List.of(boundary, shedding, space, hidden, sold, missing)))
                .extracting(r -> r.pet().getId()).containsExactly(1L);
            assertThat(strategy.match(a, List.of(shedding, space))).isEmpty();
        }
    }

    @Test void scoreBreakdownAddsUpAndTiesUseId() {
        var a = answers(HomeSize.LARGE, false, 3, 4, 5, false, 5);
        var result = weighted.match(a, List.of(pet(2, 2, 3, true, 1, 2, 1, 1, 1), pet(1, 2, 3, true, 1, 2, 1, 1, 1)));
        assertThat(result).extracting(r -> r.pet().getId()).containsExactly(1L, 2L);
        assertThat(result.getFirst().components()).hasSize(7).allSatisfy(c -> assertThat(c.fit()).isBetween(0.0, 1.0));
        assertThat(result.getFirst().formula()).contains("100.00%", "100 ×", "Tổng trọng số chuẩn hóa = 100%");
        assertThat(result.getFirst().reasons()).anyMatch(r -> r.contains("Ngân sách"));
    }

    @Test void configurationChangesScoresAndRejectsInvalidWeights() {
        var a = answers(HomeSize.LARGE, false, 5, 2, 1, false, 5);
        var pet = pet(1, 2, 5, true, 1, 2, 5, 1, 2);
        properties.getWeights().put(MatchCriterion.BUDGET, 60.0);
        assertThat(weighted.match(a, List.of(pet)).getFirst().scorePercent()).isCloseTo(100.0 * 80 / 140, within(0.00001));
        properties.getWeights().put(MatchCriterion.TIME, 0.0);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void profileRejectsInvalidRangeAndStaleVersion() {
        var pet = pet(1, 2, 3, true, 1, 2, 1, 1, 1);
        var form = pet.getProfile().toForm(); form.setEnergyLevel(6);
        assertThatThrownBy(() -> pet.updateProfile(form)).isInstanceOf(IllegalArgumentException.class);
        form.setEnergyLevel(3); form.setTimeNeedHoursPerDay(Double.NaN);
        assertThatThrownBy(() -> pet.updateProfile(form)).isInstanceOf(IllegalArgumentException.class);
        ReflectionTestUtils.setField(pet.getProfile(), "id", 1L);
        form.setVersion(9L);
        assertThatThrownBy(() -> pet.updateProfile(form)).isInstanceOf(ProductVersionConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-320, 1e-300, 1e-5, 1, 1000, 1e300, 1e305})
    void uniformWeightScalingPreservesScoresAndOrderForBothStrategies(double factor) {
        var a = answers(HomeSize.LARGE, false, 5, 2, 1, false, 5);
        var candidates = List.of(pet(3, 2, 1, true, 1, 2, 1, 1, 8),
            pet(2, 2, 5, true, 1, 2, 5, 1, 2), pet(1, 2, 5, true, 1, 2, 5, 1, 2),
            pet(4, 2, 3, true, 1, 2, 3, 1, 3));
        var expectedWeighted = weighted.match(a, candidates);
        var expectedBudget = budget.match(a, candidates);
        properties.getWeights().replaceAll((criterion, value) -> value * factor);
        properties.validate();
        var actual = List.of(weighted.match(a, candidates), budget.match(a, candidates));
        var expected = List.of(expectedWeighted, expectedBudget);
        for (int strategy = 0; strategy < actual.size(); strategy++) {
            assertThat(actual.get(strategy)).extracting(r -> r.pet().getId())
                .containsExactlyElementsOf(expected.get(strategy).stream().map(r -> r.pet().getId()).toList());
            for (int rank = 0; rank < candidates.size(); rank++) {
                var match = actual.get(strategy).get(rank);
                assertThat(match.scorePercent()).isFinite().isBetween(0.0, 100.0)
                    .isCloseTo(expected.get(strategy).get(rank).scorePercent(), within(1e-11));
                assertThat(match.components().stream().mapToDouble(MatchComponent::normalizedWeight).sum())
                    .isCloseTo(1.0, within(1e-14));
            }
        }
    }

    @ParameterizedTest @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidWeightsOrNonFiniteTotal(double value) {
        properties.getWeights().replaceAll((criterion, ignored) -> value);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trọng số");
    }

    @Test void validatesActualBudgetTotalWithoutRejectingFiniteLargeTotals() {
        properties.getWeights().replaceAll((criterion, value) -> 1e306);
        properties.getWeights().put(MatchCriterion.SIZE, 1e308);
        assertThatCode(properties::validate).doesNotThrowAnyException();
        var a = answers(HomeSize.LARGE, false, 3, 4, 5, false, 5);
        for (var strategy : List.of(weighted, budget)) {
            assertThat(strategy.match(a, List.of(pet(1, 3, 3, true, 1, 3, 3, 3, 2))).getFirst().scorePercent())
                .isFinite().isBetween(0.0, 100.0).isCloseTo(100, within(1e-12));
        }
        properties.getWeights().put(MatchCriterion.BUDGET, 6e307);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tổng trọng số");
    }

    @ParameterizedTest @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.01, 100.01})
    void resultRejectsNonFiniteOrOutOfRangeScore(double score) {
        assertThatThrownBy(() -> new MatchResult(null, score, List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("0 đến 100%");
    }

    @ParameterizedTest @ValueSource(doubles = {1e-320, 1e-5, 1})
    void explanationUsesNormalizedPercentagesAndDisplayedTermsAddUp(double factor) {
        properties.getWeights().replaceAll((criterion, value) -> value * factor);
        var a = answers(HomeSize.LARGE, false, 5, 2, 1, false, 5);
        var candidate = pet(1, 2, 4, true, 1, 2, 3, 1, 3);
        for (var strategy : List.of(weighted, budget)) {
            var match = strategy.match(a, List.of(candidate)).getFirst();
            assertThat(match.formula()).contains("Tổng trọng số chuẩn hóa = 100%", "Cộng điểm thành phần:")
                .doesNotContain("/ 0.00", "Infinity", "NaN");
            var terms = match.formula().split("Cộng điểm thành phần: ")[1].split(" = ");
            BigDecimal displayedTotal = BigDecimal.ZERO;
            for (String term : terms[0].replace(" (hiệu chỉnh làm tròn)", "").replace(" − ", " + -").split(" \\+ ")) {
                displayedTotal = displayedTotal.add(new BigDecimal(term));
            }
            var displayedScore = new BigDecimal(terms[1].replace("%.", ""));
            assertThat(displayedTotal).isEqualByComparingTo(displayedScore);
            assertThat(displayedScore).isEqualByComparingTo(BigDecimal.valueOf(match.scorePercent()).setScale(2, RoundingMode.HALF_UP));
            for (var component : match.components()) {
                assertThat(component.explanation()).contains("trọng số chuẩn hóa", component.displayedPoints() + " điểm %");
                assertThat(terms[0]).contains(component.displayedPoints().toString());
            }
            if (strategy == budget) assertThat(match.formula()).contains("hiệu chỉnh làm tròn");
        }
    }

    @Test void tinyNonzeroFitWeightAndContributionRemainVisible() {
        var component = new MatchComponent("Thời gian", 1e-12, 1e-10, 1e-20);
        assertThat(component.explanation()).contains("1E-12", "1E-8%", "1E-20 điểm %")
            .doesNotContain("0.00");
    }
}
