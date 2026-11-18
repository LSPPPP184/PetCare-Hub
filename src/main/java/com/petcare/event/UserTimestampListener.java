package com.petcare.event;

import com.petcare.domain.User;
import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

/** Ghi ngày tạo tài khoản bằng Clock chung, giúp kiểm chứng mốc ngày một cách xác định. */
@Component @RequiredArgsConstructor
public class UserTimestampListener {
    private final Clock clock;
    @PrePersist public void created(User user) { user.recordCreatedAt(LocalDateTime.now(clock)); }
}
