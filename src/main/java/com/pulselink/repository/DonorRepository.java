package com.pulselink.repository;

import com.pulselink.model.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    Optional<Donor> findByUserEmail(String email);
    Optional<Donor> findByUserUserId(Long userId);
    
    @Query("SELECT d FROM Donor d WHERE :query IS NULL OR d.user.name LIKE %:query% OR d.user.email LIKE %:query% OR d.bloodGroup LIKE %:query%")
    Page<Donor> searchDonors(@Param("query") String query, Pageable pageable);
}
