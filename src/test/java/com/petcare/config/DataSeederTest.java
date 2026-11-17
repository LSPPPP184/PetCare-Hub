package com.petcare.config;

import com.petcare.domain.Role;
import com.petcare.domain.User;
import com.petcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:seed_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DataSeeder.class, DataSeederTest.EncoderConfig.class, AppointmentClockConfig.class})
class DataSeederTest {
    @Autowired private DataSeeder seeder;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private TestEntityManager entityManager;

    @BeforeEach
    void emptyUsers() {
        users.deleteAll();
        users.flush();
        entityManager.clear();
    }

    @Test
    void seedsEmptyTableOnlyOnce() {
        seeder.run();
        users.flush();
        entityManager.clear();
        var initialUsers = users.findAll();

        assertThat(initialUsers).extracting(User::getEmail, User::getRole)
            .containsExactlyInAnyOrder(
                tuple("admin@petcare.vn", Role.ADMIN),
                tuple("staff@petcare.vn", Role.STAFF),
                tuple("customer@petcare.vn", Role.CUSTOMER));
        assertThat(encoder.matches("Admin@123", users.findByEmail("admin@petcare.vn").orElseThrow().getPassword())).isTrue();
        assertThat(encoder.matches("Staff@123", users.findByEmail("staff@petcare.vn").orElseThrow().getPassword())).isTrue();
        assertThat(encoder.matches("Customer@123", users.findByEmail("customer@petcare.vn").orElseThrow().getPassword())).isTrue();

        // Tách bản chụp khỏi persistence context để phát hiện cả việc cập nhật dữ liệu cũ.
        entityManager.clear();
        seeder.run();
        users.flush();
        entityManager.clear();

        assertThat(users.findAll()).usingRecursiveComparison()
            .ignoringCollectionOrder().isEqualTo(initialUsers);
    }

    @Test
    void leavesExistingUserUntouchedWithoutAddingSampleAccounts() {
        users.saveAndFlush(new User("Khách đã có", "existing@petcare.vn",
            encoder.encode("Existing@123"), "0901234567", Role.CUSTOMER));
        entityManager.clear();
        var initialUsers = users.findAll();
        assertThat(initialUsers).hasSize(1);
        entityManager.clear();

        seeder.run();
        users.flush();
        entityManager.clear();

        assertThat(users.findAll()).usingRecursiveComparison().isEqualTo(initialUsers);
    }

    @TestConfiguration
    static class EncoderConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
