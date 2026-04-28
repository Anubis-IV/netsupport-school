package anubis.netsupport_school.backend.api.rest;

import anubis.netsupport_school.backend.domain.dto.response.ResultsResponseDTO;
import anubis.netsupport_school.backend.service.PdfReportService;
import anubis.netsupport_school.backend.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;
    private final PdfReportService pdfReportService;

    public ResultController(ResultService resultService, PdfReportService pdfReportService) {
        this.resultService    = resultService;
        this.pdfReportService = pdfReportService;
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAllResults() {

        resultService.clearAllResults();

        return ResponseEntity.ok(
                java.util.Map.of("message", "All results cleared successfully")
        );
    }

    // =========================
    // GET RESULTS BY EXAM
    // =========================
    @GetMapping("/exam/{examId}")
    public ResultsResponseDTO getResults(@PathVariable Long examId) {

        return resultService.getResultsByExam(examId);
    }

    // =========================
    // DOWNLOAD PDF REPORT
    // =========================
    @GetMapping("/exam/{examId}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long examId) {

        ResultsResponseDTO data = resultService.getResultsByExam(examId);
        byte[] pdf = pdfReportService.generate(data);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"exam-" + examId + "-report.pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }
}
