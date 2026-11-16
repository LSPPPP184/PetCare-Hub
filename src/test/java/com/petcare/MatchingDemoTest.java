package com.petcare;

import com.petcare.config.MatchingDemoSeeder;
import com.petcare.domain.*;
import com.petcare.dto.ProductForm;
import com.petcare.repository.MatchingPetRepository;
import com.petcare.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:matching_demo;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "demo"})
class MatchingDemoTest {
    @Autowired MatchingDemoSeeder seeder;
    @Autowired MatchingPetRepository pets;
    @Autowired ProductService products;
    @Autowired PlatformTransactionManager transactions;
    private <T> T db(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }

    @Test void stableKeysDiverseProfilesVietnameseAndNoResetOnRerun() {
        var initial = pets.findCandidates();
        var demo = initial.stream().filter(p -> p.getProfile().getSeedKey().startsWith("matching-")).toList();
        assertThat(demo).hasSize(8);
        assertThat(demo).extracting(Pet::getSpecies).contains(Species.DOG, Species.CAT, Species.OTHER);
        assertThat(demo).extracting(p -> p.getProfile().getEnergyLevel()).contains(1, 2, 3, 4, 5);
        assertThat(demo).extracting(p -> p.getProfile().getSizeLevel()).contains(1, 2, 3, 4, 5);
        assertThat(demo).extracting(p -> p.getProfile().isChildFriendly()).contains(true, false);
        assertThat(demo).extracting(p -> p.getProfile().getSheddingLevel()).contains(1, 2, 3, 4, 5);
        assertThat(pets.findAll()).allSatisfy(p -> assertThat(p.getProfile()).isNotNull());
        var poodle = pets.findByProfileSeedKey("matching-poodle").orElseThrow();
        assertThat(poodle.getName()).isEqualTo("Bông – chó Poodle");
        assertThat(poodle.getDescription()).contains("Thú cưng minh họa ghép đôi");
        var form = products.get(poodle.getId()).toForm(); form.setName("Tên đã sửa vẫn giữ dấu");
        form.getProfile().setEnergyLevel(5); products.save(poodle.getId(), form);
        db(() -> { ReflectionTestUtils.setField(pets.findById(poodle.getId()).orElseThrow(), "status", PetStatus.SOLD); return null; });
        long before = pets.count(); seeder.seed(); seeder.seed();
        assertThat(pets.count()).isEqualTo(before);
        var restored = pets.findByProfileSeedKey("matching-poodle").orElseThrow();
        assertThat(restored.getId()).isEqualTo(poodle.getId());
        assertThat(restored.getName()).isEqualTo("Tên đã sửa vẫn giữ dấu");
        assertThat(restored.getStatus()).isEqualTo(PetStatus.SOLD);
        assertThat(restored.getProfile().getEnergyLevel()).isEqualTo(5);
        assertThat(pets.findAll()).extracting(Pet::getId).containsExactlyInAnyOrderElementsOf(initial.stream().map(Pet::getId).toList());
    }

    @Test void legacyPetWithoutProfileGetsOneOnlyAndKeepsCatalog() {
        var f = new ProductForm(); f.setName("Thú cũ cần hồ sơ");
        var pet = new Pet(); pet.update(f); ReflectionTestUtils.setField(pet, "profile", null);
        Long id = pets.saveAndFlush(pet).getId();
        seeder.seed();
        var restored = pets.findById(id).orElseThrow();
        assertThat(restored.getName()).isEqualTo("Thú cũ cần hồ sơ");
        assertThat(restored.getProfile().getSeedKey()).isEqualTo("matching-existing-" + id);
        Long profileId = restored.getProfile().getId(); seeder.seed();
        assertThat(pets.findById(id).orElseThrow().getProfile().getId()).isEqualTo(profileId);
        pets.deleteById(id);
    }
}
