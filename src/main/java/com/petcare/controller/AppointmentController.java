package com.petcare.controller;

import com.petcare.domain.Species;
import com.petcare.dto.AppointmentForm;
import com.petcare.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;

@Controller @RequiredArgsConstructor @RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService service;

    private void selectionModel(Long serviceId, Long staffId, LocalDateTime start, Model model) {
        var confirmation = service.confirmation(serviceId, staffId, start);
        model.addAttribute("selection", confirmation.selection());
        model.addAttribute("subscriptionPreview", confirmation.subscriptionPreview());
        model.addAttribute("quote", confirmation.quote());
    }
    @ModelAttribute("speciesOptions") Species[] species() { return Species.values(); }
    @GetMapping("/services") String services(Model model) {
        model.addAttribute("services", service.services());
        return "appointments/services";
    }
    @GetMapping("/slots") String slots(@RequestParam Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        model.addAttribute("availability", service.availableSlots(serviceId, date));
        return "appointments/slots";
    }
    @GetMapping("/confirm") String confirm(@RequestParam Long serviceId, @RequestParam Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime, Model model) {
        var form = new AppointmentForm();
        form.setServiceId(serviceId); form.setStaffId(staffId); form.setStartTime(startTime);
        model.addAttribute("form", form);
        selectionModel(serviceId, staffId, startTime, model);
        return "appointments/confirm";
    }
    @PostMapping String book(@Valid @ModelAttribute("form") AppointmentForm form, BindingResult errors,
                            Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            selectionModel(form.getServiceId(), form.getStaffId(), form.getStartTime(), model);
            return "appointments/confirm";
        }
        Long id = service.book(form);
        flash.addFlashAttribute("success", "Đặt lịch thành công.");
        return "redirect:/appointments/" + id;
    }
    @GetMapping String mine(Model model) {
        model.addAttribute("appointments", service.mine());
        model.addAttribute("staff", false);
        return "appointments/list";
    }
    @GetMapping("/{id}") String detail(@PathVariable Long id, Model model) {
        model.addAttribute("appointment", service.myDetail(id));
        model.addAttribute("staff", false);
        return "appointments/detail";
    }
    @PostMapping("/{id}/cancel") String cancel(@PathVariable Long id, RedirectAttributes flash) {
        service.cancelMine(id);
        flash.addFlashAttribute("success", "Đã hủy lịch hẹn.");
        return "redirect:/appointments/" + id;
    }
}
