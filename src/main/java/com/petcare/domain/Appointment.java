package com.petcare.domain;

import com.petcare.domain.appointment.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity @Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_staff_time", columnList = "staff_id,start_time,end_time"),
    @Index(name = "idx_appointment_customer", columnList = "customer_id")})
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id") private User customer;
    @Column(nullable = false, length = 100) private String petName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Species petSpecies;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id") private ServiceItem service;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "staff_id") private User staff;
    @Column(nullable = false) private LocalDateTime startTime;
    @Column(nullable = false) private LocalDateTime endTime;
    @Column(length = 1000) private String note;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal price;
    // Cho phép null ở DB để nâng cấp lịch cũ; getter diễn giải là chưa giảm giá.
    @Column(precision = 19, scale = 2) private BigDecimal discountAmount;
    @Column(precision = 19, scale = 2) private BigDecimal finalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AppointmentStatus stateName;

    /** Chụp giá và thời lượng lúc đặt; thời điểm truyền vào là giờ Việt Nam. */
    public static Appointment create(User customer, String petName, Species species, ServiceItem service,
                                     User staff, LocalDateTime start, String note) {
        if (customer == null || customer.getRole() != Role.CUSTOMER || staff == null || staff.getRole() != Role.STAFF)
            throw new AppointmentException("Khách hàng hoặc nhân viên thực hiện không hợp lệ.");
        if (petName == null || petName.isBlank() || petName.length() > 100 || species == null)
            throw new AppointmentException("Vui lòng nhập tên thú cưng tối đa 100 ký tự và chọn loài.");
        if (note != null && note.length() > 1000) throw new AppointmentException("Ghi chú tối đa 1000 ký tự.");
        AppointmentServices.require(service);
        if (start == null || service.getDurationMinutes() == null || service.getDurationMinutes() <= 0)
            throw new AppointmentException("Thời gian hoặc thời lượng dịch vụ không hợp lệ.");
        var a = new Appointment();
        a.customer = customer; a.petName = petName.strip(); a.petSpecies = species; a.service = service;
        a.staff = staff; a.startTime = start; a.endTime = start.plusMinutes(service.getDurationMinutes());
        a.note = note == null ? "" : note.strip();
        try { a.price = service.calculatePrice().setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new AppointmentException("Giá dịch vụ chỉ được có tối đa hai chữ số thập phân."); }
        if (a.price.signum() < 0 || a.price.precision() > 19) throw new AppointmentException("Giá dịch vụ không hợp lệ.");
        a.stateName = new BookedState().book(a);
        return a;
    }
    @Transient public AppointmentState getState() { return AppointmentStateFactory.from(stateName); }
    public BigDecimal getDiscountAmount() { return discountAmount == null ? BigDecimal.ZERO : discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount == null ? price : finalAmount; }
    /** Chụp giảm giá lúc tạo, không cho thay đổi sau khi lịch hẹn đã lưu. */
    public void snapshotDiscount(BigDecimal discount) {
        if (id != null || discount == null || discount.signum() < 0 || discount.compareTo(price) > 0)
            throw new AppointmentException("Khoản giảm giá không hợp lệ hoặc lịch hẹn đã được lưu.");
        discountAmount = discount.setScale(2, RoundingMode.UNNECESSARY);
        finalAmount = price.subtract(discountAmount);
    }
    public void book() { stateName = getState().book(this); }
    public void start() { stateName = getState().start(this); }
    public void finish() { stateName = getState().finish(this); }
    public void cancel() { stateName = getState().cancel(this); }
}
