package com.petcare.event;

import com.petcare.domain.vaccination.VaccinationStateFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Observer ghi log sau commit để không thông báo thay đổi đã rollback; có thể bổ sung thông báo về sau. */
@Component
@Slf4j
public class VaccinationStatusChangedListener {
    @TransactionalEventListener
    public void onStatusChanged(VaccinationStatusChangedEvent event) {
        String label = new VaccinationStateFactory(VaccinationStateFactory.DEFAULT_DUE_SOON_DAYS)
            .from(event.getCurrentState()).getLabel();
        log.info("Lịch tiêm #{}: {} — {}, ngày hẹn {}, ngày kiểm tra {}.", event.getRecordId(),
            event.getVaccineName(), label, event.getScheduledDate(), event.getObservedOn());
    }
}
