package com.petcare.controller;

import com.petcare.domain.loyalty.*;
import com.petcare.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
public class LoyaltyController {
    private final LoyaltyService loyalty;
    @GetMapping("/membership") String mine(Model model) {
        model.addAttribute("member", loyalty.mine()); model.addAttribute("admin", false);
        return "loyalty/card";
    }
    @PostMapping("/membership/redeem") String redeem(RedirectAttributes flash) {
        try { loyalty.redeem(); flash.addFlashAttribute("success", "Đã đổi 100 điểm lấy voucher 10.000đ."); }
        catch (LoyaltyException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/membership";
    }
    @GetMapping("/admin/members") String members(@RequestParam(required = false) MembershipTier tier,
                                               @RequestParam(defaultValue = "0") int page, Model model) {
        var members = loyalty.members(tier, page);
        model.addAttribute("members", members.getContent()); model.addAttribute("memberPage", members);
        model.addAttribute("tiers", MembershipTier.values()); model.addAttribute("benefits", loyalty.benefits());
        model.addAttribute("selectedTier", tier);
        return "loyalty/members";
    }
    @GetMapping("/admin/members/{id}") String member(@PathVariable Long id, Model model) {
        model.addAttribute("member", loyalty.member(id)); model.addAttribute("admin", true);
        return "loyalty/card";
    }
    @PostMapping("/admin/members/{id}/adjust") String adjust(@PathVariable Long id, @RequestParam long points,
            @RequestParam String reason, RedirectAttributes flash) {
        try { loyalty.adjust(id, points, reason); flash.addFlashAttribute("success", "Đã điều chỉnh điểm và ghi lại lý do."); }
        catch (LoyaltyException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/members/" + id;
    }
}
