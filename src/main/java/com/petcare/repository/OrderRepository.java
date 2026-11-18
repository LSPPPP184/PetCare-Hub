package com.petcare.repository;

import com.petcare.domain.Order;
import com.petcare.domain.order.OrderStatus;
import com.petcare.dto.OrderSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    long countByStateName(OrderStatus stateName);
    interface MonthlyRevenue {
        Integer getYear();
        Integer getMonth();
        java.math.BigDecimal getAmount();
    }
    interface BestSeller {
        Long getProductId();
        String getName();
        Long getQuantity();
    }
    @Query("""
        select year(o.createdAt) as year, month(o.createdAt) as month, sum(o.finalAmount) as amount
        from Order o where o.stateName = :status and o.createdAt >= :start and o.createdAt < :end
        group by year(o.createdAt), month(o.createdAt)
        """)
    List<MonthlyRevenue> monthlyRevenue(OrderStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("""
        select p.id as productId, p.name as name, sum(i.quantity) as quantity
        from OrderItem i join i.product p join i.order o where o.stateName = :status
        group by p.id, p.name order by sum(i.quantity) desc, p.id asc
        """)
    List<BestSeller> bestSellers(OrderStatus status, org.springframework.data.domain.Pageable pageable);

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
