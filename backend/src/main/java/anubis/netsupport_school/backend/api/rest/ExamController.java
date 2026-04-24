package anubis.netsupport_school.backend.api.rest;

import anubis.netsupport_school.backend.domain.dto.request.ExamRequestDTO;
import anubis.netsupport_school.backend.domain.dto.response.ExamDetailsDTO;
import anubis.netsupport_school.backend.domain.dto.response.ExamsResponseDTO;
import anubis.netsupport_school.backend.domain.dto.response.SimpleMessageResponseDTO;
import anubis.netsupport_school.backend.domain.mapper.ExamMapper;
import anubis.netsupport_school.backend.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    // =========================
    // GET ALL EXAMS
    // =========================
    @GetMapping
    public ExamsResponseDTO getAllExams() {

        var exams = examService.getAllExams()
                .stream()
                .map(ExamMapper::toSummaryDTO)
                .toList();

        return new ExamsResponseDTO(exams);
    }

    // =========================
    // GET EXAM BY ID
    // =========================
    @GetMapping("/{examId}")
    public ExamDetailsDTO getExam(@PathVariable Long examId) {

        var exam = examService.getExamOrThrow(examId);

        return ExamMapper.toDetailsDTO(exam);
    }

    // =========================
    // CREATE EXAM
    // =========================
    @PostMapping
    public ResponseEntity<SimpleMessageResponseDTO> createExam(
            @Valid @RequestBody ExamRequestDTO dto) {

        Long id = examService.createExam(dto);

        return ResponseEntity.status(201)
                .body(new SimpleMessageResponseDTO(
                        id,
                        "Exam created successfully"
                ));
    }

    // =========================
    // UPDATE EXAM
    // =========================
    @PutMapping("/{examId}")
    public ResponseEntity<SimpleMessageResponseDTO> updateExam(
            @PathVariable Long examId,
            @Valid @RequestBody ExamRequestDTO dto) {

        Long id = examService.updateExam(examId, dto);

        return ResponseEntity.ok(
                new SimpleMessageResponseDTO(
                        id,
                        "Exam updated successfully"
                )
        );
    }

    // =========================
    // DELETE EXAM
    // =========================
    @DeleteMapping("/{examId}")
    public ResponseEntity<?> deleteExam(@PathVariable Long examId) {

        examService.deleteExam(examId);

        return ResponseEntity.ok(
                java.util.Map.of("message", "Exam deleted successfully")
        );
    }
}
