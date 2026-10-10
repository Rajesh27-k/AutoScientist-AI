package com.autoscientist.analysis.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
public class PdfExportService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 41, 59));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.ITALIC, new Color(100, 116, 139));
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(15, 23, 42));
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85));
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(30, 41, 59));
    private static final Font DISCLAIMER_FONT = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(153, 27, 27));

    public byte[] generatePdfReport(String title, String markdownContent) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Document Header Banner
            Paragraph header = new Paragraph("AUTOSCIENTIST AI", TITLE_FONT);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph tagline = new Paragraph("From Raw Data to Evidence-Based Scientific Discovery", SUBTITLE_FONT);
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(15);
            document.add(tagline);

            // Divider Line
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            PdfPCell divCell = new PdfPCell();
            divCell.setBorder(Rectangle.BOTTOM);
            divCell.setBorderColor(new Color(203, 213, 225));
            divCell.setBorderWidth(1.5f);
            divCell.setPadding(0);
            divider.addCell(divCell);
            document.add(divider);

            // Title
            Paragraph reportTitle = new Paragraph(title, SECTION_FONT);
            reportTitle.setSpacingBefore(12);
            reportTitle.setSpacingAfter(12);
            document.add(reportTitle);

            // Process Markdown lines into styled PDF paragraphs
            String[] lines = markdownContent.split("\n");
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("# ")) {
                    // Skip top title since already added
                    continue;
                } else if (line.startsWith("## ")) {
                    Paragraph section = new Paragraph(line.substring(3).trim(), SECTION_FONT);
                    section.setSpacingBefore(12);
                    section.setSpacingAfter(6);
                    document.add(section);
                } else if (line.startsWith("### ")) {
                    Paragraph subSection = new Paragraph(line.substring(4).trim(), BOLD_FONT);
                    subSection.setSpacingBefore(8);
                    subSection.setSpacingAfter(4);
                    document.add(subSection);
                } else if (line.startsWith("> ")) {
                    // Callout / Disclaimer block
                    Paragraph callout = new Paragraph(line.substring(2).trim(), DISCLAIMER_FONT);
                    callout.setSpacingBefore(4);
                    callout.setSpacingAfter(4);
                    callout.setIndentationLeft(15);
                    document.add(callout);
                } else if (line.startsWith("- ")) {
                    Paragraph bullet = new Paragraph("• " + line.substring(2).trim(), BODY_FONT);
                    bullet.setIndentationLeft(12);
                    bullet.setSpacingAfter(2);
                    document.add(bullet);
                } else if (line.startsWith("|") && line.endsWith("|")) {
                    // Skip markdown separator rows
                    if (line.contains("---")) continue;
                    Paragraph tableRow = new Paragraph(line.replace("|", "  "), BODY_FONT);
                    tableRow.setIndentationLeft(10);
                    document.add(tableRow);
                } else {
                    Paragraph para = new Paragraph(line, BODY_FONT);
                    para.setSpacingAfter(4);
                    document.add(para);
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
