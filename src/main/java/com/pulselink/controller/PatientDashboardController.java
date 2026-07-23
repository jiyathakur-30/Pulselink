package com.pulselink.controller;

import com.pulselink.model.*;
import com.pulselink.repository.*;
import com.pulselink.service.BloodService;
import com.pulselink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/patient")
public class PatientDashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BloodService bloodService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));

        // Availability info
        List<BloodInventory> availability = inventoryRepository.findAll();

        // Requests
        List<BloodRequest> requests = requestRepository.findByPatientPatientId(patient.getPatientId());
        List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());

        model.addAttribute("patient", patient);
        model.addAttribute("availability", availability);
        model.addAttribute("requests", requests);
        model.addAttribute("notifications", notifications);

        return "patient/dashboard";
    }

    @PostMapping("/request/create")
    public String createRequest(@RequestParam("bloodGroup") String bloodGroup,
                                @RequestParam("units") int units,
                                @RequestParam("justification") String justification,
                                @RequestParam("requiredDate") String requiredDate,
                                Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserUserId(user.getUserId()).orElseThrow();
        try {
            bloodService.createRequest(patient, bloodGroup, units, justification, LocalDate.parse(requiredDate));
            return "redirect:/patient/dashboard?requestCreated=true";
        } catch (Exception e) {
            return "redirect:/patient/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserUserId(user.getUserId()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("patient", patient);
        return "patient/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("address") String address,
                                @RequestParam("medicalConditions") String medicalConditions,
                                @RequestParam("emergencyContact") String emergencyContact,
                                Principal principal) {
        userService.updateProfile(principal.getName(), name, phone, address, null, medicalConditions, emergencyContact);
        return "redirect:/patient/profile?success=true";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 Principal principal) {
        try {
            userService.changePassword(principal.getName(), currentPassword, newPassword);
            return "redirect:/patient/profile?passwordSuccess=true";
        } catch (Exception e) {
            return "redirect:/patient/profile?passwordError=" + e.getMessage();
        }
    }
}
