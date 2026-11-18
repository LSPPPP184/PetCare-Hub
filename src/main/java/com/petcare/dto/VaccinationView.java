package com.petcare.dto;

import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.vaccination.VaccinationStatus;
import java.time.LocalDate;
import java.util.Map;

public record VaccinationView(Long id, long version, Long petId, String petName, String ownerName, String vaccineName,
                              LocalDate scheduledDate, LocalDate administeredDate, String note,
                              VaccinationStatus status, String label, String badgeClass,
                              boolean canComplete, boolean canReschedule) {
    private static final Map<VaccinationStatus, String> COLORS = Map.of(
        VaccinationStatus.SCHEDULED, "text-bg-secondary", VaccinationStatus.DUE_SOON, "text-bg-warning",
        VaccinationStatus.OVERDUE, "text-bg-danger", VaccinationStatus.COMPLETED, "text-bg-success");
    public static VaccinationView from(VaccinationRecord record) {
        return from(record, record.getStateName());
    }
    /** Trạng thái hiển thị tính theo ngày, không sửa State lưu trữ khi đọc. */
    public static VaccinationView from(VaccinationRecord record, LocalDate today, int dueSoonDays) {
        var status = record.getStateName() == VaccinationStatus.COMPLETED ? VaccinationStatus.COMPLETED
            : record.getScheduledDate().isBefore(today) ? VaccinationStatus.OVERDUE
            : record.getScheduledDate().isAfter(today.plusDays(dueSoonDays)) ? VaccinationStatus.SCHEDULED : VaccinationStatus.DUE_SOON;
        return from(record, status);
    }
    private static VaccinationView from(VaccinationRecord record, VaccinationStatus status) {
        var pet = record.getOwnedPet(); var state = new com.petcare.domain.vaccination.VaccinationStateFactory(7).from(status);
        return new VaccinationView(record.getId(), record.getVersion(), pet.getId(), pet.getName(), pet.getOwner().getFullName(),
            record.getVaccineName(), record.getScheduledDate(), record.getAdministeredDate(), record.getNote(),
            status, state.getLabel(), COLORS.get(status), state.canComplete(), state.canReschedule());
    }
}
