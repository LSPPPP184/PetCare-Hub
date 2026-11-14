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
        var pet = record.getOwnedPet(); var state = record.getState();
        return new VaccinationView(record.getId(), record.getVersion(), pet.getId(), pet.getName(), pet.getOwner().getFullName(),
            record.getVaccineName(), record.getScheduledDate(), record.getAdministeredDate(), record.getNote(),
            record.getStateName(), state.getLabel(), COLORS.get(record.getStateName()), state.canComplete(), state.canReschedule());
    }
}
