package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, String> {
  Optional<JUser> findFirstByIdStartingWithOrderByIdDesc(String idPrefix);

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, String id);
}
