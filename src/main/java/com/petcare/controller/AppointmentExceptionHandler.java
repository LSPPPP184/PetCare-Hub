package com.petcare.controller;

import com.petcare.domain.appointment.AppointmentException;
import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.purchase.PurchaseException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Chuyển lỗi lịch hẹn thành thông báo tiếng Việt, không điều hướng theo URL do khách truyền. */
@ControllerAdvice(assignableTypes = {AppointmentController.class, StaffAppointmentController.class})
public class AppointmentExceptionHandler {
    @ExceptionHandler({AppointmentException.class, InvalidStateTransitionException.class, PurchaseException.class})
    String business(RuntimeException ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect(ex.getMessage(), request, flash);
    }
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    String invalid(Exception ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect("Dữ liệu lịch hẹn không hợp lệ. Vui lòng chọn lại dịch vụ, ngày và khung giờ.", request, flash);
    }
    @ExceptionHandler(ConcurrencyFailureException.class)
    String concurrent(ConcurrencyFailureException ex, HttpServletRequest request, RedirectAttributes flash) {
        return redirect("Khung giờ vừa được thay đổi. Vui lòng chọn lại khung giờ trống.", request, flash);
    }
    private String redirect(String message, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", message);
        var path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/staff/appointments")) return "redirect:/staff/appointments";
        if (path.endsWith("/cancel")) return "redirect:/appointments";
        return "redirect:/appointments/services";
    }
}
