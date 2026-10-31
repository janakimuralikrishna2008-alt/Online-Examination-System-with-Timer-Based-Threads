import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { attemptsApi } from '../../services/api';
import type { StartAttemptResponse, AttemptQuestionView } from '../../types';
import { TimerHeader } from '../../components/TimerHeader';
import { QuestionPalette } from '../../components/QuestionPalette';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
  ChevronLeft,
  ChevronRight,
  Flag,
  AlertCircle,
  RotateCcw,
  Send,
  Award
} from 'lucide-react';

export const ExamTakingPage: React.FC = () => {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();

  const [attempt, setAttempt] = useState<StartAttemptResponse | null>(null);
  const [questions, setQuestions] = useState<AttemptQuestionView[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [completedAttemptId, setCompletedAttemptId] = useState<number | null>(null);

  // Submit confirmation modal
  const [submitModalOpen, setSubmitModalOpen] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const parsedExamId = examId ? parseInt(examId, 10) : 0;

  // Track if submission already fired to prevent double-submitting
  const submittedRef = useRef<boolean>(false);

  // Load / Start attempt
  useEffect(() => {
    if (!parsedExamId) return;

    const startExam = async () => {
      try {
        const data = await attemptsApi.startAttempt(parsedExamId);
        setAttempt(data);
        setQuestions(data.questions);
      } catch (err: any) {
        console.error('Failed to start exam', err);
        const msg = err.response?.data?.message || 'Failed to start examination. Please try again.';
        setError(msg);

        // If candidate already finished attempt, resolve attemptId for direct scorecard viewing
        if (msg.includes('already completed') || msg.includes('Retakes are not permitted')) {
          try {
            const hist = await attemptsApi.getHistory();
            const match = hist.find((h: any) => h.examId === parsedExamId);
            if (match) {
              setCompletedAttemptId((match.attemptId || match.id) ?? null);
            }
          } catch (lookupErr) {
            console.warn('Could not resolve past attempt id', lookupErr);
          }
        }
      } finally {
        setLoading(false);
      }
    };

    startExam();
  }, [parsedExamId]);

  // Option selection handler
  const handleSelectOption = async (option: 'A' | 'B' | 'C' | 'D' | null) => {
    if (!attempt || questions.length === 0) return;
    const currentQuestion = questions[currentIndex];
    if (!currentQuestion) return;

    // Update local state instantly
    setQuestions((prev) =>
      prev.map((q, idx) => (idx === currentIndex ? { ...q, selectedOption: option } : q))
    );

    // Save answer to server in background
    try {
      await attemptsApi.saveAnswer(attempt.attemptId, currentQuestion.questionId, option);
    } catch (err) {
      console.warn('Failed to save answer asynchronously', err);
    }
  };

  // Flag toggle handler
  const handleToggleFlag = (index: number) => {
    setQuestions((prev) =>
      prev.map((q, idx) => (idx === index ? { ...q, isFlagged: !q.isFlagged } : q))
    );
  };

  // Submit attempt handler (manual or timer expired)
  const handleSubmitExam = useCallback(async () => {
    if (!attempt || submittedRef.current) return;
    submittedRef.current = true;
    setIsSubmitting(true);
    setSubmitModalOpen(false);

    try {
      const result = await attemptsApi.submitAttempt(attempt.attemptId);
      navigate(`/scorecard/${result.attemptId}`, { replace: true });
    } catch (err: any) {
      console.error('Failed to submit exam', err);
      // Even if error, attempt to view scorecard as server may have already auto-submitted
      navigate(`/scorecard/${attempt.attemptId}`, { replace: true });
    } finally {
      setIsSubmitting(false);
    }
  }, [attempt, navigate]);

  // Handle timer running out
  const handleTimeUp = useCallback(() => {
    console.warn('Exam timer expired! Automatically submitting examination.');
    handleSubmitExam();
  }, [handleSubmitExam]);

  if (loading) {
    return (
      <div style={{
        minHeight: '70vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'column',
        gap: '1rem',
        color: '#6ee7b7',
        background: '#111827',
      }}>
        <div style={{
          width: '38px',
          height: '38px',
          border: '3px solid #374151',
          borderTopColor: '#059669',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite'
        }} />
        <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
        <p style={{ fontWeight: 600 }}>Initializing examination session & timer...</p>
      </div>
    );
  }

  if (error || !attempt || questions.length === 0) {
    const isCompletedError = error?.includes('already completed') || error?.includes('Retakes are not permitted');

    return (
      <div style={{ maxWidth: '640px', margin: '4rem auto', padding: '1.5rem' }}>
        <div className="card" style={{ textAlign: 'center', padding: '2.5rem' }}>
          <AlertCircle size={48} color={isCompletedError ? "#f59e0b" : "#ef4444"} style={{ margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.5rem' }}>
            {isCompletedError ? 'Examination Already Completed' : 'Unable to Start Examination'}
          </h2>
          <p style={{ color: '#64748b', fontSize: '0.95rem', marginBottom: '1.75rem', lineHeight: 1.5 }}>
            {error || 'This exam has no questions assigned yet or has already concluded.'}
          </p>
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            {isCompletedError && completedAttemptId ? (
              <button
                type="button"
                onClick={() => navigate(`/scorecard/${completedAttemptId}`)}
                className="btn btn-primary"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
              >
                <Award size={16} />
                <span>View Examination Scorecard</span>
              </button>
            ) : null}
            <button
              type="button"
              onClick={() => navigate('/student')}
              className="btn btn-secondary"
            >
              Return to Dashboard
            </button>
            <button
              type="button"
              onClick={() => navigate('/exams')}
              className="btn btn-secondary"
            >
              Browse Other Exams
            </button>
          </div>
        </div>
      </div>
    );
  }

  const currentQ = questions[currentIndex];
  const answeredCount = questions.filter((q) => q.selectedOption != null).length;
  const unansweredCount = questions.length - answeredCount;
  const flaggedCount = questions.filter((q) => q.isFlagged).length;

  return (
    <div style={{ background: '#111827', minHeight: 'calc(100vh - 65px)', paddingBottom: '3rem' }}>
      {/* Authoritative Timer Header */}
      <TimerHeader
        initialRemainingSeconds={attempt.remainingSeconds}
        deadlineIso={attempt.deadlineTime}
        onTimeUp={handleTimeUp}
        examTitle={attempt.examTitle}
        subjectName={attempt.subjectName}
      />

      <div style={{ maxWidth: '1280px', margin: '1.5rem auto 0 auto', padding: '0 1.5rem' }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 1fr) 300px',
          gap: '1.5rem',
          alignItems: 'start',
        }}>
          {/* Main Question Card Area */}
          <div className="card" style={{ padding: '2rem', backgroundColor: '#ffffff' }}>
            {/* Question Top Meta */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingBottom: '1rem',
              borderBottom: '1px solid #f1f5f9',
              marginBottom: '1.5rem',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span className="badge badge-primary" style={{ fontSize: '0.85rem', padding: '0.3rem 0.75rem' }}>
                  Question {currentIndex + 1} of {questions.length}
                </span>
                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#64748b' }}>
                  Marks: {currentQ.marks} pts
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => handleToggleFlag(currentIndex)}
                  style={{
                    background: currentQ.isFlagged ? '#fef3c7' : '#f9fafb',
                    border: `1px solid ${currentQ.isFlagged ? '#f59e0b' : '#d1d5db'}`,
                    color: currentQ.isFlagged ? '#b45309' : '#64748b',
                    borderRadius: '6px',
                    padding: '0.35rem 0.65rem',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    cursor: 'pointer',
                  }}
                >
                  <Flag size={14} fill={currentQ.isFlagged ? '#f59e0b' : 'none'} color={currentQ.isFlagged ? '#b45309' : '#64748b'} />
                  <span>{currentQ.isFlagged ? 'Flagged' : 'Flag Question'}</span>
                </button>

                {currentQ.selectedOption && (
                  <button
                    type="button"
                    onClick={() => handleSelectOption(null)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#94a3b8',
                      fontSize: '0.8rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem',
                      cursor: 'pointer',
                    }}
                  >
                    <RotateCcw size={13} />
                    <span>Clear Choice</span>
                  </button>
                )}
              </div>
            </div>

            {/* Question Text */}
            <div style={{
              fontSize: '1.15rem',
              fontWeight: 600,
              color: '#1f2937',
              lineHeight: 1.6,
              marginBottom: '2rem',
              whiteSpace: 'pre-wrap',
            }}>
              {currentQ.questionText}
            </div>

            {/* Options List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', marginBottom: '2.5rem' }}>
              {(['A', 'B', 'C', 'D'] as const).map((optKey) => {
                const optText = currentQ[`option${optKey}` as keyof AttemptQuestionView] as string;
                const isSelected = currentQ.selectedOption === optKey;

                return (
                  <label
                    key={optKey}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '1rem',
                      padding: '1rem 1.25rem',
                      borderRadius: '10px',
                      backgroundColor: isSelected ? '#ecfdf5' : '#ffffff',
                      border: isSelected ? '2px solid #059669' : '1px solid #e5e7eb',
                      boxShadow: isSelected ? '0 0 0 2px rgba(5, 150, 105, 0.2)' : 'none',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <input
                      type="radio"
                      name={`question_${currentQ.questionId}`}
                      checked={isSelected}
                      onChange={() => handleSelectOption(optKey)}
                      style={{
                        width: '18px',
                        height: '18px',
                        accentColor: '#059669',
                        cursor: 'pointer',
                      }}
                    />
                    <div style={{
                      width: '26px',
                      height: '26px',
                      borderRadius: '50%',
                      backgroundColor: isSelected ? '#059669' : '#f3f4f6',
                      color: isSelected ? '#ffffff' : '#374151',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.85rem',
                      flexShrink: 0,
                    }}>
                      {optKey}
                    </div>
                    <span style={{
                      fontSize: '0.95rem',
                      color: isSelected ? '#065f46' : '#1f2937',
                      fontWeight: isSelected ? 600 : 400,
                      lineHeight: 1.4,
                    }}>
                      {optText}
                    </span>
                  </label>
                );
              })}
            </div>

            {/* Bottom Navigation Buttons */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: '1.25rem',
              borderTop: '1px solid #f1f5f9',
            }}>
              <button
                type="button"
                disabled={currentIndex === 0}
                onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
                className="btn btn-secondary"
                style={{ opacity: currentIndex === 0 ? 0.5 : 1 }}
              >
                <ChevronLeft size={16} />
                <span>Previous</span>
              </button>

              <div style={{ display: 'flex', gap: '0.75rem' }}>
                {currentIndex < questions.length - 1 ? (
                  <button
                    type="button"
                    onClick={() => setCurrentIndex((prev) => Math.min(questions.length - 1, prev + 1))}
                    className="btn btn-primary"
                  >
                    <span>Next</span>
                    <ChevronRight size={16} />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => setSubmitModalOpen(true)}
                    className="btn btn-success"
                    style={{ padding: '0.65rem 1.5rem' }}
                  >
                    <Send size={16} />
                    <span>Submit Exam</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Right Column: Question Palette & Submission Trigger */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <QuestionPalette
              questions={questions}
              currentIndex={currentIndex}
              onSelectQuestion={(idx) => setCurrentIndex(idx)}
              onToggleFlag={handleToggleFlag}
            />

            <button
              type="button"
              onClick={() => setSubmitModalOpen(true)}
              disabled={isSubmitting}
              className="btn btn-success"
              style={{
                width: '100%',
                padding: '0.85rem',
                fontSize: '1rem',
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
              }}
            >
              <Send size={18} />
              <span>{isSubmitting ? 'Submitting Answers...' : 'Finish & Submit Exam'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Submit Confirmation Modal */}
      <ConfirmModal
        isOpen={submitModalOpen}
        title="Confirm Examination Submission"
        message={`You are about to submit your examination. You have answered ${answeredCount} of ${questions.length} questions.${
          unansweredCount > 0 ? ` WARNING: ${unansweredCount} questions are still unanswered!` : ''
        }${
          flaggedCount > 0 ? ` (${flaggedCount} questions are flagged for review).` : ''
        } Once submitted, your answers will be graded immediately.`}
        confirmLabel="Yes, Submit My Exam"
        cancelLabel="Continue Working"
        variant={unansweredCount > 0 ? 'warning' : 'primary'}
        onConfirm={handleSubmitExam}
        onCancel={() => setSubmitModalOpen(false)}
      />
    </div>
  );
};
