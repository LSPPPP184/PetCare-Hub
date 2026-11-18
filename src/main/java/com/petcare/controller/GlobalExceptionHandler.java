package com.petcare.controller;

import com.petcare.domain.order.InvalidStateTransitionException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Lớp dự phòng toàn cục sau các handler nghiệp vụ; không đưa chi tiết nội bộ lên trang lỗi/log. */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    ModelAndView conflict() { return page(HttpStatus.CONFLICT, "Dữ liệu đã được thay đổi, vui lòng tải lại trang"); }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ModelAndView transition() { return page(HttpStatus.CONFLICT, "Trạng thái hiện tại không cho phép thao tác này. Vui lòng tải lại trang."); }

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    ModelAndView missing() { return new ModelAndView("error/404", HttpStatus.NOT_FOUND); }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class, HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ModelAndView validation() { return page(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường bắt buộc và định dạng đã nhập."); }

    @ExceptionHandler(AccessDeniedException.class)
    ModelAndView forbidden() { return new ModelAndView("forward:/403", HttpStatus.FORBIDDEN); }

    @ExceptionHandler(ResponseStatusException.class)
    ModelAndView status(ResponseStatusException ex) {
        var status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == HttpStatus.NOT_FOUND) return missing();
        if (status == HttpStatus.FORBIDDEN) return forbidden();
        if (status == null || status.is5xxServerError()) return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
        return page(status, "Không thể xử lý yêu cầu. Vui lòng kiểm tra dữ liệu và thử lại.");
    }

    @ExceptionHandler({org.springframework.web.HttpRequestMethodNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotAcceptableException.class})
    ModelAndView protocol(Exception ex, jakarta.servlet.http.HttpServletResponse response) {
        var error = (org.springframework.web.ErrorResponse) ex;
        error.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        return page(HttpStatus.valueOf(error.getStatusCode().value()),
            "Phương thức hoặc định dạng yêu cầu không được hỗ trợ. Vui lòng sử dụng liên kết và biểu mẫu trên trang.");
    }

    @ExceptionHandler(Exception.class)
    ModelAndView unexpected(Exception ex) {
        // Không log message, đối số hay stack trace có thể chứa mật khẩu/dữ liệu form/SQL.
        LOG.error("Lỗi xử lý yêu cầu: {}", ex.getClass().getName());
        return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ModelAndView page(HttpStatus status, String message) {
        var view = new ModelAndView("error/request", status);
        view.addObject("code", status.value()); view.addObject("description", message);
        return view;
    }
}
