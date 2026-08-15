package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JStudentGroup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupRepository extends JpaRepository<JStudentGroup, UUID> {}
