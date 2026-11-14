package com.petcare.controller;

import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.dto.HealthRecordForm;
import com.petcare.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller @RequestMapping("/staff/health") @RequiredArgsConstructor
public class StaffPetHealthController {
    private final PetHealthService service;
    private final VaccinationService vaccinations;
    @ModelAttribute("staff") boolean staff() { return true; }
    @ModelAttribute("today") LocalDate today() { return service.today(); }
    @GetMapping String alerts(@RequestParam(required = false) VaccinationStatus status, Model model) {
        model.addAttribute("records", service.alerts(status)); model.addAttribute("selectedStatus", status);
        return "health/alerts";
    }
    @GetMapping("/pets") String pets(Model model) { model.addAttribute("pets", service.allPets()); return "health/pets"; }
    @GetMapping("/pets/{id}") String detail(@PathVariable Long id, Model model) {
        model.addAttribute("profile", service.staffDetail(id)); return "health/detail";
    }
    @GetMapping("/pets/{id}/records/new") String form(@PathVariable Long id, Model model) {
        model.addAttribute("pet", service.staffDetail(id).pet());
        var form = new HealthRecordForm(); form.setDate(service.today());
        model.addAttribute("form", form); return "health/record-form";
    }
    @PostMapping("/pets/{id}/records") String add(@PathVariable Long id,
            @Valid @ModelAttribute("form") HealthRecordForm form, BindingResult errors, Model model, RedirectAttributes flash) {
        model.addAttribute("pet", service.staffDetail(id).pet());
        if (errors.hasErrors()) return "health/record-form";
        service.addHealthRecord(id, form); flash.addFlashAttribute("success", "Đã thêm bản ghi khám bệnh.");
        return "redirect:/staff/health/pets/" + id;
    }
    @PostMapping("/vaccinations/{id}/complete") String complete(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, RedirectAttributes flash) {
        Long petId = vaccinations.complete(id, date).getOwnedPet().getId();
        flash.addFlashAttribute("success", "Đã xác nhận tiêm phòng.");
        return "redirect:/staff/health/pets/" + petId;
    }
    @PostMapping("/vaccinations/{id}/reschedule") String reschedule(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam long version, RedirectAttributes flash) {
        Long petId = vaccinations.rescheduleByStaff(id, date, version);
        flash.addFlashAttribute("success", "Đã dời lịch tiêm.");
        return "redirect:/staff/health/pets/" + petId;
    }
}
