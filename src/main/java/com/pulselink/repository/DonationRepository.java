package com.pulselink.repository;

import com.pulselink.model.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorDonorId(Long donorId);
    Page<Donation> findByDonorDonorId(Long donorId, Pageable pageable);
    
    long countByDonationDate(LocalDate date);
    
    @Query("SELECT d FROM Donation d WHERE (:query IS NULL OR d.donor.user.name LIKE %:query% OR d.bloodGroup LIKE %:query%) AND (:status IS NULL OR d.status = :status)")
    Page<Donation> searchAndFilterDonations(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
