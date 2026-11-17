package com.petcare.repository;

import com.petcare.domain.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByCustomerId(Long customerId);

    /** Chỉ đọc các thẻ đã mở; fetch khách cùng trang để không phát sinh N+1 hoặc khóa ghi. */
    @Query(value = """
        select a from LoyaltyAccount a join fetch a.customer c
        where a.totalPoints >= :lower and (:upper is null or a.totalPoints < :upper)
        order by c.id desc
        """, countQuery = """
        select count(a) from LoyaltyAccount a
        where a.totalPoints >= :lower and (:upper is null or a.totalPoints < :upper)
        """)
    Page<LoyaltyAccount> findMembers(@Param("lower") long lower, @Param("upper") Long upper, Pageable pageable);
}
