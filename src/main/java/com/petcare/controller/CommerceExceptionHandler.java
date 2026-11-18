package com.petcare.controller;

import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.purchase.PurchaseException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Chuyển lỗi nghiệp vụ thành flash, chỉ điều hướng đến các đường dẫn nội bộ cố định. */
@org.springframework.core.annotation.Order(0)
@ControllerAdvice(assignableTypes = {CartController.class, OrderController.class, StaffOrderController.class})
public class CommerceExceptionHandler {
    @ExceptionHandler({PurchaseException.class, InvalidStateTransitionException.class})
    String business(RuntimeException ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect(ex.getMessage(), request, flash);
    }
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    String invalidInput(Exception ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect("Dữ liệu không hợp lệ. Vui lòng kiểm tra số lượng, mã sản phẩm hoặc trạng thái đã chọn.", request, flash);
    }
    @ExceptionHandler(ConcurrencyFailureException.class)
    String concurrentUpdate(ConcurrencyFailureException ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect("Dữ liệu vừa được thay đổi. Vui lòng tải lại trang và thử lại.", request, flash);
    }
    private String redirect(String message, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", message);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/staff/orders")) return "redirect:/staff/orders";
        if (path.startsWith("/orders")) return "redirect:/orders";
        return "redirect:/cart";
    }
}
