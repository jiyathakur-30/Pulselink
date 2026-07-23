package com.pulselink.controller;

import com.pulselink.model.*;
import com.pulselink.repository.*;
import com.pulselink.service.BloodService;
import com.pulselink.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BloodService bloodService;

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        // Cards data
        model.addAttribute("totalDonors", donorRepository.count());
        model.addAttribute("totalPatients", patientRepository.count());
        model.addAttribute("totalBloodUnits", inventoryRepository.findAll().stream().mapToInt(BloodInventory::getUnitsAvailable).sum());
        model.addAttribute("pendingRequests", requestRepository.countByStatus("PENDING"));
        model.addAttribute("approvedRequests", requestRepository.countByStatus("APPROVED"));
        model.addAttribute("todayDonations", donationRepository.countByDonationDate(LocalDate.now()));

        // Recent records
        model.addAttribute("recentDonors", donorRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "donorId"))).getContent());
        model.addAttribute("recentRequests", requestRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "requestId"))).getContent());
        model.addAttribute("recentDonations", donationRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "donationId"))).getContent());

        // For charts data
        List<BloodInventory> stockList = inventoryRepository.findAll();
        model.addAttribute("stockList", stockList);

        return "admin/dashboard";
    }

    @GetMapping("/donors")
    public String donors(Model model, 
                         @RequestParam(value = "query", required = false) String query,
                         @RequestParam(value = "page", defaultValue = "0") int page) {
        Page<Donor> donorPage = donorRepository.searchDonors(query, PageRequest.of(page, 10, Sort.by("donorId").descending()));
        model.addAttribute("donorPage", donorPage);
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        return "admin/donors";
    }

    @GetMapping("/patients")
    public String patients(Model model,
                           @RequestParam(value = "query", required = false) String query,
                           @RequestParam(value = "page", defaultValue = "0") int page) {
        Page<Patient> patientPage = patientRepository.searchPatients(query, PageRequest.of(page, 10, Sort.by("patientId").descending()));
        model.addAttribute("patientPage", patientPage);
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        return "admin/patients";
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("inventoryList", inventoryRepository.findAll());
        return "admin/inventory";
    }

    @PostMapping("/inventory/add")
    public String addStock(@RequestParam("bloodGroup") String bloodGroup, @RequestParam("units") int units) {
        bloodService.updateStock(bloodGroup, units);
        return "redirect:/admin/inventory?success=true";
    }

    @GetMapping("/requests")
    public String requests(Model model,
                           @RequestParam(value = "query", required = false) String query,
                           @RequestParam(value = "status", required = false) String status,
                           @RequestParam(value = "page", defaultValue = "0") int page) {
        if (status != null && status.isEmpty()) status = null;
        Page<BloodRequest> requestPage = requestRepository.searchAndFilterRequests(query, status, PageRequest.of(page, 10, Sort.by("requestId").descending()));
        model.addAttribute("requestPage", requestPage);
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        return "admin/requests";
    }

    @PostMapping("/requests/approve/{id}")
    public String approveRequest(@PathVariable("id") Long id) {
        try {
            bloodService.approveRequest(id);
            return "redirect:/admin/requests?approved=true";
        } catch (Exception e) {
            return "redirect:/admin/requests?error=" + e.getMessage();
        }
    }

    @PostMapping("/requests/reject/{id}")
    public String rejectRequest(@PathVariable("id") Long id) {
        bloodService.rejectRequest(id);
        return "redirect:/admin/requests?rejected=true";
    }

    @GetMapping("/donations")
    public String donations(Model model,
                            @RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "page", defaultValue = "0") int page) {
        Page<Donation> donationPage = donationRepository.searchAndFilterDonations(query, "COMPLETED", PageRequest.of(page, 10, Sort.by("donationId").descending()));
        model.addAttribute("donationPage", donationPage);
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("donorsList", donorRepository.findAll());
        return "admin/donations";
    }

    @PostMapping("/donations/record")
    public String recordDonation(@RequestParam("donorId") Long donorId, @RequestParam("units") int units) {
        try {
            Donor donor = donorRepository.findById(donorId).orElseThrow();
            bloodService.recordDonation(donorId, donor.getBloodGroup(), units, LocalDate.now());
            return "redirect:/admin/donations?recorded=true";
        } catch (Exception e) {
            return "redirect:/admin/donations?error=" + e.getMessage();
        }
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("reportsList", reportRepository.findAllByOrderByGeneratedAtDesc());
        return "admin/reports";
    }

    @GetMapping("/reports/generate")
    public String generateReport(@RequestParam("type") String type, @RequestParam("format") String format, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        String fileName = type.toLowerCase() + "_report_" + System.currentTimeMillis() + "." + ("PDF".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        
        Report report = new Report(type, user, "/admin/reports/download?file=" + fileName + "&type=" + type + "&format=" + format);
        reportRepository.save(report);

        return "redirect:/admin/reports?generated=true";
    }

    @GetMapping("/reports/download")
    public ResponseEntity<InputStreamResource> downloadReport(@RequestParam("type") String type, @RequestParam("format") String format) {
        ByteArrayInputStream bis;
        String fileName = type.toLowerCase() + "_report";
        MediaType mediaType;

        if ("PDF".equalsIgnoreCase(format)) {
            bis = reportService.generatePdfReport(type);
            fileName += ".pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else {
            bis = reportService.generateExcelReport(type);
            fileName += ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(bis));
    }
}
