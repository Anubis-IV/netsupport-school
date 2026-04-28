package anubis.netsupport_school.backend.service;

import anubis.netsupport_school.backend.domain.dto.response.ResultItemDTO;
import anubis.netsupport_school.backend.domain.dto.response.ResultsResponseDTO;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReportService {

    private static final DeviceRgb HEADER_BG      = new DeviceRgb(30,  30,  46);   // dark navy
    private static final DeviceRgb ACCENT          = new DeviceRgb(99,  102, 241);  // indigo
    private static final DeviceRgb ROW_ALT         = new DeviceRgb(243, 244, 246);  // light gray
    private static final DeviceRgb TEXT_DARK        = new DeviceRgb(17,  24,  39);
    private static final DeviceRgb TEXT_LIGHT       = new DeviceRgb(255, 255, 255);
    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generate(ResultsResponseDTO data) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter  writer  = new PdfWriter(baos);
        PdfDocument pdf    = new PdfDocument(writer);
        Document   doc     = new Document(pdf);
        doc.setMargins(36, 36, 36, 36);

        // ── Title block ──────────────────────────────────────────────────────
        Paragraph title = new Paragraph("Exam Results Report")
                .setFontColor(TEXT_DARK)
                .setFontSize(22)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4);
        doc.add(title);

        Paragraph examTitle = new Paragraph("Exam: " + data.examTitle())
                .setFontColor(new DeviceRgb(75, 85, 99))
                .setFontSize(13)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
        doc.add(examTitle);

        Paragraph examId = new Paragraph("Exam ID: " + data.examId())
                .setFontColor(new DeviceRgb(107, 114, 128))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        doc.add(examId);

        // ── Summary row ──────────────────────────────────────────────────────
        int total   = data.results().size();
        double avg  = data.results().stream()
                .mapToDouble(r -> r.totalQuestions() > 0
                        ? (double) r.score() / r.totalQuestions() * 100 : 0)
                .average().orElse(0);

        Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(24);

        addSummaryCell(summary, "Total Students", String.valueOf(total));
        addSummaryCell(summary, "Average Score",  String.format("%.1f%%", avg));
        addSummaryCell(summary, "Exam ID",        String.valueOf(data.examId()));
        doc.add(summary);

        // ── Results table ────────────────────────────────────────────────────
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 3, 2, 2, 2, 3}))
                .useAllAvailableWidth();

        // Header
        String[] headers = {"Student Name", "Student ID", "Hostname",
                "Score", "Total Qs", "Answered", "Submitted At"};
        for (String h : headers) {
            table.addHeaderCell(
                    new Cell().add(new Paragraph(h).setFontColor(TEXT_LIGHT).setFontSize(10))
                            .setBackgroundColor(HEADER_BG)
                            .setBorder(Border.NO_BORDER)
                            .setPadding(8)
            );
        }

        // Data rows
        var results = data.results();
        for (int i = 0; i < results.size(); i++) {
            ResultItemDTO r   = results.get(i);
            DeviceRgb     bg  = (i % 2 == 0) ? null : ROW_ALT;
            boolean       alt = (i % 2 != 0);

            double pct = r.totalQuestions() > 0
                    ? (double) r.score() / r.totalQuestions() * 100 : 0;
            String scoreText = r.score() + " / " + r.totalQuestions()
                    + String.format(" (%.0f%%)", pct);

            addDataCell(table, r.studentName(),  alt);
            addDataCell(table, r.studentId(),    alt);
            addDataCell(table, r.hostname(),     alt);
            addScoreCell(table, scoreText, pct,  alt);
            addDataCell(table, String.valueOf(r.totalQuestions()),    alt);
            addDataCell(table, String.valueOf(r.answeredQuestions()), alt);
            addDataCell(table, r.submittedAt() != null
                    ? r.submittedAt().format(FMT) : "-", alt);
        }

        doc.add(table);

        // ── Footer ───────────────────────────────────────────────────────────
        doc.add(new Paragraph("Generated by NetSupport School")
                .setFontColor(new DeviceRgb(156, 163, 175))
                .setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(16));

        doc.close();
        return baos.toByteArray();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void addSummaryCell(Table t, String label, String value) {
        t.addCell(new Cell()
                .add(new Paragraph(label)
                        .setFontSize(9)
                        .setFontColor(new DeviceRgb(107, 114, 128)))
                .add(new Paragraph(value)
                        .setFontSize(16)
                        .setFontColor(TEXT_DARK))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(ACCENT, 1))
                .setPadding(12)
        );
    }

    private void addDataCell(Table t, String text, boolean alt) {
        Cell cell = new Cell()
                .add(new Paragraph(text != null ? text : "-")
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(229, 231, 235), 0.5f))
                .setPaddingTop(7).setPaddingBottom(7)
                .setPaddingLeft(8).setPaddingRight(8);
        if (alt) cell.setBackgroundColor(ROW_ALT);
        t.addCell(cell);
    }

    private void addScoreCell(Table t, String text, double pct, boolean alt) {
        DeviceRgb color = pct >= 80 ? new DeviceRgb(22, 163, 74)   // green
                : pct >= 50 ? new DeviceRgb(202, 138, 4)             // amber
                  : new DeviceRgb(220, 38, 38);                         // red

        Cell cell = new Cell()
                .add(new Paragraph(text)
                        .setFontSize(10)
                        .setFontColor(color))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(229, 231, 235), 0.5f))
                .setPaddingTop(7).setPaddingBottom(7)
                .setPaddingLeft(8).setPaddingRight(8);
        if (alt) cell.setBackgroundColor(ROW_ALT);
        t.addCell(cell);
    }
}