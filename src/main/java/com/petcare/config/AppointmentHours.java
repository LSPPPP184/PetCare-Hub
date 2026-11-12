package com.petcare.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import java.time.*;
import java.util.*;

@Component @ConfigurationProperties(prefix = "petcare.appointments") @Validated @Getter @Setter
public class AppointmentHours {
    @NotNull private LocalTime open = LocalTime.of(8, 0);
    @NotNull private LocalTime close = LocalTime.of(18, 0);
    @NotEmpty private Set<DayOfWeek> workingDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
    @Min(1) @Max(600) private int slotMinutes = 30;
    @AssertTrue(message = "Giờ mở cửa phải trước giờ đóng cửa.")
    public boolean isWindowValid() { return open != null && close != null && open.isBefore(close); }
}
