package com.petcare.controller;

import com.petcare.service.SubscriptionService;
import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.purchase.PurchaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService service;
    @GetMapping("/subscriptions") String mine(@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(10000) int page, Model model) {
        var result = service.mine(org.springframework.data.domain.PageRequest.of(page, 20)); model.addAttribute("pageResult", result); model.addAttribute("subscriptions", result.getContent()); model.addAttribute("admin", false);
        return "subscriptions/list";
    }
    @GetMapping("/admin/subscriptions") String active(@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(10000) int page, Model model) {
        var result = service.active(org.springframework.data.domain.PageRequest.of(page, 20)); model.addAttribute("pageResult", result); model.addAttribute("subscriptions", result.getContent()); model.addAttribute("admin", true);
        return "subscriptions/list";
    }
    @PostMapping("/subscriptions/{id}/cancel") String cancel(@PathVariable Long id, RedirectAttributes flash) {
        service.cancel(id); flash.addFlashAttribute("success", "Đã hủy gói chăm sóc. Các lịch đã đặt vẫn được giữ nguyên.");
        return "redirect:/subscriptions";
    }
    @PostMapping("/subscriptions/{id}/renew") String renew(@PathVariable Long id, RedirectAttributes flash) {
        var orderId = service.renew(id);
        flash.addFlashAttribute("success", "Đã tạo đơn gia hạn mô phỏng. Quyền lợi được cộng khi nhân viên hoàn tất đơn.");
        return "redirect:/orders/" + orderId;
    }
    @ExceptionHandler({InvalidStateTransitionException.class, PurchaseException.class})
    String business(RuntimeException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage()); return "redirect:/subscriptions";
    }
    @ExceptionHandler(ConcurrencyFailureException.class)
    String conflict(RedirectAttributes flash) {
        flash.addFlashAttribute("error", "Gói vừa được cập nhật. Vui lòng tải lại và thử lại."); return "redirect:/subscriptions";
    }
}
