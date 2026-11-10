package com.petcare.service;
import com.petcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Locale;
@Service @RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
 private final UserRepository users;
 @Override public UserDetails loadUserByUsername(String email) {
  var user=users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
    .orElseThrow(()->new UsernameNotFoundException("Email hoặc mật khẩu không đúng."));
  return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
   .password(user.getPassword()).roles(user.getRole().name()).build();
 }
}
