package com.petcare.repository;

import com.petcare.domain.Appointment;
import com.petcare.domain.appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    long countByStartTimeGreaterThanEqualAndStartTimeLessThanAndStateNameNot(
        LocalDateTime start, LocalDateTime end, AppointmentStatus excluded);
    @Query("""
        select a from Appointment a where a.stateName <> :cancelled
        and a.startTime < :end and a.endTime > :start
        """)
    List<Appointment> findOccupied(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                   @Param("cancelled") AppointmentStatus cancelled);
    @Query("""
        select count(a) from Appointment a where a.staff.id = :staffId and a.stateName <> :cancelled
        and a.startTime < :end and a.endTime > :start
        """)
    long countOverlaps(@Param("staffId") Long staffId, @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end, @Param("cancelled") AppointmentStatus cancelled);
    @EntityGraph(attributePaths = {"customer", "staff", "service"})
    List<Appointment> findByCustomerIdOrderByStartTimeDescIdDesc(Long customerId);
    @EntityGraph(attributePaths = {"customer", "staff", "service"})
    @Query("""
        select a from Appointment a where (:staffId is null or a.staff.id = :staffId)
        and a.startTime >= :start and a.startTime < :end order by a.startTime, a.id
        """)
    List<Appointment> findSchedule(@Param("staffId") Long staffId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
