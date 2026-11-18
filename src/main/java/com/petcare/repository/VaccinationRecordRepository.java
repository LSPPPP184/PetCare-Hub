package com.petcare.repository;

import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.vaccination.VaccinationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"ownedPet", "ownedPet.owner"})
    @Query("""
        select v from VaccinationRecord v where
        (:status = 'COMPLETED' and v.stateName = com.petcare.domain.vaccination.VaccinationStatus.COMPLETED)
        or (v.stateName <> com.petcare.domain.vaccination.VaccinationStatus.COMPLETED and (
          (:status = 'OVERDUE' and v.scheduledDate < :today)
          or (:status = 'DUE_SOON' and v.scheduledDate between :today and :until)
          or (:status = 'SCHEDULED' and v.scheduledDate > :until)
          or (:status = 'ALERTS' and v.scheduledDate <= :until)))
        order by v.scheduledDate, v.id
        """)
    org.springframework.data.domain.Page<VaccinationRecord> findAlerts(String status, java.time.LocalDate today,
        java.time.LocalDate until, org.springframework.data.domain.Pageable pageable);

    long countByStateNameNotAndScheduledDateBetween(VaccinationStatus state, java.time.LocalDate start, java.time.LocalDate end);
    long countByStateNameNotAndScheduledDateBefore(VaccinationStatus state, java.time.LocalDate date);

    @Query("""
        select count(distinct v.ownedPet.id) from VaccinationRecord v
        where v.stateName <> :completed and v.scheduledDate >= :start and v.scheduledDate <= :end
        """)
    long countPetsDue(VaccinationStatus completed, java.time.LocalDate start, java.time.LocalDate end);
    @Query("""
        select count(distinct v.ownedPet.id) from VaccinationRecord v
        where v.stateName <> :completed and v.scheduledDate < :today
        """)
    long countPetsOverdue(VaccinationStatus completed, java.time.LocalDate today);
    List<VaccinationRecord> findByStateNameInOrderByScheduledDateAscIdAsc(java.util.Collection<VaccinationStatus> states);
    List<VaccinationRecord> findByStateNameNot(VaccinationStatus stateName);
    List<VaccinationRecord> findByOwnedPetIdOrderByScheduledDateAsc(Long ownedPetId);
    boolean existsByOwnedPetIdAndVaccineNameAndAnnualDoseYear(Long ownedPetId, String vaccineName, Integer annualDoseYear);
    @Query("select record.ownedPet.id from VaccinationRecord record where record.id = :id")
    Optional<Long> findOwnedPetIdById(@Param("id") Long id);
}
