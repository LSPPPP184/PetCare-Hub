package com.petcare.service;

import com.petcare.config.MatchingProperties;
import com.petcare.domain.QuizResult;
import com.petcare.dto.*;
import com.petcare.repository.*;
import com.petcare.service.matching.MatchingStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @Validated @Transactional(readOnly = true)
public class MatchingService {
    private final MatchingPetRepository pets;
    private final QuizResultRepository history;
    private final UserRepository users;
    private final MatchingProperties properties;
    private final Clock clock;
    private final Map<String, MatchingStrategy> strategies;

    public MatchingService(MatchingPetRepository pets, QuizResultRepository history, UserRepository users,
                           MatchingProperties properties, Clock clock, List<MatchingStrategy> strategies) {
        this.pets = pets; this.history = history; this.users = users; this.properties = properties; this.clock = clock;
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(MatchingStrategy::key, Function.identity()));
    }
    public Map<String, String> options() {
        return strategies.values().stream().collect(Collectors.toMap(MatchingStrategy::key, MatchingStrategy::label,
            (a, b) -> a, TreeMap::new));
    }
    public String defaultStrategy() { return properties.getDefaultStrategy(); }

    @Transactional
    public QuizReport submit(@NotNull @Valid QuizAnswers answers, String strategyKey, String email) {
        String key = strategyKey == null || strategyKey.isBlank() ? defaultStrategy() : strategyKey;
        var strategy = strategies.get(key);
        if (strategy == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiến lược ghép đôi không hợp lệ.");
        var owner = email == null ? null : users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        var matches = strategy.match(answers, pets.findCandidates()).stream().limit(3).toList();
        var result = new QuizResult(owner, answers, key, strategy.label(), LocalDateTime.now(clock), matches);
        if (owner != null) history.save(result);
        return QuizReport.from(result);
    }

    public List<QuizReport> history(String email) {
        return history.findByOwnerEmailOrderByCreatedAtDescIdDesc(email).stream().map(QuizReport::from).toList();
    }
    public QuizReport detail(Long id, String email) {
        return QuizReport.from(history.findByIdAndOwnerEmail(id, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kết quả quiz của bạn.")));
    }
}
