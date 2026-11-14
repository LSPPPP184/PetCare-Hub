package com.petcare.controller;

import com.petcare.service.DemoDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller @Profile("demo") @RequestMapping("/admin/demo/health") @RequiredArgsConstructor
public class DemoDateController {
    private final DemoDateService service;
    @GetMapping String page(Model model) { model.addAttribute("today", service.today()); return "health/demo"; }
    @PostMapping String simulate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 RedirectAttributes flash) {
        service.simulate(date);
        flash.addFlashAttribute("success", "Đã giả lập ngày và chạy scheduler tiêm phòng.");
        return "redirect:/admin/demo/health";
    }
}
