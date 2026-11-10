package com.petcare.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
@Controller
public class PageController {
 @GetMapping("/") public String home() { return "home"; }
 @GetMapping("/admin/dashboard") public String admin() { return "admin"; }
 @GetMapping("/staff/dashboard") public String staff() { return "staff"; }
 @GetMapping("/403") @ResponseStatus(HttpStatus.FORBIDDEN) public String forbidden() { return "403"; }
}
