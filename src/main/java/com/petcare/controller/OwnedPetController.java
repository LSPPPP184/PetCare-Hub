package com.petcare.controller;

import com.petcare.domain.Species;
import com.petcare.dto.OwnedPetForm;
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

@Controller @RequestMapping("/my-pets") @RequiredArgsConstructor
public class OwnedPetController {
    private final PetHealthService service;
    private final VaccinationService vaccinations;
    @ModelAttribute("speciesOptions") Species[] species() { return Species.values(); }
    @ModelAttribute("staff") boolean staff() { return false; }
    @ModelAttribute("today") LocalDate today() { return service.today(); }

    @GetMapping String list(@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(10000) int page, Model model) { var result = service.mine(org.springframework.data.domain.PageRequest.of(page, 20)); model.addAttribute("pageResult", result); model.addAttribute("pets", result.getContent()); return "health/pets"; }
    @GetMapping("/new") String create(Model model) {
        model.addAttribute("form", new OwnedPetForm()); model.addAttribute("editing", false);
        return "health/pet-form";
    }
    @PostMapping String save(@Valid @ModelAttribute("form") OwnedPetForm form, BindingResult errors,
                            Model model, RedirectAttributes flash) {
        model.addAttribute("editing", false);
        if (errors.hasErrors()) return "health/pet-form";
        Long id = service.create(form);
        flash.addFlashAttribute("success", "Đã thêm thú cưng và sinh lịch tiêm mẫu theo loài.");
        return "redirect:/my-pets/" + id;
    }
    @GetMapping("/{id}") String detail(@PathVariable Long id, Model model) {
        model.addAttribute("profile", service.myDetail(id)); return "health/detail";
    }
    @GetMapping("/{id}/edit") String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", service.myDetail(id).pet().toForm());
        model.addAttribute("editing", true); model.addAttribute("petId", id);
        return "health/pet-form";
    }
    @PostMapping("/{id}/edit") String update(@PathVariable Long id, @Valid @ModelAttribute("form") OwnedPetForm form,
                                          BindingResult errors, Model model, RedirectAttributes flash) {
        service.myDetail(id);
        model.addAttribute("editing", true); model.addAttribute("petId", id);
        if (errors.hasErrors()) return "health/pet-form";
        service.update(id, form); flash.addFlashAttribute("success", "Đã cập nhật hồ sơ thú cưng.");
        return "redirect:/my-pets/" + id;
    }
    @PostMapping("/vaccinations/{id}/reschedule") String reschedule(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam long version, RedirectAttributes flash) {
        Long petId = vaccinations.rescheduleMine(id, date, version);
        flash.addFlashAttribute("success", "Đã dời lịch tiêm.");
        return "redirect:/my-pets/" + petId;
    }
}
