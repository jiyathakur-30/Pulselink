package com.pulselink.repository;

import com.pulselink.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {
    Optional<BloodInventory> findByBloodGroup(String bloodGroup);
}
