package anubis.netsupport_school.backend.service;

import anubis.netsupport_school.backend.domain.dto.response.ResultsResponseDTO;
import anubis.netsupport_school.backend.domain.mapper.ResultMapper;
import anubis.netsupport_school.backend.domain.model.Exam;
import anubis.netsupport_school.backend.domain.model.Question;
import anubis.netsupport_school.backend.domain.model.Result;
import anubis.netsupport_school.backend.domain.model.ResultAnswer;
import anubis.netsupport_school.backend.repository.ExamRepository;
import anubis.netsupport_school.backend.repository.QuestionRepository;
import anubis.netsupport_school.backend.repository.ResultAnswerRepository;
import anubis.netsupport_school.backend.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final ResultAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Autowired
    public ResultService(ResultRepository resultRepository,
                         ResultAnswerRepository answerRepository,
                         QuestionRepository questionRepository,
                         ExamRepository examRepository) {
        this.resultRepository = resultRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
    }

    // =========================
    // SAVE OR UPDATE ANSWERS
    // =========================
    @Transactional
    private Result doSaveOrUpdateAnswers(
            Long examId,
            String studentId,
            String studentName,
            String hostname,
            Map<Long, Integer> answersMap
    ) {

        // 1. validate exam exists
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        // 2. find or create result
        Result result = resultRepository
                .findByExamExamIdAndStudentId(examId, studentId)
                .orElseGet(() -> {
                    Result r = new Result();
                    r.setExam(exam);
                    r.setStudentId(studentId);

                    // nullable = false
                    r.setScore(0);
                    r.setTotalQuestions(0);
                    r.setAnsweredQuestions(0);
                    return r;
                });

        // always update latest info
        result.setStudentName(studentName);
        result.setHostname(hostname);
        result.setSubmittedAt(LocalDateTime.now());

        //final Result savedResult = resultRepository.save(result);

        // 3. delete old answers (FAST)
        answerRepository.deleteByResultResultId(result.getResultId());

        // 4. save new answers
        List<ResultAnswer> newAnswers = answersMap.entrySet()
                .stream()
                .map(entry -> {
                    ResultAnswer a = new ResultAnswer();


                    a.setResult(result);


                    Question q = questionRepository.getReferenceById(entry.getKey());
                    a.setQuestion(q);


                    a.setSelectedChoiceId(entry.getValue());

                    return a;
                })
                .toList();

        var res = resultRepository.save(result);
        answerRepository.saveAll(newAnswers);

        // 5. calculate score
        calculateScore(result, newAnswers);

        return res;
    }

    @Transactional
    public Result saveOrUpdateAnswers(Long examId,
                                                  String studentId,
                                                  String studentName,
                                                  String hostname,
                                                  Map<Long, Integer> answersMap) {
        Object lock = locks.computeIfAbsent(studentId, k -> new Object());

        synchronized (lock) {
            return doSaveOrUpdateAnswers(examId, studentId, studentName, hostname, answersMap);
        }
    }
    // =========================
    // SCORE CALCULATION (OPTIMIZED)
    // =========================
    private void calculateScore(Result result, List<ResultAnswer> answers) {

        List<Question> questions = questionRepository
                .findByExam_ExamId(result.getExam().getExamId());

        // build map: questionId -> correctChoiceId
        Map<Long, Integer> correctMap = questions.stream()
                .collect(Collectors.toMap(
                        Question::getQuestionId,
                        Question::getCorrectChoiceId
                ));

        int correct = 0;

        for (ResultAnswer ans : answers) {
            Integer correctChoice = correctMap.get(ans.getQuestion().getQuestionId());

            if (correctChoice != null &&
                    correctChoice == ans.getSelectedChoiceId()) {
                correct++;
            }
        }

        result.setScore(correct);
        result.setTotalQuestions(questions.size());
        result.setAnsweredQuestions(answers.size());
    }

    // =========================
    // GET RESULTS BY EXAM
    // =========================
    public ResultsResponseDTO getResultsByExam(Long examId) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        List<Result> results = resultRepository.findByExamExamId(examId);

        return ResultMapper.toResponse(
                examId,
                exam.getTitle(),
                results
        );
    }

    @Transactional
    public void clearAllResults() {

        answerRepository.deleteAll();

        // then delete results
        resultRepository.deleteAll();
    }
}
