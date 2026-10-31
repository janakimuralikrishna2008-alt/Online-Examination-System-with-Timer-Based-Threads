package com.onlineexam.web.config;

import com.onlineexam.dao.*;
import com.onlineexam.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration registering domain DAOs and core business services as Spring Beans.
 */
@Configuration
public class ServiceConfig {

    @Bean
    public UserDAO userDAO() {
        return new UserDAO();
    }

    @Bean
    public SubjectDAO subjectDAO() {
        return new SubjectDAO();
    }

    @Bean
    public TopicDAO topicDAO() {
        return new TopicDAO();
    }

    @Bean
    public StudyMaterialDAO studyMaterialDAO() {
        return new StudyMaterialDAO();
    }

    @Bean
    public ExamDAO examDAO() {
        return new ExamDAO();
    }

    @Bean
    public QuestionDAO questionDAO() {
        return new QuestionDAO();
    }

    @Bean
    public StudentExamDAO studentExamDAO() {
        return new StudentExamDAO();
    }

    @Bean
    public StudentAnswerDAO studentAnswerDAO() {
        return new StudentAnswerDAO();
    }

    @Bean
    public AuthService authService(UserDAO userDAO) {
        return new AuthService(userDAO);
    }

    @Bean
    public SubjectService subjectService(SubjectDAO subjectDAO) {
        return new SubjectService(subjectDAO);
    }

    @Bean
    public TopicService topicService(TopicDAO topicDAO) {
        return new TopicService(topicDAO);
    }

    @Bean
    public LearningService learningService(StudyMaterialDAO studyMaterialDAO) {
        return new LearningService(studyMaterialDAO);
    }

    @Bean
    public ExamService examService(ExamDAO examDAO, QuestionDAO questionDAO) {
        return new ExamService(examDAO, questionDAO);
    }

    @Bean
    public EvaluationService evaluationService() {
        return new EvaluationService();
    }

    @Bean
    public AttemptService attemptService(StudentExamDAO studentExamDAO,
                                         StudentAnswerDAO studentAnswerDAO,
                                         ExamService examService,
                                         EvaluationService evaluationService) {
        return new AttemptService(studentExamDAO, studentAnswerDAO, examService, evaluationService);
    }
}

