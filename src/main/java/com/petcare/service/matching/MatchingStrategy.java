package com.petcare.service.matching;

import com.petcare.domain.Pet;
import com.petcare.dto.MatchResult;
import com.petcare.dto.QuizAnswers;
import java.util.List;

/** Strategy tách thuật toán ghép đôi khỏi service và controller, cho phép hoán đổi lúc chạy. */
public interface MatchingStrategy {
    List<MatchResult> match(QuizAnswers answers, List<Pet> candidates);
    String key();
    String label();
}
