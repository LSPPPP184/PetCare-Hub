package com.petcare.repository;

import com.petcare.domain.OwnedPet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedPetRepository extends JpaRepository<OwnedPet, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "owner")
    @org.springframework.data.jpa.repository.Query("select p from OwnedPet p where (:ownerId is null or p.owner.id = :ownerId) order by p.id")
    org.springframework.data.domain.Page<OwnedPet> findList(Long ownerId, org.springframework.data.domain.Pageable pageable);

    java.util.List<OwnedPet> findByOwnerIdOrderByIdAsc(Long ownerId);
    java.util.List<OwnedPet> findAllByOrderByIdAsc();
    boolean existsByOwnerIdAndSeedKey(Long ownerId, String seedKey);

    /** Nâng cấp seed 5B cũ bằng mũi demo cố định, không dựa vào tên thú có thể sửa. */
    @org.springframework.data.jpa.repository.Query("""
        select pet from OwnedPet pet where pet.owner.id = :ownerId and pet.species = :species
        and pet.seedKey is null and exists (select v.id from VaccinationRecord v
            where v.ownedPet = pet and v.vaccineName = :marker)
        """)
    java.util.List<OwnedPet> findLegacyDemoPets(Long ownerId, com.petcare.domain.Species species, String marker);
}
