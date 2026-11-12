package com.petcare.controller;

import com.petcare.dto.CheckoutForm;
import com.petcare.dto.QuantityForm;
import com.petcare.service.CommerceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
public class CartController {
    private final CommerceService service;

    @GetMapping("/cart") String cart(Model model) {
        model.addAttribute("cart", service.cart());
        return "commerce/cart";
    }
    @PostMapping("/cart/items") String add(@RequestParam Long productId, @Valid @ModelAttribute QuantityForm form,
                                         RedirectAttributes flash) {
        service.addItem(productId, form.getQuantity());
        flash.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng.");
        return "redirect:/cart";
    }
    @PostMapping("/cart/items/{id}/quantity") String quantity(@PathVariable Long id,
            @Valid @ModelAttribute QuantityForm form, RedirectAttributes flash) {
        service.setQuantity(id, form.getQuantity());
        flash.addFlashAttribute("success", "Đã cập nhật số lượng.");
        return "redirect:/cart";
    }
    @PostMapping("/cart/items/{id}/remove") String remove(@PathVariable Long id, RedirectAttributes flash) {
        service.removeItem(id);
        flash.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/cart";
    }
    @GetMapping("/checkout") String checkout(Model model) {
        model.addAttribute("checkoutForm", new CheckoutForm());
        model.addAttribute("cart", service.cart());
        return "commerce/checkout";
    }
    @PostMapping("/checkout") String place(@Valid @ModelAttribute CheckoutForm checkoutForm,
            BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("cart", service.cart());
            return "commerce/checkout";
        }
        Long id = service.placeOrder(checkoutForm.getShippingAddress());
        flash.addFlashAttribute("success", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận.");
        return "redirect:/orders/" + id;
    }
}
