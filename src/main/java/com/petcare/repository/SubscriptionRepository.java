package com.petcare.repository;

import com.petcare.domain.Subscription;
import com.petcare.domain.subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Query("select s.id from Subscription s where (:customerId is null or s.customer.id = :customerId) and (:activeOnly = false or (s.stateName = com.petcare.domain.subscription.SubscriptionStatus.ACTIVE and s.remainingUses > 0 and s.startDate <= :today and s.endDate >= :today)) order by s.endDate, s.id")
    org.springframework.data.domain.Page<Long> findListIds(Long customerId, boolean activeOnly, LocalDate today, org.springframework.data.domain.Pageable pageable);
    @EntityGraph(attributePaths = {"customer", "carePackage", "coveredServices"})
    @Query("select s from Subscription s where s.id in :ids order by s.endDate, s.id")
    List<Subscription> fetchList(List<Long> ids);

    boolean existsBySourceKey(String key);
    Optional<Subscription> findBySourceKey(String key);
    List<Subscription> findByCustomerIdOrderByEndDateAscIdAsc(Long customerId);
    List<Subscription> findByStateNameOrderByEndDateAscIdAsc(SubscriptionStatus state);
    @Query("select s.id from Subscription s where s.stateName = com.petcare.domain.subscription.SubscriptionStatus.ACTIVE and (s.endDate < :today or s.remainingUses = 0) order by s.id")
    List<Long> findDueIds(LocalDate today);
}
