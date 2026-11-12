package com.petcare.repository;

import com.petcare.domain.Order;
import com.petcare.domain.order.OrderStatus;
import com.petcare.dto.OrderSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
        select new com.petcare.dto.OrderSummaryView(
            o.id, o.createdAt, o.stateName, o.finalAmount, count(item.id), customer.fullName)
        from Order o join o.customer customer left join o.items item
        where (:customerId is null or customer.id = :customerId)
          and (:status is null or o.stateName = :status)
        group by o.id, o.createdAt, o.stateName, o.finalAmount, customer.fullName
        order by o.createdAt desc, o.id desc
        """)
    List<OrderSummaryView> findSummaries(@Param("customerId") Long customerId, @Param("status") OrderStatus status);
}
