package com.pulselink.repository;

import com.pulselink.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserEmail(String email);
    Optional<Patient> findByUserUserId(Long userId);
    
    @Query("SELECT p FROM Patient p WHERE :query IS NULL OR p.user.name LIKE %:query% OR p.user.email LIKE %:query% OR p.bloodGroup LIKE %:query%")
    Page<Patient> searchPatients(@Param("query") String query, Pageable pageable);
}
