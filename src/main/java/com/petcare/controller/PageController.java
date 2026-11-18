package com.petcare.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
@Controller @lombok.RequiredArgsConstructor
public class PageController {
 private final com.petcare.service.DashboardService dashboard;
 @GetMapping("/") public String home() { return "home"; }
 @GetMapping("/admin") public String admin(org.springframework.ui.Model model) {
  model.addAttribute("dashboard", dashboard.overview()); return "admin";
 }
 @GetMapping("/admin/dashboard") public String oldAdmin() { return "redirect:/admin"; }
 @GetMapping("/staff/dashboard") public String oldStaff() { return "redirect:/staff"; }
 @GetMapping("/staff") public String staff(org.springframework.ui.Model model) { model.addAttribute("work", dashboard.workOverview()); return "staff"; }
 @GetMapping("/403") @ResponseStatus(HttpStatus.FORBIDDEN) public String forbidden() { return "403"; }
}
