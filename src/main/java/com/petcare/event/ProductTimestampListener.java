package com.petcare.event;

import com.petcare.domain.Product;
import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

/** Ghi ngày tạo sản phẩm bằng Clock chung, không đọc trực tiếp đồng hồ hệ thống trong entity. */
@Component @RequiredArgsConstructor
public class ProductTimestampListener {
    private final Clock clock;
    @PrePersist public void created(Product product) { product.recordCreatedAt(LocalDateTime.now(clock)); }
}
