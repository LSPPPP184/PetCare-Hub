package com.petcare.controller;

import com.petcare.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller @RequiredArgsConstructor @RequestMapping("/staff/appointments")
public class StaffAppointmentController {
    private final AppointmentService service;
    @GetMapping String schedule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        model.addAttribute("appointments", service.schedule(date));
        model.addAttribute("date", date == null ? service.today() : date);
        model.addAttribute("staff", true);
        return "appointments/list";
    }
    @GetMapping("/{id}") String detail(@PathVariable Long id, Model model) {
        model.addAttribute("appointment", service.staffDetail(id));
        model.addAttribute("staff", true);
        return "appointments/detail";
    }
    @PostMapping("/{id}/start") String start(@PathVariable Long id, RedirectAttributes flash) {
        service.start(id);
        return success(id, "Đã bắt đầu thực hiện dịch vụ.", flash);
    }
    @PostMapping("/{id}/finish") String finish(@PathVariable Long id, RedirectAttributes flash) {
        service.finish(id);
        return success(id, "Đã hoàn tất lịch hẹn.", flash);
    }
    @PostMapping("/{id}/cancel") String cancel(@PathVariable Long id, RedirectAttributes flash) {
        service.cancelByStaff(id);
        return success(id, "Đã hủy lịch hẹn.", flash);
    }
    private String success(Long id, String message, RedirectAttributes flash) {
        flash.addFlashAttribute("success", message);
        return "redirect:/staff/appointments/" + id;
    }
}
