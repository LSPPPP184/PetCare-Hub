package com.petcare.controller;

import com.petcare.service.CommerceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
public class OrderController {
    private final CommerceService service;

    @GetMapping("/orders") String history(Model model) {
        model.addAttribute("orders", service.myOrders());
        model.addAttribute("staff", false);
        return "commerce/orders";
    }
    @GetMapping("/orders/{id}") String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", service.myOrder(id));
        model.addAttribute("staff", false);
        return "commerce/order";
    }
    @PostMapping("/orders/{id}/cancel") String cancel(@PathVariable Long id, RedirectAttributes flash) {
        service.cancelOwnOrder(id);
        flash.addFlashAttribute("success", "Đã hủy đơn hàng.");
        return "redirect:/orders/" + id;
    }
}
