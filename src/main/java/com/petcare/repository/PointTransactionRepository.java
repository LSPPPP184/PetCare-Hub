package com.petcare.repository;

import com.petcare.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    boolean existsBySourceKey(String sourceKey);
    List<PointTransaction> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
}
