package com.petcare.event;

import com.petcare.domain.Order;
import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

/** Giữ hành vi tự ghi thời gian của đơn cũ, nhưng dùng Clock được inject để kiểm thử xác định. */
@Component @RequiredArgsConstructor
public class OrderTimestampListener {
    private final Clock clock;
    @PrePersist public void created(Order order) { order.recordCreatedAt(LocalDateTime.now(clock)); }
}
