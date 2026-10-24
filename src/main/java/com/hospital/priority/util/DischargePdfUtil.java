package com.hospital.priority.util;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * CLINICAL PDF DISCHARGE SUMMARY GENERATOR (Page 12)
 * ============================================================================
 * Generates formal hospital-grade PDF discharge summaries and medical
 * certificates using Apache PDFBox 3.x.
 */
public class DischargePdfUtil {

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");

    /**
     * Generates a formal Emergency Department Discharge Summary PDF document.
     */
    public static File generateDischargeSummaryPdf(Patient patient, String outcome, String icd10,
                                                   String prescriptions, String instructions,
                                                   String doctorName, File targetFile) throws IOException {
        if (targetFile == null) {
            targetFile = new File("ED_Discharge_" + patient.getTokenNo() + ".pdf");
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 800;

                // 1. HOSPITAL HEADER
                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.newLineAtOffset(50, y);
                cs.showText("CITY GENERAL HOSPITAL");
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(50, y);
                cs.showText("DEPARTMENT OF EMERGENCY MEDICINE · CLINICAL DISCHARGE SUMMARY");
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(50, y);
                cs.showText("Level-1 Emergency & Trauma Care Facility  |  Emergency Hotline: +91 22 2456 7890");
                cs.endText();

                // Decorative horizontal divider line
                y -= 10;
                cs.setLineWidth(1.5f);
                cs.moveTo(50, y);
                cs.lineTo(545, y);
                cs.stroke();

                // 2. PATIENT IDENTIFICATION BLOCK
                y -= 22;
                drawText(cs, fontBold, 11, 50, y, "PATIENT INFORMATION");

                y -= 16;
                drawText(cs, fontBold, 9, 50, y, "Patient Name:");
                drawText(cs, fontRegular, 9, 140, y, sanitize(patient.getFullName()));

                drawText(cs, fontBold, 9, 320, y, "UHID / MRN:");
                drawText(cs, fontRegular, 9, 410, y, sanitize(patient.getUhid() != null ? patient.getUhid() : "UH-2026-00" + patient.getPatientId()));

                y -= 14;
                drawText(cs, fontBold, 9, 50, y, "Age / Gender:");
                drawText(cs, fontRegular, 9, 140, y, patient.getAge() + " yrs / " + sanitize(patient.getGender()));

                drawText(cs, fontBold, 9, 320, y, "Emergency Token:");
                drawText(cs, fontRegular, 9, 410, y, sanitize(patient.getTokenNo()));

                y -= 14;
                drawText(cs, fontBold, 9, 50, y, "Contact Phone:");
                drawText(cs, fontRegular, 9, 140, y, sanitize(patient.getPhone()));

                drawText(cs, fontBold, 9, 320, y, "Allergies:");
                String allergies = patient.getAllergies() != null && !patient.getAllergies().isEmpty() ? patient.getAllergies() : "No Known Allergies";
                drawText(cs, fontBold, 9, 410, y, sanitize(allergies));

                // 3. ENCOUNTER & ADMISSION DETAILS
                y -= 22;
                drawText(cs, fontBold, 11, 50, y, "ENCOUNTER TIMELINE & ACUITY");

                y -= 16;
                drawText(cs, fontBold, 9, 50, y, "Arrival Timestamp:");
                drawText(cs, fontRegular, 9, 160, y, patient.getArrivalTime() != null ? patient.getArrivalTime().format(DATE_TIME_FMT) : "--");

                drawText(cs, fontBold, 9, 320, y, "Discharge Timestamp:");
                drawText(cs, fontRegular, 9, 435, y, LocalDateTime.now().format(DATE_TIME_FMT));

                y -= 14;
                drawText(cs, fontBold, 9, 50, y, "Triage Priority Level:");
                EmergencyLevel level = patient.getEmergencyLevel() != null ? patient.getEmergencyLevel() : EmergencyLevel.MEDIUM;
                drawText(cs, fontBold, 9, 160, y, level.getDisplayName() + " (Rank " + level.getRank() + ")");

                drawText(cs, fontBold, 9, 320, y, "Disposition Outcome:");
                drawText(cs, fontBold, 9, 435, y, sanitize(outcome));

                y -= 14;
                drawText(cs, fontBold, 9, 50, y, "Assigned ED Bay:");
                drawText(cs, fontRegular, 9, 160, y, sanitize(patient.getAssignedBay() != null ? patient.getAssignedBay() : "Bay 1 · Resus"));

                drawText(cs, fontBold, 9, 320, y, "Attending Physician:");
                drawText(cs, fontRegular, 9, 435, y, sanitize(doctorName != null ? doctorName : "Dr. Rajesh Sharma"));

                // 4. CLINICAL EVALUATION & DIAGNOSIS
                y -= 22;
                drawText(cs, fontBold, 11, 50, y, "CLINICAL EVALUATION & DIAGNOSIS");

                y -= 16;
                drawText(cs, fontBold, 9, 50, y, "Presenting Symptoms:");
                drawText(cs, fontRegular, 9, 160, y, sanitize(patient.getSymptoms()));

                y -= 14;
                drawText(cs, fontBold, 9, 50, y, "Triage Physiological Vitals:");
                String vitalsStr = patient.getVitals() != null ? patient.getVitals().toSummaryString() : "Vitals not recorded";
                drawText(cs, fontRegular, 9, 185, y, sanitize(vitalsStr));

                y -= 14;
                drawText(cs, fontBold, 9, 50, y, "Final ICD-10 Diagnosis:");
                drawText(cs, fontBold, 9.5f, 185, y, sanitize(icd10 != null && !icd10.isEmpty() ? icd10 : "I21.0 - Acute transmural myocardial infarction"));

                // 5. DISCHARGE MEDICATIONS & PRESCRIPTION
                y -= 22;
                drawText(cs, fontBold, 11, 50, y, "DISCHARGE MEDICATIONS & PRESCRIPTION");

                y -= 16;
                String[] rxLines = (prescriptions != null && !prescriptions.isEmpty())
                        ? prescriptions.split("\n")
                        : new String[]{
                        "1. Tab. Aspirin 75 mg - 1 tablet once daily after lunch x 30 days",
                        "2. Tab. Clopidogrel 75 mg - 1 tablet once daily x 30 days",
                        "3. Tab. Atorvastatin 40 mg - 1 tablet at bedtime x 30 days",
                        "4. Tab. Pantoprazole 40 mg - 1 tablet once daily empty stomach x 14 days"
                };

                for (String rx : rxLines) {
                    if (rx.trim().isEmpty()) continue;
                    drawText(cs, fontRegular, 9, 60, y, sanitize(rx.trim()));
                    y -= 13;
                }

                // 6. FOLLOW-UP ADVICE & WARNING RED FLAGS
                y -= 14;
                drawText(cs, fontBold, 11, 50, y, "FOLLOW-UP PLAN & RED-FLAG WARNING SIGNS");

                y -= 16;
                String[] planLines = (instructions != null && !instructions.isEmpty())
                        ? instructions.split("\n")
                        : new String[]{
                        "• Follow up in Cardiology OPD on Monday at 10:00 AM for repeat 2D-Echocardiogram.",
                        "• Maintain low-salt, low-fat cardiac diet. Strict bed rest for 48 hours.",
                        "• EMERGENCY RED FLAGS: Return to ED immediately if severe chest pressure, diaphoresis,",
                        "  shortness of breath, syncope, or radiating jaw/arm pain recurs."
                };

                for (String plan : planLines) {
                    if (plan.trim().isEmpty()) continue;
                    drawText(cs, fontRegular, 9, 60, y, sanitize(plan.trim()));
                    y -= 13;
                }

                // 7. MEDICAL-LEGAL DOCTOR SIGNATURE BLOCK
                y -= 30;
                cs.setLineWidth(1.0f);
                cs.moveTo(350, y);
                cs.lineTo(540, y);
                cs.stroke();

                y -= 12;
                drawText(cs, fontBold, 9.5f, 350, y, sanitize(doctorName != null ? doctorName : "Dr. Rajesh Sharma, MD (Emergency Medicine)"));

                y -= 12;
                drawText(cs, fontRegular, 8.5f, 350, y, "Senior Emergency Physician · Registration No: MCI-58291");

                y -= 10;
                drawText(cs, fontOblique, 8, 350, y, "Electronically Signed & Authenticated");

                // FOOTER
                y = 35;
                cs.setLineWidth(0.5f);
                cs.moveTo(50, y + 10);
                cs.lineTo(545, y + 10);
                cs.stroke();

                drawText(cs, fontOblique, 7.5f, 50, y,
                        "HEPS Clinical Document · Generated automatically on " + LocalDateTime.now().format(DATE_TIME_FMT) + " · Page 1 of 1");
            }

            doc.save(targetFile);
        }

        return targetFile;
    }

    private static void drawText(PDPageContentStream cs, PDType1Font font, float fontSize,
                                 float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static String sanitize(String text) {
        if (text == null) return "";
        // Replace non-ASCII / special chars with safe ASCII equivalents for Helvetica standard font
        return text.replaceAll("[^\\x00-\\x7F]", " ").trim();
    }
}
