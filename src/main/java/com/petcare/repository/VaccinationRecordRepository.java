package com.petcare.repository;

import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.vaccination.VaccinationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {
    List<VaccinationRecord> findByStateNameInOrderByScheduledDateAscIdAsc(java.util.Collection<VaccinationStatus> states);
    List<VaccinationRecord> findByStateNameNot(VaccinationStatus stateName);
    List<VaccinationRecord> findByOwnedPetIdOrderByScheduledDateAsc(Long ownedPetId);
    boolean existsByOwnedPetIdAndVaccineNameAndAnnualDoseYear(Long ownedPetId, String vaccineName, Integer annualDoseYear);
    @Query("select record.ownedPet.id from VaccinationRecord record where record.id = :id")
    Optional<Long> findOwnedPetIdById(@Param("id") Long id);
}
