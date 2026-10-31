package com.onlineexam.service;

import com.onlineexam.model.Exam;
import com.onlineexam.model.ExamResult;
import com.onlineexam.model.Question;
import com.onlineexam.model.StudentAnswer;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Business service responsible for strictly evaluating examination answers.
 * <p>
 * Evaluates candidate responses against canonical question answer keys from the database,
 * preventing any reliance on client-supplied scores. Calculates earned marks, percentages,
 * question breakdown (correct, incorrect, unanswered), and pass/fail status.
 * </p>
 */
public class EvaluationService {

    /**
     * Evaluates candidate responses from a question-to-option mapping.
     *
     * @param attemptId unique exam session ID (may be null in simulated evaluations)
     * @param studentId ID of the candidate
     * @param exam the exam definition containing title, passing marks, and total marks
     * @param questions the authoritative list of exam questions
     * @param submittedAnswers map of question ID to candidate selected option
     * @param autoSubmitted whether the submission was caused by countdown expiration
     * @return fully computed and validated {@link ExamResult}
     */
    public ExamResult evaluate(Long attemptId,
                               Long studentId,
                               Exam exam,
                               List<Question> questions,
                               Map<Long, Question.Option> submittedAnswers,
                               boolean autoSubmitted) {

        validateInputs(exam, questions);

        Map<Long, Question.Option> answers = (submittedAnswers != null) ? submittedAnswers : Collections.emptyMap();
        List<StudentAnswer> evaluatedList = new ArrayList<>();

        int totalQuestions = questions.size();
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;
        int totalPossibleMarks = 0;
        int earnedMarks = 0;

        for (Question question : questions) {
            int qMarks = question.getMarks();
            totalPossibleMarks += qMarks;

            Question.Option selected = answers.get(question.getId());

            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setStudentExamId(attemptId);
            studentAnswer.setQuestionId(question.getId());
            studentAnswer.setSelectedOption(selected);
            studentAnswer.setAnsweredAt(LocalDateTime.now());

            if (selected == null) {
                unansweredCount++;
                studentAnswer.setCorrect(false);
            } else if (question.isCorrect(selected)) {
                correctCount++;
                earnedMarks += qMarks;
                studentAnswer.setCorrect(true);
            } else {
                incorrectCount++;
                studentAnswer.setCorrect(false);
            }

            evaluatedList.add(studentAnswer);
        }

        // Percentage calculation protected against division by zero
        double percentage = (totalPossibleMarks > 0)
                ? ((double) earnedMarks / totalPossibleMarks) * 100.0
                : 0.0;

        boolean passed = earnedMarks >= exam.getPassingMarks();

        return new ExamResult(
                attemptId,
                studentId,
                exam.getId(),
                exam.getTitle(),
                totalQuestions,
                correctCount,
                incorrectCount,
                unansweredCount,
                totalPossibleMarks,
                earnedMarks,
                exam.getPassingMarks(),
                Math.round(percentage * 100.0) / 100.0, // 2 decimal precision
                passed,
                autoSubmitted,
                LocalDateTime.now(),
                evaluatedList
        );
    }

    /**
     * Evaluates existing {@link StudentAnswer} objects stored in database.
     */
    public ExamResult evaluate(Long attemptId,
                               Long studentId,
                               Exam exam,
                               List<Question> questions,
                               List<StudentAnswer> existingAnswers,
                               boolean autoSubmitted) {

        Map<Long, Question.Option> answerMap = new HashMap<>();
        if (existingAnswers != null) {
            for (StudentAnswer a : existingAnswers) {
                if (a.getQuestionId() != null) {
                    answerMap.put(a.getQuestionId(), a.getSelectedOption());
                }
            }
        }
        return evaluate(attemptId, studentId, exam, questions, answerMap, autoSubmitted);
    }

    private void validateInputs(Exam exam, List<Question> questions) {
        if (exam == null) {
            throw new IllegalArgumentException("Exam definition cannot be null for evaluation.");
        }
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Cannot evaluate exam without questions.");
        }
    }
}
