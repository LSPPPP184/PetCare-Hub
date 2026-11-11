package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.domain.purchase.PurchaseException;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Khóa DB theo ID tăng dần; refresh tránh đọc tồn kho cũ từ persistence context. */
@Component
public class CommerceLocks {
    @PersistenceContext private EntityManager entityManager;
    public <T> T lock(Class<T> type, Long id) {
        if (id == null) throw new PurchaseException("Mã dữ liệu không được trống.");
        // Giữ các thay đổi trước đó nếu bên gọi ghép nhiều thao tác trong một transaction.
        entityManager.flush();
        T entity = entityManager.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) throw new PurchaseException("Không tìm thấy dữ liệu yêu cầu.");
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return entity;
    }
    public void products(Collection<Long> ids) {
        ids.stream().distinct().sorted().forEach(id -> lock(Product.class, id));
    }
}
