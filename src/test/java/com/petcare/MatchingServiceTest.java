package com.petcare;

import com.petcare.config.MatchingProperties;
import com.petcare.domain.*;
import com.petcare.dto.HomeSize;
import com.petcare.repository.*;
import com.petcare.service.MatchingService;
import com.petcare.service.matching.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchingServiceTest {
    @Test void configuredStrategyAndInjectedClockAreUsedWithoutSavingGuest() {
        var pets = mock(MatchingPetRepository.class);
        var history = mock(QuizResultRepository.class);
        var users = mock(UserRepository.class);
        var properties = new MatchingProperties(); properties.setDefaultStrategy("budget-first");
        var clock = Clock.fixed(Instant.parse("2026-12-31T17:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        var service = new MatchingService(pets, history, users, properties, clock,
            List.of(new WeightedScoreStrategy(properties), new BudgetFirstStrategy(properties)));
        when(pets.findCandidates()).thenReturn(List.of());
        var answers = MatchingStrategyTest.answers(HomeSize.SMALL, false, 1, 1, 1, false, 1);
        var report = service.submit(answers, null, null);
        assertThat(report.strategyLabel()).isEqualTo("Ưu tiên ngân sách");
        assertThat(report.createdAt()).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0));
        verifyNoInteractions(history, users);
        when(users.findByEmail("customer@test.vn")).thenReturn(Optional.of(new User("Khách", "customer@test.vn", "hash", "", Role.CUSTOMER)));
        service.submit(answers, "weighted", "customer@test.vn");
        verify(history).save(any(QuizResult.class));
        assertThatThrownBy(() -> service.submit(answers, "unknown", null)).isInstanceOf(ResponseStatusException.class);
    }
}
