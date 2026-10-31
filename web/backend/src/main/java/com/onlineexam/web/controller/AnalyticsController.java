package com.onlineexam.web.controller;

import com.onlineexam.dao.*;
import com.onlineexam.model.*;
import com.onlineexam.web.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final StudentExamDAO studentExamDAO;
    private final ExamDAO examDAO;
    private final UserDAO userDAO;
    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;
    private final QuestionDAO questionDAO;
    private final StudyMaterialDAO studyMaterialDAO;

    public AnalyticsController(StudentExamDAO studentExamDAO, ExamDAO examDAO, UserDAO userDAO,
                               SubjectDAO subjectDAO, TopicDAO topicDAO, QuestionDAO questionDAO,
                               StudyMaterialDAO studyMaterialDAO) {
        this.studentExamDAO = studentExamDAO;
        this.examDAO = examDAO;
        this.userDAO = userDAO;
        this.subjectDAO = subjectDAO;
        this.topicDAO = topicDAO;
        this.questionDAO = questionDAO;
        this.studyMaterialDAO = studyMaterialDAO;
    }

    @GetMapping("/student/overview")
    public ResponseEntity<?> getStudentOverview(@AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        if (principal == null) return ResponseEntity.status(401).build();

        List<StudentExam> attempts = studentExamDAO.findByStudentId(principal.id());
        int totalAttempts = attempts.size();
        long passedCount = 0;
        double totalPct = 0.0;

        for (StudentExam se : attempts) {
            Optional<Exam> examOpt = examDAO.findById(se.getExamId());
            if (examOpt.isPresent()) {
                Exam exam = examOpt.get();
                if (se.getTotalScore() >= exam.getPassingMarks()) {
                    passedCount++;
                }
                if (exam.getTotalMarks() > 0) {
                    totalPct += ((double) se.getTotalScore() / exam.getTotalMarks()) * 100.0;
                }
            }
        }
        double avgScore = totalAttempts > 0 ? (totalPct / totalAttempts) : 0.0;
        int completedMaterials = studyMaterialDAO.countCompletedByStudentId(principal.id());

        Map<String, Object> data = new HashMap<>();
        data.put("totalAttempts", totalAttempts);
        data.put("passedCount", passedCount);
        data.put("passedAttempts", passedCount);
        data.put("failedCount", totalAttempts - passedCount);
        data.put("averageScore", Math.round(avgScore * 10.0) / 10.0);
        data.put("averageScorePercentage", Math.round(avgScore * 10.0) / 10.0);
        data.put("passRate", totalAttempts > 0 ? Math.round(((double) passedCount / totalAttempts) * 1000.0) / 10.0 : 0.0);
        data.put("completedMaterialsCount", completedMaterials);
        data.put("recentAttempts", attempts.stream().limit(5).toList());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminOverview() throws SQLException {
        List<User> users = userDAO.findAll();
        long studentCount = users.stream().filter(u -> u.getRole() == User.Role.STUDENT).count();
        List<Exam> exams = examDAO.findAll();
        List<Subject> subjects = subjectDAO.findAll();
        List<Topic> topics = topicDAO.findAll();
        List<Question> questions = questionDAO.findAll();
        List<StudentExam> allAttempts = studentExamDAO.findAll();

        int totalAttempts = allAttempts.size();
        long passedCount = 0;
        double totalPct = 0.0;

        for (StudentExam se : allAttempts) {
            Optional<Exam> examOpt = examDAO.findById(se.getExamId());
            if (examOpt.isPresent()) {
                Exam exam = examOpt.get();
                if (se.getTotalScore() >= exam.getPassingMarks()) {
                    passedCount++;
                }
                if (exam.getTotalMarks() > 0) {
                    totalPct += ((double) se.getTotalScore() / exam.getTotalMarks()) * 100.0;
                }
            }
        }

        double passRate = totalAttempts > 0 ? ((double) passedCount / totalAttempts) * 100.0 : 0.0;
        double avgScore = totalAttempts > 0 ? (totalPct / totalAttempts) : 0.0;

        Map<String, Object> data = new HashMap<>();
        data.put("totalStudents", studentCount);
        data.put("totalSubjects", subjects.size());
        data.put("totalTopics", topics.size());
        data.put("totalExams", exams.size());
        data.put("totalQuestions", questions.size());
        data.put("totalAttempts", totalAttempts);
        data.put("overallPassRate", Math.round(passRate * 10.0) / 10.0);
        data.put("averagePassingRate", Math.round(passRate * 10.0) / 10.0);
        data.put("averageScore", Math.round(avgScore * 10.0) / 10.0);
        data.put("recentAttempts", allAttempts.stream().limit(10).toList());

        return ResponseEntity.ok(data);
    }
}
