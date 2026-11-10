package com.petcare.controller;
import com.petcare.dto.RegistrationRequest;
import com.petcare.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequiredArgsConstructor
public class AuthController {
 private final RegistrationService registrationService;
 @GetMapping("/login") public String login() { return "login"; }
 @GetMapping("/register") public String register(Model model) {
  model.addAttribute("registration",new RegistrationRequest()); return "register";
 }
 @PostMapping("/register") public String register(@Valid @ModelAttribute("registration") RegistrationRequest request,
    BindingResult errors, RedirectAttributes flash) {
  if(errors.hasErrors()) return "register";
  if(!registrationService.register(request)) {
   errors.rejectValue("email","duplicate","Email đã được sử dụng."); return "register";
  }
  flash.addFlashAttribute("success","Đăng ký thành công. Vui lòng đăng nhập.");
  return "redirect:/login";
 }
}
