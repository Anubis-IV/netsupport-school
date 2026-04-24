package anubis.netsupport_school.backend.api.rest;

import anubis.netsupport_school.backend.domain.dto.response.ResultsResponseDTO;
import anubis.netsupport_school.backend.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    // =========================
    // GET RESULTS BY EXAM
    // =========================
    @GetMapping("/exam/{examId}")
    public ResultsResponseDTO getResults(@PathVariable Long examId) {

        return resultService.getResultsByExam(examId);
    }

    /*
    // =========================
    // DOWNLOAD PDF REPORT
    // =========================
    @GetMapping("/exam/{examId}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long examId) {

        byte[] pdf = resultService.generatePdfReport(examId);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"exam-" + examId + "-report.pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }

     */
}
