package com.petcare.dto;

import com.petcare.domain.OwnedPet;
import com.petcare.domain.Species;
import java.time.LocalDate;

/** Snapshot trong giao dịch, tránh truy cập quan hệ lazy hoặc lộ User từ giao diện. */
public record OwnedPetView(Long id, long version, String name, Species species, String breed,
                           LocalDate birthDate, String gender, String note, String ownerName) {
    public static OwnedPetView from(OwnedPet pet) {
        return new OwnedPetView(pet.getId(), pet.getVersion(), pet.getName(), pet.getSpecies(), pet.getBreed(),
            pet.getBirthDate(), pet.getGender(), pet.getNote(), pet.getOwner().getFullName());
    }
    public OwnedPetForm toForm() {
        var form = new OwnedPetForm();
        form.setVersion(version); form.setName(name); form.setSpecies(species); form.setBirthDate(birthDate);
        form.setBreed(breed); form.setGender(gender); form.setNote(note);
        return form;
    }
}
