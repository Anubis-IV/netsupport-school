package anubis.netsupport_school.backend.service;

import anubis.netsupport_school.backend.domain.dto.request.ExamRequestDTO;
import anubis.netsupport_school.backend.domain.mapper.ExamMapper;
import anubis.netsupport_school.backend.domain.mapper.QuestionMapper;
import anubis.netsupport_school.backend.domain.model.Exam;
import anubis.netsupport_school.backend.domain.model.Question;
import anubis.netsupport_school.backend.repository.ExamRepository;
import anubis.netsupport_school.backend.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamService {

    private final ExamRepository examRepository;



    @Autowired
    public ExamService(ExamRepository examRepository) {
        this.examRepository = examRepository;


    }

    @Transactional
    public Long createExam(ExamRequestDTO dto) {

        Exam exam = ExamMapper.toEntity(dto);

        return examRepository.save(exam).getExamId();
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Exam getExamOrThrow(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
    }

    @Transactional
    public Long updateExam(Long examId, ExamRequestDTO dto) {

        Exam existing = getExamOrThrow(examId);

        existing.getQuestions().clear();

        existing.setTitle(dto.title());
        existing.setDurationMin(dto.durationMinutes());

        List<Question> newQuestions = dto.questions().stream()
                .map(q -> QuestionMapper.toEntity(q, existing))
                .toList();

        existing.getQuestions().addAll(newQuestions);

        return examRepository.save(existing).getExamId();
    }

    @Transactional
    public void deleteExam(Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new RuntimeException("Exam not found");
        }
        examRepository.deleteById(examId);
    }
}