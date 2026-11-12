package com.petcare.controller;

import com.petcare.domain.order.OrderStatus;
import com.petcare.dto.OrderStatusView;
import com.petcare.service.CommerceService;
import com.petcare.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Arrays;

@Controller @RequiredArgsConstructor @RequestMapping("/staff/orders")
public class StaffOrderController {
    private final CommerceService views;
    private final OrderService orders;

    @GetMapping String list(@RequestParam(required = false) OrderStatus status, Model model) {
        model.addAttribute("orders", views.staffOrders(status));
        model.addAttribute("statuses", Arrays.stream(OrderStatus.values()).map(OrderStatusView::from).toList());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("staff", true);
        return "commerce/orders";
    }
    @GetMapping("/{id}") String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", views.staffOrder(id));
        model.addAttribute("staff", true);
        return "commerce/order";
    }
    @PostMapping("/{id}/confirm") String confirm(@PathVariable Long id, RedirectAttributes flash) {
        orders.confirm(id);
        return success(id, "Đã xác nhận đơn hàng.", flash);
    }
    @PostMapping("/{id}/ship") String ship(@PathVariable Long id, RedirectAttributes flash) {
        orders.ship(id);
        return success(id, "Đơn hàng đã chuyển sang trạng thái đang giao.", flash);
    }
    @PostMapping("/{id}/complete") String complete(@PathVariable Long id, RedirectAttributes flash) {
        orders.complete(id);
        return success(id, "Đã hoàn tất đơn hàng.", flash);
    }
    @PostMapping("/{id}/cancel") String cancel(@PathVariable Long id, RedirectAttributes flash) {
        orders.cancel(id);
        return success(id, "Đã hủy đơn hàng.", flash);
    }
    private String success(Long id, String message, RedirectAttributes flash) {
        flash.addFlashAttribute("success", message);
        return "redirect:/staff/orders/" + id;
    }
}
