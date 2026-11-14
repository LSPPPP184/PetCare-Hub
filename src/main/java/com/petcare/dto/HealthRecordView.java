package com.petcare.dto;

import com.petcare.domain.HealthRecord;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthRecordView(LocalDate date, String type, String description, BigDecimal weight, String vetName) {
    public static HealthRecordView from(HealthRecord record) {
        return new HealthRecordView(record.getDate(), record.getType(), record.getDescription(), record.getWeight(), record.getVetName());
    }
}
