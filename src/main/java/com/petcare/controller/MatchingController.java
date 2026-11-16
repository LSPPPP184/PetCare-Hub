package com.petcare.controller;

import com.petcare.dto.*;
import com.petcare.service.MatchingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@Controller @RequiredArgsConstructor
@RequestMapping("/quiz")
public class MatchingController {
    private final MatchingService service;
    @ModelAttribute("homeSizes") HomeSize[] homeSizes() { return HomeSize.values(); }
    @ModelAttribute("strategies") Map<String, String> strategies() { return service.options(); }

    @GetMapping String form(Model model) {
        model.addAttribute("answers", new QuizAnswers());
        model.addAttribute("strategy", service.defaultStrategy());
        return "matching/quiz";
    }

    @PostMapping String submit(@Valid @ModelAttribute("answers") QuizAnswers answers, BindingResult errors,
                              @RequestParam(required = false) String strategy, Principal principal,
                              HttpSession session, Model model) {
        if (strategy != null && !strategy.isBlank() && !service.options().containsKey(strategy))
            errors.reject("matching.strategy", "Vui lòng chọn một chiến lược trong danh sách.");
        model.addAttribute("strategy", strategy == null || strategy.isBlank() ? service.defaultStrategy() : strategy);
        if (errors.hasErrors()) return "matching/quiz";
        var report = service.submit(answers, strategy, principal == null ? null : principal.getName());
        session.setAttribute("latestQuiz", report);
        return "redirect:/quiz/results";
    }

    @GetMapping("/results") String results(HttpSession session, Model model) {
        model.addAttribute("report", session.getAttribute("latestQuiz"));
        return "matching/results";
    }

    @GetMapping("/history") String history(Principal principal, Model model) {
        model.addAttribute("history", service.history(principal.getName()));
        return "matching/history";
    }

    @GetMapping("/history/{id}") String detail(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("report", service.detail(id, principal.getName()));
        return "matching/results";
    }
}
