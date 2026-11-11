package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {
    private final CartRepository carts;
    private final CommerceAccess access;
    private final CommerceLocks locks;

    private Cart cart(Long customerId) {
        access.customer(customerId);
        User customer = locks.lock(User.class, customerId);
        return carts.findByCustomerId(customerId).orElseGet(() -> carts.save(new Cart(customer)));
    }
    public Cart addItem(Long customerId, Long productId, int quantity) {
        var cart = cart(customerId);
        cart.add(locks.lock(Product.class, productId), quantity);
        return cart;
    }
    public Cart setQuantity(Long customerId, Long productId, int quantity) {
        var cart = cart(customerId);
        locks.lock(Product.class, productId);
        cart.setQuantity(productId, quantity);
        return cart;
    }
    public Cart removeItem(Long customerId, Long productId) {
        var cart = cart(customerId);
        cart.remove(productId);
        return cart;
    }
}
