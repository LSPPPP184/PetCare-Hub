package com.petcare.repository;

import com.petcare.domain.ServiceItem;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface AppointmentServiceRepository extends JpaRepository<ServiceItem, Long> {
    @Query("select s from ServiceItem s where s.active = true and type(s) in (GroomingService, VetService) order by s.name, s.id")
    List<ServiceItem> findBookable();
}
