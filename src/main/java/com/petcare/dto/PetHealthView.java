package com.petcare.dto;

import java.util.List;

public record PetHealthView(OwnedPetView pet, List<VaccinationView> vaccinations, List<HealthRecordView> history) {}
