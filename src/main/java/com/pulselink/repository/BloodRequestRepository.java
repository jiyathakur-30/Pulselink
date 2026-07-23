package com.pulselink.repository;

import com.pulselink.model.BloodRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByPatientPatientId(Long patientId);
    Page<BloodRequest> findByPatientPatientId(Long patientId, Pageable pageable);
    
    long countByStatus(String status);
    
    @Query("SELECT r FROM BloodRequest r WHERE (:query IS NULL OR r.patient.user.name LIKE %:query% OR r.bloodGroup LIKE %:query%) AND (:status IS NULL OR r.status = :status)")
    Page<BloodRequest> searchAndFilterRequests(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
