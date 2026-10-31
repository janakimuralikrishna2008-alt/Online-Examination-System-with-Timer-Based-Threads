package com.onlineexam.web.service;

import com.onlineexam.dao.ExamDAO;
import com.onlineexam.dao.StudentExamDAO;
import com.onlineexam.model.Exam;
import com.onlineexam.model.StudentExam;
import com.onlineexam.service.AttemptService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Multi-Threaded Examination Timer Service for Web API Sessions.
 * <p>
 * Manages active student exam attempts using a dedicated {@link ScheduledExecutorService}.
 * Automatically auto-submits expired examination sessions when countdown deadlines are reached,
 * preventing unsubmitted exams from remaining stuck in limbo even if the client disconnects.
 * </p>
 */
@Service
public class ServerExamTimerManager {

    private static final Logger LOGGER = Logger.getLogger(ServerExamTimerManager.class.getName());
    public static final int GRACE_PERIOD_SECONDS = 15; // 15-second network latency buffer

    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> activeTimerTasks = new ConcurrentHashMap<>();

    private final StudentExamDAO studentExamDAO;
    private final ExamDAO examDAO;
    private final AttemptService attemptService;

    public ServerExamTimerManager(StudentExamDAO studentExamDAO, ExamDAO examDAO, AttemptService attemptService) {
        this.studentExamDAO = studentExamDAO;
        this.examDAO = examDAO;
        this.attemptService = attemptService;
        this.scheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "WebExamTimerThread-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void initAndRecoverActiveSessions() {
        LOGGER.info("Initializing Web Exam Timer Manager and checking for active sessions...");
        try {
            for (StudentExam attempt : studentExamDAO.findAll()) {
                if (attempt.isInProgress()) {
                    Optional<Exam> examOpt = examDAO.findById(attempt.getExamId());
                    if (examOpt.isPresent()) {
                        Exam exam = examOpt.get();
                        LocalDateTime deadline = attempt.getStartTime().plusMinutes(exam.getDurationMinutes());
                        long remainingSeconds = Duration.between(LocalDateTime.now(), deadline).getSeconds();

                        if (remainingSeconds <= -GRACE_PERIOD_SECONDS) {
                            LOGGER.info("Auto-submitting overdue attempt on startup: " + attempt.getId());
                            autoSubmitExpiredAttempt(attempt.getId());
                        } else {
                            long delay = Math.max(1, remainingSeconds + GRACE_PERIOD_SECONDS);
                            scheduleAutoSubmission(attempt.getId(), delay);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error recovering active exam timer sessions on startup", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down Web Exam Timer scheduler...");
        scheduler.shutdownNow();
    }

    /**
     * Registers and starts a countdown timer thread for a newly started or resumed attempt.
     */
    public void registerSessionTimer(Long attemptId, int durationMinutes, LocalDateTime startTime) {
        if (attemptId == null) return;

        LocalDateTime deadline = startTime.plusMinutes(durationMinutes);
        long remainingSeconds = Duration.between(LocalDateTime.now(), deadline).getSeconds();
        long delaySeconds = Math.max(1, remainingSeconds + GRACE_PERIOD_SECONDS);

        // Cancel previous if exists
        cancelSessionTimer(attemptId);

        scheduleAutoSubmission(attemptId, delaySeconds);
        LOGGER.info(String.format("Scheduled timer thread for attempt %d. Auto-submission trigger in %d seconds.",
                attemptId, delaySeconds));
    }

    /**
     * Cancels an active timer thread (e.g. when candidate manually submits before deadline).
     */
    public void cancelSessionTimer(Long attemptId) {
        if (attemptId == null) return;
        ScheduledFuture<?> future = activeTimerTasks.remove(attemptId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            LOGGER.info("Cancelled timer thread for attempt " + attemptId);
        }
    }

    /**
     * Checks if the deadline for an attempt has passed beyond the grace period.
     */
    public boolean isExpired(StudentExam attempt, Exam exam) {
        if (attempt == null || exam == null || attempt.getStartTime() == null) {
            return false;
        }
        LocalDateTime deadline = attempt.getStartTime().plusMinutes(exam.getDurationMinutes());
        return LocalDateTime.now().isAfter(deadline.plusSeconds(GRACE_PERIOD_SECONDS));
    }

    /**
     * Calculates remaining seconds from server clock.
     */
    public long getRemainingSeconds(StudentExam attempt, Exam exam) {
        if (attempt == null || exam == null || attempt.getStartTime() == null) {
            return 0;
        }
        LocalDateTime deadline = attempt.getStartTime().plusMinutes(exam.getDurationMinutes());
        long diff = Duration.between(LocalDateTime.now(), deadline).getSeconds();
        return Math.max(0, diff);
    }

    private void scheduleAutoSubmission(Long attemptId, long delaySeconds) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            autoSubmitExpiredAttempt(attemptId);
        }, delaySeconds, TimeUnit.SECONDS);

        activeTimerTasks.put(attemptId, future);
    }

    private void autoSubmitExpiredAttempt(Long attemptId) {
        try {
            activeTimerTasks.remove(attemptId);
            Optional<StudentExam> attOpt = studentExamDAO.findById(attemptId);
            if (attOpt.isPresent() && attOpt.get().isInProgress()) {
                LOGGER.info(String.format("[TIMER THREAD EXPIRED] Automatically finalizing exam attempt %d...", attemptId));
                attemptService.submitAttempt(attemptId, Collections.emptyMap(), true);
                LOGGER.info(String.format("[TIMER THREAD EXPIRED] Attempt %d auto-submitted successfully.", attemptId));
            }
        } catch (IllegalStateException e) {
            // Already submitted by candidate manually
            LOGGER.info(String.format("Attempt %d was already submitted before timer auto-submit.", attemptId));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to auto-submit attempt " + attemptId + " on timer expiry", e);
        }
    }
}
