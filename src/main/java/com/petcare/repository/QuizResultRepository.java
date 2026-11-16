package com.petcare.repository;

import com.petcare.domain.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByOwnerEmailOrderByCreatedAtDescIdDesc(String email);
    Optional<QuizResult> findByIdAndOwnerEmail(Long id, String email);
}
