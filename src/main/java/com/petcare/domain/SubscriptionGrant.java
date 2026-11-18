package com.petcare.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Biên nhận hoàn tất cả lô cấp/gia hạn, ghi sau khi xử lý mọi dòng trong cùng giao dịch.
 * Giữ PK order_id và FK subscription cũ (trỏ gói đầu tiên của lô), không đổi schema đã có.
 * Mỗi đơn vị mua mới chống trùng riêng bằng Subscription.sourceKey; biên nhận cũ vẫn hợp lệ.
 */
@Entity @Table(name = "subscription_grants") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionGrant {
    @Id private Long id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Subscription subscription;
    public SubscriptionGrant(Order order, Subscription subscription) { this.order = order; this.subscription = subscription; }
}
