package com.petcare.domain;

import com.petcare.domain.loyalty.PointTransactionType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Khóa nguồn không null tránh khác biệt xử lý UNIQUE/NULL giữa SQL Server và H2. */
@Entity @Table(name = "point_transactions") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private LoyaltyAccount account;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PointTransactionType type;
    @Column(nullable = false) private long points;
    @Column(nullable = false, length = 1200) private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "related_order_id") private Order relatedOrder;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "related_appointment_id") private Appointment relatedAppointment;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false, unique = true, updatable = false, length = 100) private String sourceKey;
    public PointTransaction(LoyaltyAccount account, PointTransactionType type, long points, String description,
                            Order order, Appointment appointment, LocalDateTime createdAt, String sourceKey) {
        this.account = account; this.type = type; this.points = points; this.description = description;
        this.relatedOrder = order; this.relatedAppointment = appointment; this.createdAt = createdAt; this.sourceKey = sourceKey;
    }
}
