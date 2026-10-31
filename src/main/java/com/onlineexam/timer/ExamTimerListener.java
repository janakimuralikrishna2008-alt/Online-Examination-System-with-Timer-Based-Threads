package com.onlineexam.timer;

/**
 * Listener interface for monitoring examination countdown timer events.
 * Implementations updating Swing UI components can rely on {@link ExamTimerService}
 * ensuring all callbacks are dispatched safely to the Event Dispatch Thread (EDT).
 */
public interface ExamTimerListener {

    /**
     * Fired at each periodic timer tick.
     *
     * @param remainingSeconds remaining seconds until deadline
     * @param formattedTime formatted display string (e.g., "14:59")
     */
    void onTick(long remainingSeconds, String formattedTime);

    /**
     * Fired when remaining time reaches or falls below a warning threshold (e.g., 60 seconds).
     *
     * @param remainingSeconds remaining seconds
     */
    default void onWarning(long remainingSeconds) {}

    /**
     * Fired when the deadline has expired and auto-submission is being initiated.
     */
    void onTimeExpired();

    /**
     * Fired when the exam attempt has been finalized (either via auto-submission or manual submission).
     *
     * @param autoSubmitted true if triggered by deadline expiry, false if manually submitted
     */
    default void onSubmitted(boolean autoSubmitted) {}

    /**
     * Fired if the timer is manually cancelled or stopped before completion.
     */
    default void onCancelled() {}

    /**
     * Fired if an unhandled error occurs during timer execution or persistence.
     *
     * @param error the throwable encountered
     */
    default void onError(Throwable error) {}
}
