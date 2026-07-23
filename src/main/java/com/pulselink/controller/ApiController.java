package com.pulselink.controller;

import com.pulselink.model.BloodInventory;
import com.pulselink.model.Donation;
import com.pulselink.model.BloodRequest;
import com.pulselink.model.Notification;
import com.pulselink.repository.BloodInventoryRepository;
import com.pulselink.repository.DonationRepository;
import com.pulselink.repository.BloodRequestRepository;
import com.pulselink.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/stats/blood-groups")
    public List<Map<String, Object>> getBloodGroupStats() {
        return inventoryRepository.findAll().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("group", item.getBloodGroup());
                    map.put("units", item.getUnitsAvailable());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/stats/trends")
    public Map<String, Object> getMonthlyTrends() {
        // Group donations by month for the last 6 months
        List<Donation> donations = donationRepository.findAll();
        List<BloodRequest> requests = requestRepository.findAll();

        Map<String, Integer> donationMap = new LinkedHashMap<>();
        Map<String, Integer> requestMap = new LinkedHashMap<>();

        // Initialize last 6 months
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate targetDate = now.minusMonths(i);
            String monthName = targetDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + targetDate.getYear();
            donationMap.put(monthName, 0);
            requestMap.put(monthName, 0);
        }

        // Fill donation data
        for (Donation d : donations) {
            LocalDate date = d.getDonationDate();
            String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getYear();
            if (donationMap.containsKey(monthName)) {
                donationMap.put(monthName, donationMap.get(monthName) + d.getUnitsDonated());
            }
        }

        // Fill request data
        for (BloodRequest r : requests) {
            LocalDate date = r.getRequestDate().toLocalDate();
            String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getYear();
            if (requestMap.containsKey(monthName)) {
                requestMap.put(monthName, requestMap.get(monthName) + r.getUnitsRequested());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", new ArrayList<>(donationMap.keySet()));
        response.put("donations", new ArrayList<>(donationMap.values()));
        response.put("requests", new ArrayList<>(requestMap.values()));

        return response;
    }

    @PostMapping("/notifications/mark-read/{id}")
    public Map<String, Object> markNotificationRead(@PathVariable("id") Long notificationId) {
        Optional<Notification> opt = notificationRepository.findById(notificationId);
        Map<String, Object> response = new HashMap<>();
        if (opt.isPresent()) {
            Notification notification = opt.get();
            notification.setIsRead(true);
            notificationRepository.save(notification);
            response.put("success", true);
        } else {
            response.put("success", false);
            response.put("message", "Notification not found");
        }
        return response;
    }
}
