package com.petcare.repository;

import com.petcare.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MatchingPetRepository extends JpaRepository<Pet, Long> {
    @Query("select p from Pet p join fetch p.profile where p.active = true and p.status = com.petcare.domain.PetStatus.AVAILABLE")
    List<Pet> findCandidates();
    Optional<Pet> findByProfileSeedKey(String seedKey);
}
