package com.pulselink.config;

import com.pulselink.dto.UserRegistrationDto;
import com.pulselink.model.*;
import com.pulselink.repository.*;
import com.pulselink.service.BloodService;
import com.pulselink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BloodService bloodService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Roles
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role("ROLE_ADMIN"));
            roleRepository.save(new Role("ROLE_DONOR"));
            roleRepository.save(new Role("ROLE_PATIENT"));
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();

        // 2. Seed Admin
        if (userRepository.findByEmail("admin@pulselink.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@pulselink.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setName("System Admin");
            admin.setPhone("+1234567890");
            admin.setRole(adminRole);
            admin.setStatus("ACTIVE");
            userRepository.save(admin);
        }

        // 3. Seed Blood Inventory (if empty)
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        if (inventoryRepository.count() == 0) {
            for (String bg : bloodGroups) {
                inventoryRepository.save(new BloodInventory(bg, 10 + new Random().nextInt(15)));
            }
        }

        // 4. Seed special Demo Donor
        if (userRepository.findByEmail("donor@pulselink.com").isEmpty()) {
            UserRegistrationDto donorDto = new UserRegistrationDto();
            donorDto.setEmail("donor@pulselink.com");
            donorDto.setPassword("Donor@123");
            donorDto.setName("David Miller");
            donorDto.setPhone("+15550100");
            donorDto.setRoleType("DONOR");
            donorDto.setBloodGroup("O+");
            donorDto.setDob(LocalDate.of(1990, 5, 15));
            donorDto.setGender("Male");
            donorDto.setWeight(75.5);
            donorDto.setAddress("456 Health St, Care City");
            userService.registerUser(donorDto);
        }

        // 5. Seed special Demo Patient
        if (userRepository.findByEmail("patient@pulselink.com").isEmpty()) {
            UserRegistrationDto patientDto = new UserRegistrationDto();
            patientDto.setEmail("patient@pulselink.com");
            patientDto.setPassword("Patient@123");
            patientDto.setName("Sarah Jenkins");
            patientDto.setPhone("+15550200");
            patientDto.setRoleType("PATIENT");
            patientDto.setBloodGroup("A-");
            patientDto.setDob(LocalDate.of(1993, 8, 22));
            patientDto.setGender("Female");
            patientDto.setAddress("789 Medical Lane, Aid Town");
            patientDto.setMedicalConditions("Anemia");
            patientDto.setEmergencyContact("John Jenkins (+15550201)");
            userService.registerUser(patientDto);
        }

        // 6. Seed extra Donors (20 - 30)
        String[] maleNames = {"James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles"};
        String[] femaleNames = {"Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia", "Rodriguez", "Wilson"};
        
        Random rand = new Random();
        if (donorRepository.count() < 15) {
            for (int i = 1; i <= 20; i++) {
                String gender = rand.nextBoolean() ? "Male" : "Female";
                String firstName = "Male".equals(gender) ? maleNames[rand.nextInt(10)] : femaleNames[rand.nextInt(10)];
                String lastName = lastNames[rand.nextInt(10)];
                String name = firstName + " " + lastName;
                String email = "donor" + i + "@pulselink.com";
                
                if (userRepository.findByEmail(email).isEmpty()) {
                    UserRegistrationDto dto = new UserRegistrationDto();
                    dto.setEmail(email);
                    dto.setPassword("Donor@123");
                    dto.setName(name);
                    dto.setPhone("+155590" + String.format("%02d", i));
                    dto.setRoleType("DONOR");
                    dto.setBloodGroup(bloodGroups[rand.nextInt(8)]);
                    dto.setDob(LocalDate.of(1975 + rand.nextInt(25), 1 + rand.nextInt(11), 1 + rand.nextInt(28)));
                    dto.setGender(gender);
                    dto.setWeight(50.0 + rand.nextDouble() * 40.0);
                    dto.setAddress(100 + i + " Donor Way, Red Cross Area");
                    userService.registerUser(dto);
                }
            }
        }

        // 7. Seed extra Patients (15)
        if (patientRepository.count() < 10) {
            for (int i = 1; i <= 15; i++) {
                String gender = rand.nextBoolean() ? "Male" : "Female";
                String firstName = "Male".equals(gender) ? maleNames[rand.nextInt(10)] : femaleNames[rand.nextInt(10)];
                String lastName = lastNames[rand.nextInt(10)];
                String name = firstName + " " + lastName;
                String email = "patient" + i + "@pulselink.com";

                if (userRepository.findByEmail(email).isEmpty()) {
                    UserRegistrationDto dto = new UserRegistrationDto();
                    dto.setEmail(email);
                    dto.setPassword("Patient@123");
                    dto.setName(name);
                    dto.setPhone("+155580" + String.format("%02d", i));
                    dto.setRoleType("PATIENT");
                    dto.setBloodGroup(bloodGroups[rand.nextInt(8)]);
                    dto.setDob(LocalDate.of(1980 + rand.nextInt(25), 1 + rand.nextInt(11), 1 + rand.nextInt(28)));
                    dto.setGender(gender);
                    dto.setAddress(200 + i + " Patient Blvd, Clinic Heights");
                    dto.setMedicalConditions(rand.nextBoolean() ? "General Surgery" : "Accident Trauma Recovery");
                    dto.setEmergencyContact("Relative Name (+15558000)");
                    userService.registerUser(dto);
                }
            }
        }

        // 8. Seed sample historical Donations
        if (donationRepository.count() == 0) {
            List<Donor> donors = donorRepository.findAll();
            for (int i = 0; i < Math.min(15, donors.size()); i++) {
                Donor donor = donors.get(i);
                LocalDate donationDate = LocalDate.now().minusDays(10 + rand.nextInt(100));
                
                Donation donation = new Donation();
                donation.setDonor(donor);
                donation.setBloodGroup(donor.getBloodGroup());
                donation.setUnitsDonated(1 + rand.nextInt(2));
                donation.setDonationDate(donationDate);
                donation.setStatus("COMPLETED");
                donation.setCertificateCode("CERT-" + (100000 + rand.nextInt(900000)));
                donationRepository.save(donation);

                // update stock
                bloodService.updateStock(donor.getBloodGroup(), donation.getUnitsDonated());
            }
        }

        // 9. Seed sample Blood Requests
        if (requestRepository.count() == 0) {
            List<Patient> patients = patientRepository.findAll();
            String[] justifications = {"Emergency Heart Surgery", "Severe Anemia treatment", "Post-partum hemorrhage recovery", "Chemotherapy support", "Accident trauma blood replacement"};
            String[] statuses = {"PENDING", "APPROVED", "REJECTED"};
            
            for (int i = 0; i < Math.min(10, patients.size()); i++) {
                Patient patient = patients.get(i);
                
                BloodRequest req = new BloodRequest();
                req.setPatient(patient);
                req.setBloodGroup(patient.getBloodGroup());
                req.setUnitsRequested(1 + rand.nextInt(3));
                req.setJustification(justifications[rand.nextInt(justifications.length)]);
                req.setStatus(statuses[rand.nextInt(statuses.length)]);
                req.setRequestDate(LocalDateTime.now().minusDays(1 + rand.nextInt(10)));
                req.setRequiredDate(LocalDate.now().plusDays(2 + rand.nextInt(5)));
                requestRepository.save(req);
            }
        }

        // 10. Seed sample Appointments
        if (appointmentRepository.count() == 0) {
            List<Donor> donors = donorRepository.findAll();
            String[] times = {"09:00 AM", "10:30 AM", "11:00 AM", "02:00 PM", "03:30 PM"};
            for (int i = 0; i < Math.min(5, donors.size()); i++) {
                Appointment appt = new Appointment();
                appt.setDonor(donors.get(i));
                appt.setAppointmentDate(LocalDate.now().plusDays(1 + rand.nextInt(7)));
                appt.setAppointmentTime(times[rand.nextInt(times.length)]);
                appt.setStatus("SCHEDULED");
                appointmentRepository.save(appt);
            }
        }

        // 11. Seed notifications for Admin and Demo users
        if (notificationRepository.count() == 0) {
            User admin = userRepository.findByEmail("admin@pulselink.com").orElse(null);
            User donor = userRepository.findByEmail("donor@pulselink.com").orElse(null);
            User patient = userRepository.findByEmail("patient@pulselink.com").orElse(null);

            if (admin != null) {
                notificationRepository.save(new Notification(admin, "New pending blood requests require approval."));
                notificationRepository.save(new Notification(admin, "Weekly stock report is ready."));
            }
            if (donor != null) {
                notificationRepository.save(new Notification(donor, "Reminder: Your donation appointment is scheduled for next week."));
                notificationRepository.save(new Notification(donor, "Congratulations! You earned your first 'Bronze Donor' badge."));
            }
            if (patient != null) {
                notificationRepository.save(new Notification(patient, "Your request for blood has been submitted successfully."));
            }
        }
    }
}
