package com.pulselink.repository;

import com.pulselink.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDonorDonorId(Long donorId);
    Page<Appointment> findByDonorDonorId(Long donorId, Pageable pageable);
    List<Appointment> findByStatus(String status);
}
