package com.petcare.service;
import com.petcare.domain.*;
import com.petcare.dto.RegistrationRequest;
import com.petcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Locale;
@Service @RequiredArgsConstructor
public class RegistrationService {
 private final UserRepository users;
 private final PasswordEncoder encoder;
 public boolean register(RegistrationRequest request) {
  String email=request.getEmail().strip().toLowerCase(Locale.ROOT);
  if(users.existsByEmail(email)) return false;
  try {
   users.saveAndFlush(new User(request.getFullName().strip(),email,encoder.encode(request.getPassword()),request.getPhone(),Role.CUSTOMER));
   return true;
  } catch(DataIntegrityViolationException ex) {
   if(users.existsByEmail(email)) return false;
   throw ex;
  }
 }
}
