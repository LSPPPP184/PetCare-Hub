package com.petcare.repository;

import com.petcare.domain.OwnedPet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedPetRepository extends JpaRepository<OwnedPet, Long> {}
