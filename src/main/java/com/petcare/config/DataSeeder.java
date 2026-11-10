package com.petcare.config;
import com.petcare.domain.*;
import com.petcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
 private final UserRepository users;
 private final PasswordEncoder encoder;
 @Override @Transactional public void run(String... args) {
  if(users.count()!=0) return;
  users.save(new User("Quản trị viên","admin@petcare.vn",encoder.encode("Admin@123"),"",Role.ADMIN));
  users.save(new User("Nhân viên","staff@petcare.vn",encoder.encode("Staff@123"),"",Role.STAFF));
  users.save(new User("Khách hàng","customer@petcare.vn",encoder.encode("Customer@123"),"",Role.CUSTOMER));
 }
}
