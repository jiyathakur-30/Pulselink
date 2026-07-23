package com.pulselink.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pulselink.model.*;
import com.pulselink.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- PDF GENERATION ---

    public ByteArrayInputStream generatePdfReport(String type) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Paragraph title = new Paragraph("PulseLink - " + type.toUpperCase() + " REPORT");
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Subtitle
            Paragraph generatedAt = new Paragraph("Generated on: " + java.time.LocalDateTime.now().format(DATETIME_FORMATTER));
            generatedAt.setAlignment(Element.ALIGN_LEFT);
            generatedAt.setSpacingAfter(20);
            document.add(generatedAt);

            if ("STOCK".equalsIgnoreCase(type)) {
                PdfPTable table = new PdfPTable(3);
                table.setWidthPercentage(100);
                table.addCell("Blood Group");
                table.addCell("Units Available");
                table.addCell("Last Updated");

                List<BloodInventory> items = inventoryRepository.findAll();
                for (BloodInventory item : items) {
                    table.addCell(item.getBloodGroup());
                    table.addCell(String.valueOf(item.getUnitsAvailable()));
                    table.addCell(item.getLastUpdated().format(DATETIME_FORMATTER));
                }
                document.add(table);

            } else if ("DONOR".equalsIgnoreCase(type)) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.addCell("Name");
                table.addCell("Email");
                table.addCell("Phone");
                table.addCell("Blood Group");
                table.addCell("Last Donation");

                List<Donor> items = donorRepository.findAll();
                for (Donor item : items) {
                    table.addCell(item.getUser().getName());
                    table.addCell(item.getUser().getEmail());
                    table.addCell(item.getUser().getPhone());
                    table.addCell(item.getBloodGroup());
                    table.addCell(item.getLastDonationDate() != null ? item.getLastDonationDate().format(DATE_FORMATTER) : "N/A");
                }
                document.add(table);

            } else if ("PATIENT".equalsIgnoreCase(type)) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.addCell("Name");
                table.addCell("Email");
                table.addCell("Phone");
                table.addCell("Blood Group");
                table.addCell("Emergency Contact");

                List<Patient> items = patientRepository.findAll();
                for (Patient item : items) {
                    table.addCell(item.getUser().getName());
                    table.addCell(item.getUser().getEmail());
                    table.addCell(item.getUser().getPhone());
                    table.addCell(item.getBloodGroup());
                    table.addCell(item.getEmergencyContact() != null ? item.getEmergencyContact() : "N/A");
                }
                document.add(table);

            } else if ("DONATION".equalsIgnoreCase(type)) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.addCell("Donor Name");
                table.addCell("Blood Group");
                table.addCell("Units");
                table.addCell("Donation Date");
                table.addCell("Certificate");

                List<Donation> items = donationRepository.findAll();
                for (Donation item : items) {
                    table.addCell(item.getDonor().getUser().getName());
                    table.addCell(item.getBloodGroup());
                    table.addCell(String.valueOf(item.getUnitsDonated()));
                    table.addCell(item.getDonationDate().format(DATE_FORMATTER));
                    table.addCell(item.getCertificateCode());
                }
                document.add(table);

            } else if ("REQUEST".equalsIgnoreCase(type)) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.addCell("Patient Name");
                table.addCell("Blood Group");
                table.addCell("Units");
                table.addCell("Status");
                table.addCell("Required Date");

                List<BloodRequest> items = requestRepository.findAll();
                for (BloodRequest item : items) {
                    table.addCell(item.getPatient().getUser().getName());
                    table.addCell(item.getBloodGroup());
                    table.addCell(String.valueOf(item.getUnitsRequested()));
                    table.addCell(item.getStatus());
                    table.addCell(item.getRequiredDate().format(DATE_FORMATTER));
                }
                document.add(table);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- EXCEL GENERATION ---

    public ByteArrayInputStream generateExcelReport(String type) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(type + " Report");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.PINK.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            if ("STOCK".equalsIgnoreCase(type)) {
                String[] columns = {"Blood Group", "Units Available", "Last Updated"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                List<BloodInventory> items = inventoryRepository.findAll();
                int rowIdx = 1;
                for (BloodInventory item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getBloodGroup());
                    row.createCell(1).setCellValue(item.getUnitsAvailable());
                    row.createCell(2).setCellValue(item.getLastUpdated().format(DATETIME_FORMATTER));
                }

            } else if ("DONOR".equalsIgnoreCase(type)) {
                String[] columns = {"Name", "Email", "Phone", "Blood Group", "Gender", "Last Donation", "Address"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                List<Donor> items = donorRepository.findAll();
                int rowIdx = 1;
                for (Donor item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getUser().getName());
                    row.createCell(1).setCellValue(item.getUser().getEmail());
                    row.createCell(2).setCellValue(item.getUser().getPhone());
                    row.createCell(3).setCellValue(item.getBloodGroup());
                    row.createCell(4).setCellValue(item.getGender());
                    row.createCell(5).setCellValue(item.getLastDonationDate() != null ? item.getLastDonationDate().format(DATE_FORMATTER) : "N/A");
                    row.createCell(6).setCellValue(item.getAddress());
                }

            } else if ("PATIENT".equalsIgnoreCase(type)) {
                String[] columns = {"Name", "Email", "Phone", "Blood Group", "Gender", "Emergency Contact", "Address"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                List<Patient> items = patientRepository.findAll();
                int rowIdx = 1;
                for (Patient item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getUser().getName());
                    row.createCell(1).setCellValue(item.getUser().getEmail());
                    row.createCell(2).setCellValue(item.getUser().getPhone());
                    row.createCell(3).setCellValue(item.getBloodGroup());
                    row.createCell(4).setCellValue(item.getGender());
                    row.createCell(5).setCellValue(item.getEmergencyContact());
                    row.createCell(6).setCellValue(item.getAddress());
                }

            } else if ("DONATION".equalsIgnoreCase(type)) {
                String[] columns = {"Donor Name", "Blood Group", "Units Donated", "Donation Date", "Status", "Certificate Code"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                List<Donation> items = donationRepository.findAll();
                int rowIdx = 1;
                for (Donation item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getDonor().getUser().getName());
                    row.createCell(1).setCellValue(item.getBloodGroup());
                    row.createCell(2).setCellValue(item.getUnitsDonated());
                    row.createCell(3).setCellValue(item.getDonationDate().format(DATE_FORMATTER));
                    row.createCell(4).setCellValue(item.getStatus());
                    row.createCell(5).setCellValue(item.getCertificateCode());
                }

            } else if ("REQUEST".equalsIgnoreCase(type)) {
                String[] columns = {"Patient Name", "Blood Group", "Units Requested", "Status", "Request Date", "Required Date"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                List<BloodRequest> items = requestRepository.findAll();
                int rowIdx = 1;
                for (BloodRequest item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getPatient().getUser().getName());
                    row.createCell(1).setCellValue(item.getBloodGroup());
                    row.createCell(2).setCellValue(item.getUnitsRequested());
                    row.createCell(3).setCellValue(item.getStatus());
                    row.createCell(4).setCellValue(item.getRequestDate().format(DATETIME_FORMATTER));
                    row.createCell(5).setCellValue(item.getRequiredDate().format(DATE_FORMATTER));
                }
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
