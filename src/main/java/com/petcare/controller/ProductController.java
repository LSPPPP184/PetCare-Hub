package com.petcare.controller;
import com.petcare.service.ProductService;
import com.petcare.dto.*;
import com.petcare.domain.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@Controller @RequiredArgsConstructor public class ProductController  {
    private final ProductService service;
    private static final Map<ProductType,List<String>> FIELDS=Map.of(ProductType.PET,List.of("species","breed","ageMonths","gender","size","healthNote","status"),ProductType.ACCESSORY,List.of("brand","stockQuantity","category"),ProductType.GROOMING,List.of("durationMinutes","petSizeSurcharge"),ProductType.VET,List.of("durationMinutes","doctorName"),ProductType.MONTHLY,List.of("durationMinutes","durationMonths","includedUses","includedServices"));
    @ModelAttribute("types") ProductType[] types() {
        return ProductType.values();
    }
    @ModelAttribute("speciesOptions") Species[] species() {
        return Species.values();
    }
    @ModelAttribute("statusOptions") PetStatus[] statuses() {
        return PetStatus.values();
    }
    @GetMapping("/products") String catalog(@RequestParam(defaultValue="") String group,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean descending,Model m) {
        m.addAttribute("products",service.search(group,q,page,descending,false));
        return "products/list";
    }
    @GetMapping("/products/{id}") String detail(@PathVariable Long id,Model m) {
        m.addAttribute("product",service.detail(id));
        return "products/detail";
    }
    @GetMapping("/staff/products") String manage(@RequestParam(defaultValue="") String group,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean descending,Model m) {
        m.addAttribute("products",service.search(group,q,page,descending,true));
        m.addAttribute("staff",true);
        return "products/list";
    }
    @GetMapping("/staff/products/new") String create(@RequestParam(defaultValue="PET") ProductType type,Model m) {
        var f=new ProductForm();
        f.setProfile(new PetProfileForm());
        f.setType(type);
        m.addAttribute("form",f);
        m.addAttribute("fields",FIELDS.get(type));
        return "products/form";
    }
    @GetMapping("/staff/products/{id}/edit") String edit(@PathVariable Long id,Model m) {
        var f=service.get(id).toForm();
        m.addAttribute("form",f);
        m.addAttribute("fields",FIELDS.get(f.getType()));
        m.addAttribute("id",id);
        return "products/form";
    }
    @PostMapping("/staff/products/save") String save(@RequestParam(required=false) Long id,@Valid @ModelAttribute("form") ProductForm f,BindingResult errors,Model m) {
        if(!errors.hasErrors()) {
            try {
                service.save(id,f);
            } catch(ProductVersionConflictException ex) {
                errors.reject("product.version.conflict",ex.getMessage());
            }
        }
        if(errors.hasErrors()) {
            if (f.getProfile() == null) f.setProfile(new PetProfileForm());
            m.addAttribute("id",id);
            m.addAttribute("fields", f.getType() == null ? List.of() : FIELDS.getOrDefault(f.getType(), List.of()));
            return "products/form";
        }
        return "redirect:/staff/products";
    }
    @PostMapping("/staff/products/{id}/hide") String hide(@PathVariable Long id) {
        service.hide(id);
        return "redirect:/staff/products";
    }
}
