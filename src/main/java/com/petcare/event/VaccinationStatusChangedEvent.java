package com.petcare.event;

import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.vaccination.VaccinationStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDate;

/**
 * Observer: domain đăng ký sự kiện, Spring Data phát khi save; listener không gắn vào State.
 * Chụp trạng thái/ngày/vaccine tại lúc chuyển để các thay đổi tiếp theo không sửa nội dung sự kiện.
 */
@Getter
public class VaccinationStatusChangedEvent extends ApplicationEvent {
    private final VaccinationStatus previousState;
    private final VaccinationStatus currentState;
    private final LocalDate observedOn;
    private final LocalDate scheduledDate;
    private final String vaccineName;

    public VaccinationStatusChangedEvent(VaccinationRecord record, VaccinationStatus previousState,
                                         VaccinationStatus currentState, LocalDate observedOn) {
        super(record);
        this.previousState = previousState;
        this.currentState = currentState;
        this.observedOn = observedOn;
        this.scheduledDate = record.getScheduledDate();
        this.vaccineName = record.getVaccineName();
    }
    /** ID đã được JPA cấp khi Spring Data phát sự kiện, kể cả hồ sơ mới. */
    public Long getRecordId() { return ((VaccinationRecord) getSource()).getId(); }
}
