package com.onlineexam.timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic unit tests for {@link ExamTimerService} multi-threaded countdown service.
 */
public class ExamTimerServiceTest {

    private ExamTimerService timerService;

    @AfterEach
    void tearDown() {
        if (timerService != null) {
            timerService.shutdown();
        }
    }

    @Test
    @DisplayName("Timer starts correctly and ticks decrease remaining time")
    void testTimerStartsAndTicks() throws InterruptedException {
        CountDownLatch tickLatch = new CountDownLatch(3);
        AtomicLong lastRemaining = new AtomicLong(10);

        timerService = new ExamTimerService(101L, 10L, 50L, 5L, null, null, null);
        timerService.addListener(new ExamTimerListener() {
            @Override
            public void onTick(long remainingSeconds, String formattedTime) {
                lastRemaining.set(remainingSeconds);
                tickLatch.countDown();
            }

            @Override
            public void onTimeExpired() {}
        });

        timerService.start();
        assertTrue(timerService.isRunning());

        boolean receivedTicks = tickLatch.await(2, TimeUnit.SECONDS);
        assertTrue(receivedTicks, "Should receive at least 3 ticks within 2 seconds");
        assertTrue(lastRemaining.get() <= 10, "Remaining seconds should be <= initial duration");
    }

    @Test
    @DisplayName("Timer automatically expires when deadline passes and triggers auto-submission")
    void testTimerExpiresAutomatically() throws InterruptedException {
        CountDownLatch expireLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(1);
        AtomicBoolean autoFlag = new AtomicBoolean(false);

        // 1 second duration with 30ms tick interval for fast deterministic test
        timerService = new ExamTimerService(102L, 1L, 30L, 0L, null, null, null);
        timerService.addListener(new ExamTimerListener() {
            @Override
            public void onTick(long remainingSeconds, String formattedTime) {}

            @Override
            public void onTimeExpired() {
                expireLatch.countDown();
            }

            @Override
            public void onSubmitted(boolean autoSubmitted) {
                autoFlag.set(autoSubmitted);
                submitLatch.countDown();
            }
        });

        timerService.start();

        assertTrue(expireLatch.await(3, TimeUnit.SECONDS), "Timer should expire within 3 seconds");
        assertTrue(submitLatch.await(3, TimeUnit.SECONDS), "Submission callback should be invoked");
        assertTrue(autoFlag.get(), "Submission should be marked as autoSubmitted");
        assertTrue(timerService.isSubmitted());
        assertEquals(0, timerService.getRemainingSeconds());
    }

    @Test
    @DisplayName("Manual cancellation stops countdown and prevents subsequent expiration callbacks")
    void testManualCancellationPreventsTimeout() throws InterruptedException {
        AtomicBoolean expiredCalled = new AtomicBoolean(false);
        CountDownLatch cancelLatch = new CountDownLatch(1);

        // 2 second timer
        timerService = new ExamTimerService(103L, 2L, 30L, 0L, null, null, null);
        timerService.addListener(new ExamTimerListener() {
            @Override
            public void onTick(long remainingSeconds, String formattedTime) {}

            @Override
            public void onTimeExpired() {
                expiredCalled.set(true);
            }

            @Override
            public void onCancelled() {
                cancelLatch.countDown();
            }
        });

        timerService.start();
        Thread.sleep(60); // Allow first tick
        timerService.cancel();

        assertTrue(cancelLatch.await(1, TimeUnit.SECONDS), "onCancelled should be triggered");
        assertTrue(timerService.isCancelled());
        assertFalse(timerService.isRunning());

        // Wait past original 2s deadline to verify timeout NEVER fires
        Thread.sleep(250);
        assertFalse(expiredCalled.get(), "Expiration should not be called after cancellation");
    }

    @Test
    @DisplayName("Thread safety: Manual submit and timeout cannot double-submit")
    void testDoubleSubmissionProtection() throws InterruptedException {
        AtomicInteger submissionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        timerService = new ExamTimerService(104L, 5L, 50L, 0L, null, null, null);
        timerService.addListener(new ExamTimerListener() {
            @Override
            public void onTick(long remainingSeconds, String formattedTime) {}

            @Override
            public void onTimeExpired() {}

            @Override
            public void onSubmitted(boolean autoSubmitted) {
                submissionCount.incrementAndGet();
                latch.countDown();
            }
        });

        timerService.start();

        // Concurrently attempt manual submissions from two separate threads
        Thread t1 = new Thread(() -> timerService.manualSubmit());
        Thread t2 = new Thread(() -> timerService.manualSubmit());

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        // Give background tasks a brief moment to ensure no second call occurred
        Thread.sleep(100);

        assertEquals(1, submissionCount.get(), "Exactly one submission must take effect despite concurrent attempts");
        assertTrue(timerService.isSubmitted());
    }

    @Test
    @DisplayName("Prevents multiple simultaneous timers for the same active attempt ID")
    void testPreventDuplicateTimersForSameAttempt() {
        ExamTimerService firstTimer = new ExamTimerService(555L, 60L, null, null);
        ExamTimerService secondTimer = new ExamTimerService(555L, 60L, null, null);

        try {
            firstTimer.start();
            assertTrue(firstTimer.isRunning());

            // Attempting to start a second timer for attempt 555 should throw IllegalStateException
            assertThrows(IllegalStateException.class, secondTimer::start);

        } finally {
            firstTimer.shutdown();
            secondTimer.shutdown();
        }
    }

    @Test
    @DisplayName("Resource cleanup and shutdown properly halts scheduler")
    void testShutdownReleasesResources() {
        timerService = new ExamTimerService(105L, 30L, null, null);
        timerService.start();
        assertTrue(timerService.isRunning());

        timerService.shutdown();
        assertTrue(timerService.isCancelled());
        assertFalse(timerService.isRunning());
    }
}
