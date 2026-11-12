package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Locale;

/** Kiểm tra người dùng tại biên service, không tin customerId do bên gọi truyền vào. */
@Component
@RequiredArgsConstructor
public class CommerceAccess {
    private final UserRepository users;
    private User actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw denied();
        return users.findByEmail(auth.getName().strip().toLowerCase(Locale.ROOT)).orElseThrow(this::denied);
    }
    private AccessDeniedException denied() { return new AccessDeniedException("Bạn không có quyền thực hiện thao tác này."); }
    public User customer(Long customerId) {
        var actor = actor();
        if (actor.getRole() != Role.CUSTOMER || !actor.getId().equals(customerId)) throw denied();
        return actor;
    }
    /** Lấy khách hiện tại từ phiên đăng nhập, không nhận ID khách từ form. */
    public User currentCustomer() {
        var actor = actor();
        if (actor.getRole() != Role.CUSTOMER) throw denied();
        return actor;
    }
    public void staff() {
        var role = actor().getRole();
        if (role != Role.ADMIN && role != Role.STAFF) throw denied();
    }
    public void ownerOrStaff(Order order) {
        var actor = actor();
        if (actor.getRole() == Role.ADMIN || actor.getRole() == Role.STAFF) return;
        if (actor.getRole() != Role.CUSTOMER || !actor.getId().equals(order.getCustomer().getId())) throw denied();
    }
}
