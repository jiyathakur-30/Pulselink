package com.pulselink.repository;

import com.pulselink.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUserUserIdAndIsReadFalse(Long userId);
}
