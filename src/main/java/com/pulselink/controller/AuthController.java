package com.pulselink.controller;

import com.pulselink.dto.UserRegistrationDto;
import com.pulselink.model.BloodInventory;
import com.pulselink.repository.BloodInventoryRepository;
import com.pulselink.repository.DonationRepository;
import com.pulselink.repository.DonorRepository;
import com.pulselink.repository.PatientRepository;
import com.pulselink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DonationRepository donationRepository;

    @GetMapping("/")
    public String landingPage(Model model) {
        // Fetch stats dynamically
        long donorCount = donorRepository.count();
        long patientCount = patientRepository.count();
        long donationCount = donationRepository.count();
        
        List<BloodInventory> inventory = inventoryRepository.findAll();
        int totalBloodUnits = inventory.stream().mapToInt(BloodInventory::getUnitsAvailable).sum();

        model.addAttribute("donorCount", donorCount);
        model.addAttribute("patientCount", patientCount);
        model.addAttribute("donationCount", donationCount);
        model.addAttribute("totalBloodUnits", totalBloodUnits);
        
        return "landing";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto registrationDto, 
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerUser(registrationDto);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        if (userService.findByEmail(email).isPresent()) {
            model.addAttribute("successMessage", "Password reset instructions sent to your email!");
        } else {
            model.addAttribute("errorMessage", "Email address not found!");
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("email") String email, 
                                      @RequestParam("password") String password, Model model) {
        try {
            // Simulated reset password (updates in DB)
            userService.changePassword(email, password, password); // just set it directly or update
            return "redirect:/login?reset=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Invalid email address!");
            return "reset-password";
        }
    }
}
