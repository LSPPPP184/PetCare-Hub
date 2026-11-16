package com.petcare.dto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Trọng số lưu dưới dạng tỷ lệ 0–1, hiển thị theo phần trăm; điểm thô không bị làm tròn. */
public record MatchComponent(String criterion, double fit, double normalizedWeight, double points) {
    public BigDecimal displayedPoints() {
        var value = BigDecimal.valueOf(points);
        var rounded = value.setScale(2, RoundingMode.HALF_UP);
        // Điểm dương rất nhỏ vẫn hiện bằng số có nghĩa, không biến thành 0,00.
        return rounded.signum() == 0 && value.signum() != 0
            ? value.round(new MathContext(6, RoundingMode.HALF_UP)).stripTrailingZeros() : rounded;
    }

    static String formatNumber(double value) {
        var rounded = BigDecimal.valueOf(value).round(new MathContext(6, RoundingMode.HALF_UP)).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.toPlainString() : rounded.toString();
    }

    public String explanation() {
        return criterion + ": độ khớp " + formatNumber(fit) + " × trọng số chuẩn hóa "
            + formatNumber(normalizedWeight * 100) + "% → " + displayedPoints() + " điểm %.";
    }
}
