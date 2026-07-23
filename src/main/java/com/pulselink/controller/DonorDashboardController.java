package com.pulselink.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.pulselink.model.*;
import com.pulselink.repository.*;
import com.pulselink.service.BloodService;
import com.pulselink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/donor")
public class DonorDashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

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

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Donor donor = donorRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Donor profile not found"));

        // Eligibility Calculation
        boolean eligible = true;
        LocalDate nextEligibleDate = LocalDate.now();
        if (donor.getLastDonationDate() != null) {
            nextEligibleDate = donor.getLastDonationDate().plusDays(56);
            eligible = LocalDate.now().isAfter(nextEligibleDate) || LocalDate.now().isEqual(nextEligibleDate);
        }

        // History
        List<Donation> donations = donationRepository.findByDonorDonorId(donor.getDonorId());
        List<Appointment> appointments = appointmentRepository.findByDonorDonorId(donor.getDonorId());
        List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());

        model.addAttribute("donor", donor);
        model.addAttribute("eligible", eligible);
        model.addAttribute("nextEligibleDate", nextEligibleDate);
        model.addAttribute("donations", donations);
        model.addAttribute("appointments", appointments);
        model.addAttribute("notifications", notifications);

        // Badge determination (Bronze, Silver, Gold)
        String badge = "Bronze Donor";
        String badgeClass = "badge-bronze";
        if (donations.size() >= 10) {
            badge = "Gold Donor";
            badgeClass = "badge-gold";
        } else if (donations.size() >= 5) {
            badge = "Silver Donor";
            badgeClass = "badge-silver";
        }
        model.addAttribute("badge", badge);
        model.addAttribute("badgeClass", badgeClass);

        return "donor/dashboard";
    }

    @PostMapping("/appointment/book")
    public String bookAppointment(@RequestParam("date") String date, @RequestParam("time") String time, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Donor donor = donorRepository.findByUserUserId(user.getUserId()).orElseThrow();
        try {
            bloodService.bookAppointment(donor, LocalDate.parse(date), time);
            return "redirect:/donor/dashboard?appointmentBooked=true";
        } catch (Exception e) {
            return "redirect:/donor/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/certificate/download/{id}")
    public ResponseEntity<InputStreamResource> downloadCertificate(@PathVariable("id") Long donationId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Donor donor = donorRepository.findByUserUserId(user.getUserId()).orElseThrow();
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("Donation record not found"));

        if (!donation.getDonor().getDonorId().equals(donor.getDonorId())) {
            return ResponseEntity.status(403).build(); // Unauthorized access
        }

        // Generate Certificate PDF
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font styles
            Font titleFont = new Font(Font.HELVETICA, 28, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 16, Font.ITALIC);
            Font bodyFont = new Font(Font.HELVETICA, 14, Font.NORMAL);
            Font nameFont = new Font(Font.HELVETICA, 22, Font.BOLD);
            Font certFont = new Font(Font.COURIER, 10, Font.NORMAL);

            Paragraph pTitle = new Paragraph("CERTIFICATE OF DONATION", titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingAfter(20);
            document.add(pTitle);

            Paragraph pSub = new Paragraph("PulseLink Blood Bank System proudly recognizes", subtitleFont);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingAfter(15);
            document.add(pSub);

            Paragraph pName = new Paragraph(donor.getUser().getName().toUpperCase(), nameFont);
            pName.setAlignment(Element.ALIGN_CENTER);
            pName.setSpacingAfter(15);
            document.add(pName);

            String text = "for selflessly donating " + donation.getUnitsDonated() + " unit(s) of " + donation.getBloodGroup() + 
                         " blood on " + donation.getDonationDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + ".\n" +
                         "Your contribution helps save lives and support critical healthcare services.";
            Paragraph pBody = new Paragraph(text, bodyFont);
            pBody.setAlignment(Element.ALIGN_CENTER);
            pBody.setSpacingAfter(30);
            document.add(pBody);

            Paragraph pCertCode = new Paragraph("Verification Code: " + donation.getCertificateCode(), certFont);
            pCertCode.setAlignment(Element.ALIGN_CENTER);
            document.add(pCertCode);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=DonationCertificate_" + donation.getCertificateCode() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Donor donor = donorRepository.findByUserUserId(user.getUserId()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("donor", donor);
        return "donor/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("address") String address,
                                @RequestParam("weight") Double weight,
                                Principal principal) {
        userService.updateProfile(principal.getName(), name, phone, address, weight, null, null);
        return "redirect:/donor/profile?success=true";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 Principal principal, Model model) {
        try {
            userService.changePassword(principal.getName(), currentPassword, newPassword);
            return "redirect:/donor/profile?passwordSuccess=true";
        } catch (Exception e) {
            return "redirect:/donor/profile?passwordError=" + e.getMessage();
        }
    }
}
