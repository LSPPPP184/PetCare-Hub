package com.petcare.domain;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

/** Số dư chỉ thay đổi qua hành vi nghiệp vụ, không cung cấp setter cho form. */
@Entity @Table(name = "loyalty_accounts") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(constraints = "current_points >= 0 and total_points >= 0")
public class LoyaltyAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true) private User customer;
    @Column(nullable = false) private long totalPoints;
    @Column(nullable = false) private long currentPoints;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MembershipTier tier = MembershipTier.BRONZE;
    @Column(nullable = false) private boolean voucherAvailable;

    public LoyaltyAccount(User customer) {
        if (customer == null || customer.getRole() != Role.CUSTOMER) throw new LoyaltyException("Chỉ khách hàng có tài khoản điểm.");
        this.customer = customer;
    }
    public void credit(long points, LoyaltyProperties properties) {
        if (points < 0) throw new LoyaltyException("Điểm tích lũy không được âm.");
        try {
            long newCurrent = Math.addExact(currentPoints, points);
            long newTotal = Math.addExact(totalPoints, points);
            currentPoints = newCurrent;
            totalPoints = newTotal;
        } catch (ArithmeticException ex) { throw new LoyaltyException("Số điểm vượt giới hạn lưu trữ."); }
        synchronizeTier(properties);
    }
    /**
     * Ngưỡng hiện hành áp dụng ngay cho mọi tài khoản vì hạng chỉ phụ thuộc tổng điểm tích lũy.
     * Luồng đọc tính hạng tại chỗ, không cập nhật entity để tránh khóa ghi khi xem danh sách.
     * Đổi ngưỡng có thể đổi hạng; tiêu điểm không đổi totalPoints nên không làm giảm hạng.
     * Giá đơn/lịch đã chốt không được tính lại theo hạng này.
     */
    public MembershipTier currentTier(LoyaltyProperties properties) { return properties.tierFor(totalPoints); }
    /** Đồng bộ cột hạng trong giao dịch đã khóa đúng tài khoản cần cập nhật. */
    public void synchronizeTier(LoyaltyProperties properties) { tier = currentTier(properties); }
    /** Điều chỉnh âm chỉ trừ số dư; điểm tích lũy và hạng không bị giảm. */
    public void adjust(long points, LoyaltyProperties properties) {
        if (points >= 0) { credit(points, properties); return; }
        if (points < -currentPoints) throw new LoyaltyException("Điểm hiện có không đủ để điều chỉnh.");
        currentPoints += points;
    }
    public void redeem() {
        if (voucherAvailable) throw new LoyaltyException("Vui lòng dùng voucher hiện có trước khi đổi tiếp.");
        if (currentPoints < 100) throw new LoyaltyException("Cần ít nhất 100 điểm để đổi voucher.");
        currentPoints -= 100;
        voucherAvailable = true;
    }
    public void consumeVoucher() {
        if (!voucherAvailable) throw new LoyaltyException("Không có voucher để sử dụng.");
        voucherAvailable = false;
    }
}
