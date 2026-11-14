package com.petcare;

import com.petcare.domain.*;
import jakarta.persistence.EntityManager;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class UnicodePersistenceTest {
    @Autowired EntityManager em;
    @Autowired Environment environment;
    @Autowired PasswordEncoder encoder;

    @Test @Transactional
    void vietnameseTextSurvivesFlushAndReload() {
        var owner = new User("Tuấn Đạt", "unicode-test@petcare.vn", encoder.encode("TestOnly@123"), "", Role.CUSTOMER);
        em.persist(owner);
        var today = LocalDate.of(2026, 11, 14);
        var pet = OwnedPet.create(owner, "Lucky lớn", Species.DOG, "", today.minusYears(5), "", "Đặt hàng", today);
        em.persist(pet);
        em.flush();
        em.clear();
        assertThat(em.find(User.class, owner.getId()).getFullName()).isEqualTo("Tuấn Đạt");
        var restored = em.find(OwnedPet.class, pet.getId());
        assertThat(restored.getName()).isEqualTo("Lucky lớn");
        assertThat(restored.getNote()).isEqualTo("Đặt hàng");
    }

    /** Kiểm tra DDL SQL Server bằng metadata, không cần kết nối SQL Server thật. */
    @Test void sqlServerStringColumnsUseNationalizedTypesAndKeepLengths() {
        var nationalized = environment.getRequiredProperty("spring.jpa.properties.hibernate.use_nationalized_character_data", Boolean.class);
        assertThat(nationalized).isTrue();
        var registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.use_nationalized_character_data", nationalized).build();
        try {
            var sources = new MetadataSources(registry);
            em.getMetamodel().getEntities().forEach(entity -> sources.addAnnotatedClass(entity.getJavaType()));
            var metadata = sources.buildMetadata();
            for (var entity : metadata.getEntityBindings()) {
                for (var property : entity.getPropertyClosure()) {
                    if (property.getType().getReturnedClass() == String.class) {
                        for (var column : property.getColumns()) {
                            assertThat(column.getSqlType(metadata)).as(entity.getEntityName() + "." + property.getName())
                                .startsWith("nvarchar(");
                        }
                    }
                }
            }
            assertThat(metadata.getEntityBinding(User.class.getName()).getProperty("fullName").getColumns().getFirst()
                .getSqlType(metadata)).isEqualTo("nvarchar(100)");
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
