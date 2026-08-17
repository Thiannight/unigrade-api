package com.unigrade.api.security;

import com.unigrade.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String userId) {
    return userRepository
        .findById(userId)
        .map(AppUserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("No user with id " + userId));
  }
}
