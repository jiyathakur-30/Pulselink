package com.pulselink.service;

import com.pulselink.dto.UserRegistrationDto;
import com.pulselink.model.*;
import com.pulselink.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Donor> findDonorByUser(User user) {
        return donorRepository.findByUserUserId(user.getUserId());
    }

    public Optional<Patient> findPatientByUser(User user) {
        return patientRepository.findByUserUserId(user.getUserId());
    }

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        // Fetch or create Role
        String roleName = "ROLE_" + dto.getRoleType().toUpperCase();
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        // Create User
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setStatus("ACTIVE");
        
        user = userRepository.save(user);

        // Create specific role profile
        if ("DONOR".equalsIgnoreCase(dto.getRoleType())) {
            Donor donor = new Donor();
            donor.setUser(user);
            donor.setBloodGroup(dto.getBloodGroup());
            donor.setDob(dto.getDob());
            donor.setGender(dto.getGender());
            donor.setWeight(dto.getWeight());
            donor.setAddress(dto.getAddress());
            donor.setHealthStatus("ELIGIBLE");
            donorRepository.save(donor);
        } else if ("PATIENT".equalsIgnoreCase(dto.getRoleType())) {
            Patient patient = new Patient();
            patient.setUser(user);
            patient.setBloodGroup(dto.getBloodGroup());
            patient.setDob(dto.getDob());
            patient.setGender(dto.getGender());
            patient.setMedicalConditions(dto.getMedicalConditions());
            patient.setAddress(dto.getAddress());
            patient.setEmergencyContact(dto.getEmergencyContact());
            patientRepository.save(patient);
        }

        return user;
    }

    @Transactional
    public void updateProfile(String email, String name, String phone, String address, Double weight, 
                              String medicalConditions, String emergencyContact) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setName(name);
        user.setPhone(phone);
        userRepository.save(user);

        if ("ROLE_DONOR".equals(user.getRole().getName())) {
            Donor donor = donorRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Donor profile not found"));
            donor.setAddress(address);
            donor.setWeight(weight);
            donorRepository.save(donor);
        } else if ("ROLE_PATIENT".equals(user.getRole().getName())) {
            Patient patient = patientRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));
            patient.setAddress(address);
            patient.setMedicalConditions(medicalConditions);
            patient.setEmergencyContact(emergencyContact);
            patientRepository.save(patient);
        }
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
