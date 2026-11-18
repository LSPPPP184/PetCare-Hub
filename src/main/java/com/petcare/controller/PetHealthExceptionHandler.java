package com.petcare.controller;

import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.purchase.PurchaseException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@org.springframework.core.annotation.Order(0)
@ControllerAdvice(assignableTypes = {OwnedPetController.class, StaffPetHealthController.class, DemoDateController.class})
public class PetHealthExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, InvalidStateTransitionException.class, PurchaseException.class})
    String business(RuntimeException ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect(ex.getMessage(), request, flash);
    }
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    String invalid(Exception ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect("Dữ liệu sức khỏe không hợp lệ. Vui lòng kiểm tra ngày, trạng thái và các trường bắt buộc.", request, flash);
    }
    @ExceptionHandler(ConcurrencyFailureException.class)
    String concurrent(Exception ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect("Dữ liệu vừa được thay đổi. Vui lòng mở lại hồ sơ và thử lại.", request, flash);
    }
    private String redirect(String message, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", message);
        var path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/admin/demo/health")) return "redirect:/admin/demo/health";
        if (path.startsWith("/staff/health")) return "redirect:/staff/health";
        return "redirect:/my-pets";
    }
}
