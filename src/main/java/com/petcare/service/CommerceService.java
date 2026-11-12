package com.petcare.service;

import com.petcare.domain.Order;
import com.petcare.domain.order.OrderStatus;
import com.petcare.dto.*;
import com.petcare.repository.CartRepository;
import com.petcare.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Biên giao diện: xác định khách từ phiên đăng nhập và dựng DTO trước khi đóng transaction. */
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CommerceService {
    private final CommerceAccess access;
    private final CartService carts;
    private final OrderService orders;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public CartView cart() {
        return cartRepository.findByCustomerId(access.currentCustomer().getId())
            .map(CartView::from).orElseGet(CartView::empty);
    }
    @Transactional public void addItem(Long productId, int quantity) {
        carts.addItem(access.currentCustomer().getId(), productId, quantity);
    }
    @Transactional public void setQuantity(Long productId, int quantity) {
        carts.setQuantity(access.currentCustomer().getId(), productId, quantity);
    }
    @Transactional public void removeItem(Long productId) {
        carts.removeItem(access.currentCustomer().getId(), productId);
    }
    @Transactional public Long placeOrder(String address) {
        return orders.placeOrderFromCart(access.currentCustomer().getId(), address).getId();
    }
    public List<OrderSummaryView> myOrders() {
        return orderRepository.findSummaries(access.currentCustomer().getId(), null);
    }
    private Order findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
    }
    public OrderView myOrder(Long id) {
        var order = findOrder(id);
        access.customer(order.getCustomer().getId());
        return OrderView.from(order);
    }
    @Transactional public void cancelOwnOrder(Long id) {
        var order = findOrder(id);
        access.customer(order.getCustomer().getId());
        orders.cancel(id);
    }
    public List<OrderSummaryView> staffOrders(OrderStatus status) {
        access.staff();
        return orderRepository.findSummaries(null, status);
    }
    public OrderView staffOrder(Long id) {
        access.staff();
        return OrderView.from(findOrder(id));
    }
}
