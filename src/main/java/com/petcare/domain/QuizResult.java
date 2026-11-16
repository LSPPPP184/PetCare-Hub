package com.petcare.domain;

import com.petcare.dto.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "quiz_results") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false) private User owner;
    @Embedded private QuizAnswers answers;
    @Column(nullable = false, length = 30) private String strategyKey;
    @Column(nullable = false, length = 100) private String strategyLabel;
    @Column(nullable = false) private LocalDateTime createdAt;
    @ElementCollection
    @CollectionTable(name = "quiz_matches", joinColumns = @JoinColumn(name = "quiz_id"))
    @OrderColumn(name = "rank_index")
    private List<QuizMatchSnapshot> matches = new ArrayList<>();

    public QuizResult(User owner, QuizAnswers answers, String key, String label,
                      LocalDateTime createdAt, List<MatchResult> matches) {
        this.owner = owner; this.answers = answers.copy(); this.strategyKey = key;
        this.strategyLabel = label; this.createdAt = createdAt;
        this.matches.addAll(matches.stream().map(QuizMatchSnapshot::new).toList());
    }

    public QuizAnswers getAnswers() { return answers.copy(); }
    public List<QuizMatchSnapshot> getMatches() { return List.copyOf(matches); }
}
