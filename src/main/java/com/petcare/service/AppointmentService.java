package com.petcare.service;

import com.petcare.config.AppointmentHours;
import com.petcare.domain.*;
import com.petcare.domain.appointment.*;
import com.petcare.dto.*;
import com.petcare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/**
 * Điều phối đặt lịch và State. Khóa nhân viên bằng CommerceLocks rồi kiểm tra trùng
 * trong cùng transaction; hai khách chọn cùng khung giờ chỉ một người đặt thành công.
 * STAFF quản lý lịch được giao, ADMIN quản lý tất cả; mọi STAFF được nhận cả hai loại dịch vụ.
 */
@Service @Transactional(readOnly = true) @RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final AppointmentServiceRepository services;
    private final ProductRepository products;
    private final UserRepository users;
    private final CommerceAccess access;
    private final CommerceLocks locks;
    private final AppointmentPolicy policy;
    private final AppointmentHours hours;

    public LocalDate today() { return policy.today(); }
    public List<AppointmentServiceView> services() { return services.findBookable().stream().map(AppointmentServiceView::from).toList(); }
    private ServiceItem service(Long id) {
        if (id == null) throw new AppointmentException("Vui lòng chọn dịch vụ.");
        return AppointmentServices.require(products.findById(id).orElseThrow(() -> new AppointmentException("Không tìm thấy dịch vụ.")));
    }
    private User staff(Long id) {
        if (id == null) throw new AppointmentException("Vui lòng chọn nhân viên.");
        var staff = users.findById(id).orElseThrow(() -> new AppointmentException("Không tìm thấy nhân viên."));
        if (staff.getRole() != Role.STAFF) throw new AppointmentException("Người thực hiện phải là nhân viên STAFF.");
        return staff;
    }
    private void validateSlot(ServiceItem service, Long staffId, LocalDateTime start) {
        policy.validateBooking(start, service.getDurationMinutes());
        if (appointments.countOverlaps(staffId, start, policy.endTime(start, service.getDurationMinutes()), AppointmentStatus.CANCELLED) > 0)
            throw new AppointmentException("Nhân viên đã có lịch trong khung giờ này. Vui lòng chọn khung giờ khác.");
    }
    public AppointmentSlots availableSlots(Long serviceId, LocalDate requestedDate) {
        access.currentCustomer();
        var service = service(serviceId);
        var date = requestedDate == null ? policy.today() : requestedDate;
        var slots = new ArrayList<AppointmentSlots.Slot>();
        if (!date.isBefore(policy.today()) && policy.isWorkingDay(date)) {
            var occupied = appointments.findOccupied(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), AppointmentStatus.CANCELLED);
            var employees = users.findByRoleOrderByIdAsc(Role.STAFF);
            var now = policy.now();
            for (var start = date.atTime(hours.getOpen()); !start.isAfter(date.atTime(hours.getClose())); start = start.plusMinutes(hours.getSlotMinutes())) {
                var end = policy.endTime(start, service.getDurationMinutes());
                if (start.isBefore(now) || end.isAfter(date.atTime(hours.getClose()))) continue;
                for (var employee : employees) {
                    var candidateStart = start;
                    boolean conflict = occupied.stream().anyMatch(a -> a.getStaff().getId().equals(employee.getId())
                        && AppointmentPolicy.overlaps(candidateStart, end, a.getStartTime(), a.getEndTime()));
                    if (!conflict) slots.add(new AppointmentSlots.Slot(employee.getId(), employee.getFullName(), start, end));
                }
            }
        }
        return new AppointmentSlots(service.getId(), service.getName(), service.getDurationMinutes(), service.calculatePrice(), date, List.copyOf(slots));
    }
    public AppointmentSelection selection(Long serviceId, Long staffId, LocalDateTime start) {
        access.currentCustomer();
        var service = service(serviceId);
        var employee = staff(staffId);
        validateSlot(service, employee.getId(), start);
        return new AppointmentSelection(service.getId(), service.getName(), employee.getId(), employee.getFullName(),
            start, policy.endTime(start, service.getDurationMinutes()), service.calculatePrice());
    }
    @Transactional public Long book(AppointmentForm form) {
        var customer = access.currentCustomer();
        staff(form.getStaffId());
        var employee = locks.lock(User.class, form.getStaffId());
        if (employee.getRole() != Role.STAFF) throw new AppointmentException("Người thực hiện phải là nhân viên STAFF.");
        if (form.getServiceId() == null) throw new AppointmentException("Vui lòng chọn dịch vụ.");
        var service = AppointmentServices.require(locks.lock(Product.class, form.getServiceId()));
        validateSlot(service, employee.getId(), form.getStartTime());
        return appointments.save(Appointment.create(customer, form.getPetName(), form.getPetSpecies(), service,
            employee, form.getStartTime(), form.getNote())).getId();
    }
    private Appointment find(Long id) {
        return appointments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn."));
    }
    private Appointment locked(Long id) {
        var appointment = find(id);
        locks.lock(User.class, appointment.getStaff().getId());
        return locks.lock(Appointment.class, id);
    }
    private AppointmentView view(Appointment a) { return AppointmentView.from(a, policy.canCustomerCancel(a.getStartTime())); }
    public List<AppointmentView> mine() {
        return appointments.findByCustomerIdOrderByStartTimeDescIdDesc(access.currentCustomer().getId()).stream().map(this::view).toList();
    }
    public AppointmentView myDetail(Long id) {
        var a = find(id);
        access.customer(a.getCustomer().getId());
        return view(a);
    }
    @Transactional public void cancelMine(Long id) {
        access.customer(find(id).getCustomer().getId());
        var a = locked(id);
        policy.validateCustomerCancellation(a.getStartTime());
        a.cancel();
    }
    private void checkAssigned(User actor, Appointment a) {
        if (actor.getRole() != Role.ADMIN && !actor.getId().equals(a.getStaff().getId()))
            throw new AccessDeniedException("Bạn chỉ được quản lý lịch hẹn được giao cho mình.");
    }
    public List<AppointmentView> schedule(LocalDate date) {
        var actor = access.currentStaff();
        var day = date == null ? policy.today() : date;
        return appointments.findSchedule(actor.getRole() == Role.ADMIN ? null : actor.getId(), day.atStartOfDay(), day.plusDays(1).atStartOfDay())
            .stream().map(this::view).toList();
    }
    public AppointmentView staffDetail(Long id) {
        var actor = access.currentStaff();
        var a = find(id);
        checkAssigned(actor, a);
        return view(a);
    }
    private Appointment assignedAndLocked(Long id) {
        var actor = access.currentStaff();
        checkAssigned(actor, find(id));
        var a = locked(id);
        checkAssigned(actor, a);
        return a;
    }
    @Transactional public void start(Long id) { assignedAndLocked(id).start(); }
    @Transactional public void finish(Long id) { assignedAndLocked(id).finish(); }
    /** Nhân viên có thể hủy lịch BOOKED để xử lý sự cố; mốc hai tiếng chỉ áp dụng cho khách. */
    @Transactional public void cancelByStaff(Long id) { assignedAndLocked(id).cancel(); }
}
