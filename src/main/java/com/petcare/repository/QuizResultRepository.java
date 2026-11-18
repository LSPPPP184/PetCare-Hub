package com.petcare.repository;

import com.petcare.domain.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    @org.springframework.data.jpa.repository.Query(value = "select new com.petcare.dto.QuizSummary(q.id, q.strategyLabel, q.createdAt, cast(size(q.matches) as long)) from QuizResult q where q.owner.email = :email order by q.createdAt desc, q.id desc",
        countQuery = "select count(q) from QuizResult q where q.owner.email = :email")
    org.springframework.data.domain.Page<com.petcare.dto.QuizSummary> findSummaries(String email, org.springframework.data.domain.Pageable pageable);

    List<QuizResult> findByOwnerEmailOrderByCreatedAtDescIdDesc(String email);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "matches")
    Optional<QuizResult> findByIdAndOwnerEmail(Long id, String email);
}
