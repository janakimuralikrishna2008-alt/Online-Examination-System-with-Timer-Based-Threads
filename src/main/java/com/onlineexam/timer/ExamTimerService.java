package com.onlineexam.timer;

import com.onlineexam.dao.StudentExamDAO;
import com.onlineexam.model.StudentExam;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Multi-Threaded Examination Timer Service.
 * <p>
 * Manages deadline countdowns for student exam attempts using {@link ScheduledExecutorService}.
 * Calculates remaining time using authoritative {@link System#nanoTime()} rather than naively decrementing counters.
 * Guarantees thread-safe one-time submission, prevents duplicate tasks per attempt, and dispatches UI updates safely to the Swing EDT.
 * </p>
 */
public class ExamTimerService {

    private static final Logger LOGGER = Logger.getLogger(ExamTimerService.class.getName());

    public enum TimerState {
        READY,
        RUNNING,
        SUBMITTED,
        CANCELLED
    }

    // Global registry preventing multiple concurrent timers for the same attempt ID
    private static final Map<Long, ExamTimerService> ACTIVE_TIMERS = new ConcurrentHashMap<>();

    private final Long attemptId;
    private final long totalDurationSeconds;
    private final long tickIntervalMs;
    private final long warningThresholdSeconds;

    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;
    private final List<ExamTimerListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicReference<TimerState> state = new AtomicReference<>(TimerState.READY);
    private final AtomicBoolean submissionGuard = new AtomicBoolean(false);

    private volatile ScheduledFuture<?> scheduledTask;
    private volatile long deadlineNano;
    private volatile long startNano;

    // Optional background persistence and submission hooks
    private final StudentExamDAO studentExamDAO;
    private final Runnable onSubmitTask;
    private long lastPersistedSecond = -1;
    private static final int PERSISTENCE_THROTTLE_SECONDS = 5;

    /**
     * Standard constructor using an internally managed daemon thread pool.
     *
     * @param attemptId the unique student exam session ID (or negative value for standalone demos)
     * @param durationSeconds total duration of the examination
     * @param studentExamDAO optional DAO for periodic database persistence (may be null)
     * @param onSubmitTask optional task to execute upon submission (may be null)
     */
    public ExamTimerService(Long attemptId,
                            long durationSeconds,
                            StudentExamDAO studentExamDAO,
                            Runnable onSubmitTask) {
        this(attemptId, durationSeconds, 1000L, 60L, studentExamDAO, onSubmitTask, null);
    }

    /**
     * Comprehensive constructor supporting custom intervals and injected executors for unit testing.
     */
    public ExamTimerService(Long attemptId,
                            long durationSeconds,
                            long tickIntervalMs,
                            long warningThresholdSeconds,
                            StudentExamDAO studentExamDAO,
                            Runnable onSubmitTask,
                            ScheduledExecutorService customScheduler) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be strictly positive: " + durationSeconds);
        }
        this.attemptId = attemptId;
        this.totalDurationSeconds = durationSeconds;
        this.tickIntervalMs = Math.max(10L, tickIntervalMs);
        this.warningThresholdSeconds = warningThresholdSeconds;
        this.studentExamDAO = studentExamDAO;
        this.onSubmitTask = onSubmitTask;

        if (customScheduler != null) {
            this.scheduler = customScheduler;
            this.ownsScheduler = false;
        } else {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonTimerThreadFactory(attemptId));
            this.ownsScheduler = true;
        }
    }

    /**
     * Starts the countdown timer.
     *
     * @throws IllegalStateException if timer is already started or another timer is active for this attempt
     */
    public synchronized void start() {
        if (!state.compareAndSet(TimerState.READY, TimerState.RUNNING)) {
            throw new IllegalStateException("Cannot start timer in state: " + state.get());
        }

        // Prevent duplicate timers for the same database attempt
        if (attemptId != null && attemptId > 0) {
            ExamTimerService existing = ACTIVE_TIMERS.putIfAbsent(attemptId, this);
            if (existing != null && existing != this) {
                state.set(TimerState.CANCELLED);
                throw widespreadAttemptConflict(attemptId);
            }
        }

        this.startNano = System.nanoTime();
        this.deadlineNano = startNano + TimeUnit.SECONDS.toNanos(totalDurationSeconds);

        LOGGER.info(String.format("Starting exam timer for attempt %d: duration=%d seconds", attemptId, totalDurationSeconds));

        // Initial tick dispatch
        onTimerTick();

        // Schedule periodic ticks
        this.scheduledTask = scheduler.scheduleAtFixedRate(
                this::safeTick,
                tickIntervalMs,
                tickIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Periodic task executed by the scheduler.
     * Robust exception handling ensures thread doesn't terminate prematurely.
     */
    private void safeTick() {
        try {
            if (state.get() != TimerState.RUNNING) {
                return;
            }
            onTimerTick();
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Unexpected exception in exam timer tick for attempt " + attemptId, t);
            notifyListeners(l -> l.onError(t));
        }
    }

    /**
     * Evaluates authoritative remaining time via System.nanoTime() against deadline.
     */
    private void onTimerTick() {
        long currentNano = System.nanoTime();
        long remainingNanos = deadlineNano - currentNano;

        if (remainingNanos <= 0) {
            // Deadline reached
            handleDeadlineExpiration();
        } else {
            // Calculate remaining seconds with ceiling rounding so 0 only occurs when deadline has truly elapsed
            long remainingSeconds = (remainingNanos + 999_999_999L) / 1_000_000_000L;
            String formatted = formatTime(remainingSeconds);

            notifyListeners(l -> l.onTick(remainingSeconds, formatted));

            if (remainingSeconds <= warningThresholdSeconds) {
                notifyListeners(l -> l.onWarning(remainingSeconds));
            }

            // Periodic throttled persistence to DB
            throttlePersist(remainingSeconds);
        }
    }

    private void handleDeadlineExpiration() {
        if (state.get() != TimerState.RUNNING) {
            return;
        }

        LOGGER.info("Exam deadline expired for attempt: " + attemptId + ". Triggering auto-submission.");

        // First notify UI of expiration
        notifyListeners(ExamTimerListener::onTimeExpired);

        // Perform thread-safe auto-submission
        performSubmission(true);
    }

    /**
     * Manually triggers exam submission (e.g., student clicks 'Submit' button in UI).
     * Protected by thread-safe guard against concurrent timeout submission.
     *
     * @return true if this call performed the submission, false if submission already occurred
     */
    public boolean manualSubmit() {
        LOGGER.info("Manual submission requested for attempt: " + attemptId);
        return performSubmission(false);
    }

    /**
     * Core submission logic guarded by AtomicBoolean to guarantee exactly-once execution.
     */
    private boolean performSubmission(boolean isAutoSubmitted) {
        if (!submissionGuard.compareAndSet(false, true)) {
            LOGGER.warning("Duplicate submission attempt detected and prevented for attempt: " + attemptId);
            return false;
        }

        state.set(TimerState.SUBMITTED);
        cancelScheduledTask();
        cleanRegistry();

        LOGGER.info(String.format("Executing exam submission for attempt %d (autoSubmitted=%b)", attemptId, isAutoSubmitted));

        // Execute submission database task in background thread
        ForkJoinPool.commonPool().execute(() -> {
            try {
                if (onSubmitTask != null) {
                    onSubmitTask.run();
                }

                // If DAO is available, ensure final status and remaining time are recorded
                if (studentExamDAO != null && attemptId != null && attemptId > 0) {
                    studentExamDAO.updateTimer(attemptId, 0);
                    StudentExam.ExamStatus finalStatus = isAutoSubmitted ?
                            StudentExam.ExamStatus.AUTO_SUBMITTED : StudentExam.ExamStatus.SUBMITTED;
                    studentExamDAO.updateStatus(attemptId, finalStatus);
                }

                // Notify UI of submission completion
                notifyListeners(l -> l.onSubmitted(isAutoSubmitted));

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed executing submission for attempt " + attemptId, e);
                notifyListeners(l -> l.onError(e));
            }
        });

        return true;
    }

    /**
     * Cancels the active timer countdown without submitting the exam.
     */
    public synchronized void cancel() {
        if (state.get() == TimerState.SUBMITTED || state.get() == TimerState.CANCELLED) {
            return;
        }

        state.set(TimerState.CANCELLED);
        cancelScheduledTask();
        cleanRegistry();

        LOGGER.info("Exam timer cancelled for attempt: " + attemptId);
        notifyListeners(ExamTimerListener::onCancelled);
    }

    /**
     * Releases executor resources. Must be called when the timer service is no longer needed.
     */
    public void shutdown() {
        cancel();
        if (ownsScheduler && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void cancelScheduledTask() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }
    }

    private void cleanRegistry() {
        if (attemptId != null) {
            ACTIVE_TIMERS.remove(attemptId, this);
        }
    }

    private void throttlePersist(long remainingSeconds) {
        if (studentExamDAO != null && attemptId != null && attemptId > 0) {
            if (lastPersistedSecond == -1 || (lastPersistedSecond - remainingSeconds) >= PERSISTENCE_THROTTLE_SECONDS) {
                lastPersistedSecond = remainingSeconds;
                ForkJoinPool.commonPool().execute(() -> {
                    try {
                        studentExamDAO.updateTimer(attemptId, (int) remainingSeconds);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Could not persist periodic timer update", e);
                    }
                });
            }
        }
    }

    public void addListener(ExamTimerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ExamTimerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Dispatches callbacks to all registered listeners safely on the Swing Event Dispatch Thread (EDT).
     */
    private void notifyListeners(java.util.function.Consumer<ExamTimerListener> consumer) {
        for (ExamTimerListener listener : listeners) {
            dispatchToEdt(() -> {
                try {
                    consumer.accept(listener);
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "Error in ExamTimerListener", t);
                }
            });
        }
    }

    private static void dispatchToEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static String formatTime(long seconds) {
        long s = Math.max(0, seconds);
        long hours = s / 3600;
        long minutes = (s % 3600) / 60;
        long remainingSec = s % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSec);
        } else {
            return String.format("%02d:%02d", minutes, remainingSec);
        }
    }

    private static IllegalStateException widespreadAttemptConflict(Long attemptId) {
        return new IllegalStateException(
                "A countdown timer task is already actively running for exam attempt ID: " + attemptId
        );
    }

    // Accessors

    public Long getAttemptId() {
        return attemptId;
    }

    public TimerState getState() {
        return state.get();
    }

    public boolean isRunning() {
        return state.get() == TimerState.RUNNING;
    }

    public boolean isSubmitted() {
        return state.get() == TimerState.SUBMITTED;
    }

    public boolean isCancelled() {
        return state.get() == TimerState.CANCELLED;
    }

    public long getRemainingSeconds() {
        long current = System.nanoTime();
        long rem = deadlineNano - current;
        return (rem <= 0) ? 0 : (rem + 999_999_999L) / 1_000_000_000L;
    }

    /**
     * Dedicated Daemon Thread Factory for clean executor naming and lifecycle.
     */
    private static class DaemonTimerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadIndex = new AtomicInteger(1);
        private final Long attemptId;

        DaemonTimerThreadFactory(Long attemptId) {
            this.attemptId = attemptId;
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = String.format("exam-timer-%s-worker-%d",
                    (attemptId != null ? attemptId : "demo"),
                    threadIndex.getAndIncrement());
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY + 1); // Slight priority boost for timing accuracy
            return thread;
        }
    }
}
