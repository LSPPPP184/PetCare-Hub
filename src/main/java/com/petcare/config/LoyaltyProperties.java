package com.petcare.config;

import com.petcare.domain.loyalty.MembershipTier;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/** Cấu hình tên tiếng Việt, ngưỡng và quyền lợi; thứ tự hạng luôn tăng dần. */
@Component @ConfigurationProperties(prefix = "petcare.loyalty") @Getter
public class LoyaltyProperties {
    private final Map<MembershipTier, Tier> tiers = new EnumMap<>(MembershipTier.class);
    public LoyaltyProperties() {
        tiers.put(MembershipTier.BRONZE, new Tier("Đồng", 0, "0", "Tích điểm cho mỗi đơn hàng và lịch hẹn hoàn tất."));
        tiers.put(MembershipTier.SILVER, new Tier("Bạc", 500, "0.03", "Giảm 3% khi mua hàng và đặt lịch."));
        tiers.put(MembershipTier.GOLD, new Tier("Vàng", 2000, "0.05", "Giảm 5% khi mua hàng và đặt lịch."));
        tiers.put(MembershipTier.PLATINUM, new Tier("Kim cương", 5000, "0.10", "Giảm 10% khi mua hàng và đặt lịch."));
    }
    @Getter @Setter
    public static class Tier {
        private String name;
        private long threshold;
        private BigDecimal rate;
        private String benefits;
        public Tier() {}
        public Tier(String name, long threshold, String rate, String benefits) {
            this.name = name; this.threshold = threshold; this.rate = new BigDecimal(rate); this.benefits = benefits;
        }
    }
    @PostConstruct public void validate() {
        long previous = -1;
        for (var tier : MembershipTier.values()) {
            var value = tiers.get(tier);
            if (value == null || value.threshold <= previous || (tier == MembershipTier.BRONZE && value.threshold != 0)
                || value.rate == null || value.rate.signum() < 0 || value.rate.compareTo(BigDecimal.ONE) > 0
                || value.name == null || value.name.isBlank() || value.benefits == null || value.benefits.isBlank())
                throw new IllegalStateException("Cấu hình hạng thành viên không hợp lệ.");
            previous = value.threshold;
        }
    }
    public Tier tier(MembershipTier tier) { return tiers.get(tier); }
    public MembershipTier tierFor(long total) {
        return Arrays.stream(MembershipTier.values()).filter(t -> total >= tier(t).threshold)
            .reduce((a, b) -> b).orElse(MembershipTier.BRONZE);
    }
    /** Khoảng nửa mở dùng cùng ngưỡng với tierFor để lọc và phân trang ngay tại DB. */
    public PointsRange rangeFor(MembershipTier tier) {
        if (tier == null) return new PointsRange(0, null);
        Long upper = Arrays.stream(MembershipTier.values()).filter(t -> t.ordinal() > tier.ordinal()).findFirst()
            .map(t -> tier(t).getThreshold()).orElse(null);
        return new PointsRange(tier(tier).getThreshold(), upper);
    }
    public record PointsRange(long lowerInclusive, Long upperExclusive) {}
}
