package com.pulselink.service;

import com.pulselink.model.*;
import com.pulselink.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BloodService {

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DonorRepository donorRepository;

    // --- Inventory ---

    public List<BloodInventory> getInventory() {
        return inventoryRepository.findAll();
    }

    public Optional<BloodInventory> getInventoryByGroup(String bloodGroup) {
        return inventoryRepository.findByBloodGroup(bloodGroup);
    }

    @Transactional
    public void updateStock(String bloodGroup, int units) {
        BloodInventory inventory = inventoryRepository.findByBloodGroup(bloodGroup)
                .orElseGet(() -> new BloodInventory(bloodGroup, 0));
        inventory.setUnitsAvailable(Math.max(0, inventory.getUnitsAvailable() + units));
        inventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    // --- Donations ---

    @Transactional
    public Donation recordDonation(Long donorId, String bloodGroup, int units, LocalDate date) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new IllegalArgumentException("Donor not found"));

        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setBloodGroup(bloodGroup);
        donation.setUnitsDonated(units);
        donation.setDonationDate(date);
        donation.setStatus("COMPLETED");
        donation.setCertificateCode("CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        donation = donationRepository.save(donation);

        // Update donor's last donation date
        donor.setLastDonationDate(date);
        donorRepository.save(donor);

        // Add to blood inventory
        updateStock(bloodGroup, units);

        // Send Notification
        User user = donor.getUser();
        notificationRepository.save(new Notification(user, 
            "Thank you for donating " + units + " units of " + bloodGroup + "! Your certificate " + donation.getCertificateCode() + " is ready."));

        return donation;
    }

    public List<Donation> getDonorDonations(Long donorId) {
        return donationRepository.findByDonorDonorId(donorId);
    }

    // --- Requests ---

    @Transactional
    public BloodRequest createRequest(Patient patient, String bloodGroup, int units, String justification, LocalDate requiredDate) {
        BloodRequest request = new BloodRequest();
        request.setPatient(patient);
        request.setBloodGroup(bloodGroup);
        request.setUnitsRequested(units);
        request.setJustification(justification);
        request.setRequiredDate(requiredDate);
        request.setStatus("PENDING");
        request.setRequestDate(LocalDateTime.now());

        request = requestRepository.save(request);

        // Notify user
        notificationRepository.save(new Notification(patient.getUser(), 
            "Your blood request for " + units + " units of " + bloodGroup + " has been submitted. Status: PENDING"));

        return request;
    }

    @Transactional
    public void approveRequest(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        BloodInventory inventory = inventoryRepository.findByBloodGroup(request.getBloodGroup())
                .orElseThrow(() -> new IllegalArgumentException("No inventory found for blood group " + request.getBloodGroup()));

        if (inventory.getUnitsAvailable() < request.getUnitsRequested()) {
            throw new IllegalArgumentException("Insufficient inventory available! Requested: " + 
                request.getUnitsRequested() + ", Available: " + inventory.getUnitsAvailable());
        }

        // Deduct inventory
        updateStock(request.getBloodGroup(), -request.getUnitsRequested());

        request.setStatus("APPROVED");
        requestRepository.save(request);

        // Notify user
        notificationRepository.save(new Notification(request.getPatient().getUser(), 
            "Congratulations! Your request for " + request.getUnitsRequested() + " units of " + request.getBloodGroup() + " has been APPROVED."));
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }

        request.setStatus("REJECTED");
        requestRepository.save(request);

        // Notify user
        notificationRepository.save(new Notification(request.getPatient().getUser(), 
            "Your request for " + request.getUnitsRequested() + " units of " + request.getBloodGroup() + " has been REJECTED."));
    }

    public List<BloodRequest> getPatientRequests(Long patientId) {
        return requestRepository.findByPatientPatientId(patientId);
    }

    // --- Appointments ---

    @Transactional
    public Appointment bookAppointment(Donor donor, LocalDate date, String time) {
        // Simple eligibility check (donors must wait 56 days / 8 weeks between donations)
        if (donor.getLastDonationDate() != null) {
            LocalDate nextEligibleDate = donor.getLastDonationDate().plusDays(56);
            if (date.isBefore(nextEligibleDate)) {
                throw new IllegalArgumentException("You are not eligible to donate yet. Next eligible date: " + nextEligibleDate);
            }
        }

        Appointment appt = new Appointment();
        appt.setDonor(donor);
        appt.setAppointmentDate(date);
        appt.setAppointmentTime(time);
        appt.setStatus("SCHEDULED");

        appt = appointmentRepository.save(appt);

        // Notify user
        notificationRepository.save(new Notification(donor.getUser(), 
            "Your donation appointment is scheduled for " + date + " at " + time + "."));

        return appt;
    }

    @Transactional
    public void completeAppointment(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appt.setStatus("COMPLETED");
        appointmentRepository.save(appt);

        // Record a standard donation (e.g. 1 unit)
        recordDonation(appt.getDonor().getDonorId(), appt.getDonor().getBloodGroup(), 1, appt.getAppointmentDate());
    }

    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appt.setStatus("CANCELLED");
        appointmentRepository.save(appt);

        notificationRepository.save(new Notification(appt.getDonor().getUser(), 
            "Your donation appointment for " + appt.getAppointmentDate() + " was cancelled."));
    }
}
