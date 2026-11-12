package com.petcare.repository;
import com.petcare.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User,Long> {
 Optional<User> findByEmail(String email);
 boolean existsByEmail(String email);
 java.util.List<User> findByRoleOrderByIdAsc(com.petcare.domain.Role role);
}
