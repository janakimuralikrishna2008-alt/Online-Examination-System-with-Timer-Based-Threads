import React, { useEffect, useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { analyticsApi, aiApi, examsApi, attemptsApi } from '../../services/api';
import type { StudentAnalytics, Exam, StudentExamAttempt } from '../../types';
import { AiStudyDrawer } from '../../components/AiStudyDrawer';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
  Award,
  BookOpen,
  CheckCircle,
  TrendingUp,
  Sparkles,
  ArrowRight,
  FileText,
  Clock,
  Play,
  CheckCircle2,
  AlertTriangle,
  RotateCcw
} from 'lucide-react';

export const StudentDashboard: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [analytics, setAnalytics] = useState<StudentAnalytics | null>(null);
  const [recommendations, setRecommendations] = useState<string[]>([]);
  const [allExams, setAllExams] = useState<Exam[]>([]);
  const [attempts, setAttempts] = useState<StudentExamAttempt[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Modals & Drawers
  const [aiDrawerOpen, setAiDrawerOpen] = useState<boolean>(false);
  const [examToStart, setExamToStart] = useState<Exam | null>(null);

  // Load all dashboard sources
  useEffect(() => {
    let isMounted = true;

    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [statsData, recData, examsData, historyData] = await Promise.all([
          analyticsApi.getStudentOverview(),
          aiApi.getRecommendations().catch(() => ({ recommendations: [
            'Review OOP inheritance and polymorphism before taking the Formal Java Exam.',
            'Complete topics under Spring Framework to boost your enterprise concepts.',
            'Take a quick Practice Quiz on Database Transactions.'
          ]})),
          examsApi.getAll().catch(() => []),
          attemptsApi.getHistory().catch(() => [])
        ]);

        if (!isMounted) return;

        setAnalytics(statsData);
        setAllExams(examsData || []);
        setAttempts(historyData || []);

        // Resilient recommendations extraction
        let recList: string[] = [];
        if (Array.isArray(recData)) {
          recList = recData.map((r: any) =>
            typeof r === 'string' ? r : (r.reason ? `${r.topicName || 'Topic'}: ${r.reason}` : String(r))
          );
        } else if (recData && Array.isArray((recData as any).recommendations)) {
          recList = (recData as any).recommendations.map((r: any) =>
            typeof r === 'string' ? r : (r.reason || String(r))
          );
        }

        if (recList.length === 0) {
          recList = [
            'Review OOP inheritance and polymorphism before taking the Formal Java Exam.',
            'Complete topics under Spring Framework to boost your enterprise concepts.',
            'Take a quick Practice Quiz on Database Transactions.'
          ];
        }
        setRecommendations(recList);
      } catch (err: any) {
        if (!isMounted) return;
        console.error('Failed to load student dashboard data', err);
        setError('Failed to load dashboard overview. Refresh or check your connection.');
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    fetchDashboardData();

    return () => {
      isMounted = false;
    };
  }, []);

  // Filter Active (IN_PROGRESS) attempts
  const activeAttempts = useMemo(() => {
    return attempts.filter((att) => att.status === 'IN_PROGRESS');
  }, [attempts]);

  // Filter Completed (SUBMITTED / AUTO_SUBMITTED) attempts
  const completedAttempts = useMemo(() => {
    return attempts.filter((att) => att.status === 'SUBMITTED' || att.status === 'AUTO_SUBMITTED');
  }, [attempts]);

  // Primary active attempt for top chronometer card
  const primaryActive = activeAttempts.length > 0 ? activeAttempts[0] : null;

  // Live countdown timer synchronized with server deadline
  const [activeSecondsRemaining, setActiveSecondsRemaining] = useState<number>(0);

  useEffect(() => {
    if (!primaryActive) {
      setActiveSecondsRemaining(0);
      return;
    }

    const baselineSeconds = primaryActive.remainingSeconds ?? 0;
    const startPerf = performance.now();
    setActiveSecondsRemaining(baselineSeconds);

    const interval = setInterval(() => {
      const elapsed = Math.floor((performance.now() - startPerf) / 1000);
      const remaining = Math.max(0, baselineSeconds - elapsed);
      setActiveSecondsRemaining(remaining);
      if (remaining <= 0) {
        clearInterval(interval);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [primaryActive]);

  // Format seconds to MM:SS
  const formatTimerString = (totalSeconds: number): string => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  const handleStartExamConfirmed = () => {
    if (examToStart) {
      const targetId = examToStart.id;
      setExamToStart(null);
      navigate(`/take-exam/${targetId}`);
    }
  };

  return (
    <div style={{ maxWidth: '1350px', margin: '0 auto', padding: '2rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      {/* Welcome & Status Header */}
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '2rem',
          backgroundColor: '#ffffff',
          padding: '1.75rem',
          borderRadius: '12px',
          border: '1px solid #d1fae5',
          boxShadow: '0 4px 6px -1px rgba(0,0,0,0.15)',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">Student Portal</span>
            <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>•</span>
            <span style={{ fontSize: '0.85rem', color: '#64748b' }}>{user?.email}</span>
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#1f2937', lineHeight: 1.2 }}>
            Welcome back, {user?.fullName || user?.username}!
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.92rem', marginTop: '0.25rem' }}>
            Track real-time examination sessions, review past scorecards, and continue curriculum prep.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button
            type="button"
            onClick={() => setAiDrawerOpen(true)}
            className="btn"
            style={{
              background: '#047857',
              color: '#ffffff',
            }}
          >
            <Sparkles size={16} />
            <span>AI Study Tutor</span>
          </button>
          <Link to="/exams" className="btn btn-primary" style={{ backgroundColor: '#059669' }}>
            <span>Browse All Exams</span>
            <ArrowRight size={16} />
          </Link>
        </div>
      </header>

      {/* Loading & Error States */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '5rem 1rem', color: '#6ee7b7' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              border: '3px solid #374151',
              borderTopColor: '#059669',
              borderRadius: '50%',
              margin: '0 auto 1rem auto',
              animation: 'spin 0.8s linear infinite',
            }}
          />
          <p style={{ fontWeight: 600 }}>Loading examination command center...</p>
        </div>
      ) : error ? (
        <div
          style={{
            backgroundColor: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#dc2626',
            padding: '1.25rem',
            borderRadius: '10px',
            marginBottom: '2rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span>{error}</span>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="btn btn-secondary"
            style={{ fontSize: '0.82rem', padding: '0.4rem 0.8rem' }}
          >
            <RotateCcw size={14} />
            <span>Retry</span>
          </button>
        </div>
      ) : (
        /* Approved Two-Column Command Center */
        <div className="student-dashboard-grid">
          {/* ================= LEFT COLUMN (70%): Examination Command Center ================= */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            {/* 1. Active In-Progress Examination Card */}
            {primaryActive ? (
              <section
                className="card"
                style={{
                  border: '1px solid #059669',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.08), 0 2px 4px -2px rgba(0, 0, 0, 0.08)',
                  backgroundColor: '#ffffff',
                  padding: '1.75rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
                      <span className="badge badge-primary" style={{ backgroundColor: '#ecfdf5', color: '#047857' }}>
                        ACTIVE EXAMINATION
                      </span>
                      <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
                        Session #{primaryActive.id || (primaryActive as any).attemptId}
                      </span>
                    </div>
                    <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#1f2937' }}>
                      {primaryActive.examTitle || `Exam #${primaryActive.examId}`}
                    </h2>
                    <div style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.2rem' }}>
                      Subject: {primaryActive.subjectName || 'Computer Science'} • {primaryActive.examType || 'FORMAL'}
                    </div>
                  </div>

                  {/* Digital Chronometer Readout */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      backgroundColor: activeSecondsRemaining <= 60 ? '#fef2f2' : activeSecondsRemaining <= 300 ? '#fffbeb' : '#f0fdf4',
                      border: `1px solid ${activeSecondsRemaining <= 60 ? '#f87171' : activeSecondsRemaining <= 300 ? '#fbbf24' : '#a7f3d0'}`,
                      borderRadius: '8px',
                      padding: '0.75rem 1.25rem',
                    }}
                  >
                    {activeSecondsRemaining <= 60 ? (
                      <AlertTriangle size={24} color="#dc2626" className="pulse-warning" />
                    ) : (
                      <Clock size={22} color={activeSecondsRemaining <= 300 ? '#d97706' : '#059669'} />
                    )}
                    <div>
                      <div
                        style={{
                          fontFamily: "'Consolas', monospace",
                          fontSize: '1.65rem',
                          fontWeight: 700,
                          lineHeight: 1,
                          color: activeSecondsRemaining <= 60 ? '#dc2626' : activeSecondsRemaining <= 300 ? '#d97706' : '#064e3b',
                          fontVariantNumeric: 'tabular-nums',
                        }}
                      >
                        {formatTimerString(activeSecondsRemaining)}
                      </div>
                      <div style={{ fontSize: '0.65rem', textTransform: 'uppercase', letterSpacing: '0.04em', color: '#64748b', marginTop: '0.2rem' }}>
                        {activeSecondsRemaining <= 60 ? 'Final Minute!' : activeSecondsRemaining <= 300 ? 'Warning: Low Time' : 'Time Remaining'}
                      </div>
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', paddingTop: '1rem', borderTop: '1px solid #f1f5f9' }}>
                  <p style={{ fontSize: '0.88rem', color: '#475569', margin: 0 }}>
                    Your timed examination thread is actively counting down on the server. Responses are saved in real-time.
                  </p>
                  <Link
                    to={`/take-exam/${primaryActive.examId}`}
                    className="btn btn-primary"
                    style={{ padding: '0.65rem 1.35rem', fontWeight: 700, backgroundColor: '#059669' }}
                  >
                    <span>Resume Assessment</span>
                    <ArrowRight size={16} />
                  </Link>
                </div>
              </section>
            ) : (
              <section
                className="card"
                style={{
                  backgroundColor: '#ffffff',
                  border: '1px solid #d1fae5',
                  padding: '1.25rem 1.5rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '1rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                  <div
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '8px',
                      backgroundColor: '#ecfdf5',
                      color: '#059669',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <CheckCircle2 size={22} />
                  </div>
                  <div>
                    <h2 style={{ fontSize: '1rem', fontWeight: 700, color: '#1f2937' }}>
                      No Active Examinations in Progress
                    </h2>
                    <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
                      You have no interrupted timed attempts. Select any available exam below when you are ready.
                    </p>
                  </div>
                </div>
                <Link to="/exams" className="btn btn-secondary" style={{ fontSize: '0.85rem' }}>
                  <span>View All Exams</span>
                  <ArrowRight size={14} />
                </Link>
              </section>
            )}

            {/* 2. Upcoming & Available Examinations Section */}
            <section>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#ffffff' }}>
                    Available Examinations & Practice Quizzes
                  </h2>
                  <p style={{ fontSize: '0.82rem', color: '#9ca3af' }}>
                    Standardized timed assessments with thread-safe evaluation and instant scorecards.
                  </p>
                </div>
                <Link to="/exams" style={{ fontSize: '0.85rem', fontWeight: 600, color: '#6ee7b7' }}>
                  Catalog View →
                </Link>
              </div>

              {allExams.length === 0 ? (
                <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem', color: '#64748b' }}>
                  <FileText size={32} color="#cbd5e1" style={{ margin: '0 auto 0.75rem auto' }} />
                  <p style={{ fontWeight: 600 }}>No examinations currently published.</p>
                  <p style={{ fontSize: '0.82rem', color: '#94a3b8', marginTop: '0.25rem' }}>
                    Check back soon or consult your course instructor for upcoming schedules.
                  </p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                  {allExams.slice(0, 5).map((exam) => {
                    const existingAttempt = attempts.find((a) => a.examId === exam.id);
                    const isCompleted = existingAttempt?.status === 'SUBMITTED' ||
                                        existingAttempt?.status === 'AUTO_SUBMITTED' ||
                                        (exam as any).studentAttemptStatus === 'PASSED' ||
                                        (exam as any).studentAttemptStatus === 'FAILED';
                    const isInProgress = existingAttempt?.status === 'IN_PROGRESS' ||
                                         (exam as any).studentAttemptStatus === 'IN_PROGRESS';
                    const attemptId = existingAttempt?.attemptId || existingAttempt?.id;
                    const isPassed = existingAttempt?.isPassed ?? existingAttempt?.passed ?? ((exam as any).studentAttemptStatus === 'PASSED');

                    return (
                      <article
                        key={exam.id}
                        className="card"
                        style={{
                          padding: '1.25rem 1.5rem',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          flexWrap: 'wrap',
                          gap: '1rem',
                          transition: 'border-color 0.2s ease',
                        }}
                      >
                        <div style={{ flex: 1, minWidth: '280px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                            <span
                              className={`badge ${exam.examType === 'FORMAL' ? 'badge-primary' : 'badge-purple'}`}
                              style={{ fontSize: '0.7rem' }}
                            >
                              {exam.examType === 'FORMAL' ? 'Formal Assessment' : 'Practice Quiz'}
                            </span>
                            {isCompleted ? (
                              <span
                                className={`badge ${isPassed ? 'badge-success' : 'badge-danger'}`}
                                style={{ fontSize: '0.7rem' }}
                              >
                                {isPassed ? 'Completed (Passed)' : 'Completed (Did Not Pass)'}
                              </span>
                            ) : isInProgress ? (
                              <span className="badge badge-warning" style={{ fontSize: '0.7rem' }}>
                                In Progress
                              </span>
                            ) : null}
                            <span style={{ fontSize: '0.78rem', color: '#64748b', fontWeight: 500 }}>
                              {exam.subjectName || 'General Engineering'}
                            </span>
                          </div>
                          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a' }}>
                            {exam.title}
                          </h3>
                          <p style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.25rem', lineHeight: 1.4 }}>
                            {exam.description || 'Comprehensive evaluation assessing core technical competency.'}
                          </p>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', marginTop: '0.75rem', fontSize: '0.8rem', color: '#475569' }}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                              <Clock size={14} color="#64748b" />
                              <strong>{exam.durationMinutes} min</strong> duration
                            </span>
                            <span>•</span>
                            <span>
                              <strong>{exam.questionCount || 4}</strong> questions
                            </span>
                            <span>•</span>
                            <span>
                              Pass: <strong>{exam.passingMarks} / {exam.totalMarks} pts</strong>
                            </span>
                          </div>
                        </div>

                        <div>
                          {isCompleted && attemptId ? (
                            <Link
                              to={`/scorecard/${attemptId}`}
                              className="btn btn-secondary"
                              style={{ fontSize: '0.88rem', padding: '0.55rem 1.15rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                            >
                              <Award size={15} color="#059669" />
                              <span>View Scorecard</span>
                            </Link>
                          ) : isInProgress ? (
                            <Link
                              to={`/take-exam/${exam.id}`}
                              className="btn btn-warning"
                              style={{ fontSize: '0.88rem', padding: '0.55rem 1.15rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                            >
                              <RotateCcw size={15} />
                              <span>Resume Assessment</span>
                            </Link>
                          ) : (
                            <button
                              type="button"
                              onClick={() => setExamToStart(exam)}
                              className="btn btn-primary"
                              style={{ fontSize: '0.88rem', padding: '0.55rem 1.15rem', backgroundColor: '#059669' }}
                            >
                              <Play size={15} />
                              <span>Start Exam</span>
                            </button>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>

            {/* 3. Completed Examinations & Scorecard History Section */}
            <section>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#ffffff' }}>
                    Completed Examination Scorecards
                  </h2>
                  <p style={{ fontSize: '0.82rem', color: '#9ca3af' }}>
                    Verifiable scores, answer accuracy breakdowns, and teacher feedback.
                  </p>
                </div>
                <Link to="/history" style={{ fontSize: '0.85rem', fontWeight: 600, color: '#6ee7b7' }}>
                  Complete History ({completedAttempts.length}) →
                </Link>
              </div>

              {completedAttempts.length === 0 ? (
                <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem', color: '#64748b' }}>
                  <Award size={32} color="#cbd5e1" style={{ margin: '0 auto 0.75rem auto' }} />
                  <p style={{ fontWeight: 600 }}>No examinations completed yet.</p>
                  <p style={{ fontSize: '0.82rem', color: '#94a3b8', marginTop: '0.25rem' }}>
                    Complete an exam or practice quiz to record your verifiable scorecard and progress.
                  </p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {completedAttempts.slice(0, 5).map((att: any) => {
                    const attemptId = att.attemptId || att.id;
                    const score = att.score ?? att.totalScore ?? 0;
                    const totalPossible = att.totalMarks ?? 4;
                    const pct = att.percentage ?? Math.round((score / totalPossible) * 100);
                    const isPassed = att.isPassed ?? att.passed ?? (pct >= 75);

                    return (
                      <article
                        key={attemptId}
                        className="card"
                        style={{
                          padding: '1rem 1.25rem',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          flexWrap: 'wrap',
                          gap: '0.75rem',
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#0f172a' }}>
                            {att.examTitle || `Exam #${att.examId}`}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: '#64748b', display: 'flex', gap: '0.5rem', marginTop: '0.2rem' }}>
                            <span>{att.subjectName || 'Computer Science'}</span>
                            <span>•</span>
                            <span>{att.assessmentType || att.examType || 'FORMAL'}</span>
                            {att.completedAt && (
                              <>
                                <span>•</span>
                                <span>{new Date(att.completedAt).toLocaleDateString()}</span>
                              </>
                            )}
                          </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                          <div style={{ textAlign: 'right' }}>
                            <div
                              style={{
                                fontWeight: 800,
                                fontSize: '1.05rem',
                                color: isPassed ? '#059669' : '#dc2626',
                                fontFamily: "'Consolas', monospace",
                                fontVariantNumeric: 'tabular-nums',
                              }}
                            >
                              {score} / {totalPossible} pts ({pct}%)
                            </div>
                            <span
                              className={`badge ${isPassed ? 'badge-success' : 'badge-danger'}`}
                              style={{ fontSize: '0.68rem', padding: '0.15rem 0.5rem', marginTop: '0.15rem' }}
                            >
                              {isPassed ? 'PASSED' : 'FAILED'}
                            </span>
                          </div>

                          <Link
                            to={`/scorecard/${attemptId}`}
                            className="btn btn-secondary"
                            style={{ padding: '0.45rem 0.85rem', fontSize: '0.82rem' }}
                          >
                            <span>View Scorecard</span>
                          </Link>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          </div>

          {/* ================= RIGHT COLUMN (30%): Metrics & AI Study Tutor ================= */}
          <aside style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* Performance Overview KPI Card */}
            <div className="card" style={{ backgroundColor: '#ffffff', border: '1px solid #d1fae5' }}>
              <h2 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#1f2937', marginBottom: '1.25rem' }}>
                Performance Overview
              </h2>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.85rem' }}>
                {/* Total Attempts */}
                <div
                  style={{
                    backgroundColor: '#f9fafb',
                    padding: '0.9rem',
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#059669', marginBottom: '0.35rem' }}>
                    <FileText size={16} />
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>Exams</span>
                  </div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1f2937', lineHeight: 1.1 }}>
                    {analytics?.totalAttempts ?? 0}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: '#9ca3af' }}>Taken to date</div>
                </div>

                {/* Passed Exams */}
                <div
                  style={{
                    backgroundColor: '#f9fafb',
                    padding: '0.9rem',
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#10b981', marginBottom: '0.35rem' }}>
                    <CheckCircle size={16} />
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>Passed</span>
                  </div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1f2937', lineHeight: 1.1 }}>
                    {analytics?.passedAttempts ?? (analytics as any)?.passedCount ?? 0}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: '#9ca3af' }}>Above pass mark</div>
                </div>

                {/* Average Score */}
                <div
                  style={{
                    backgroundColor: '#f9fafb',
                    padding: '0.9rem',
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#047857', marginBottom: '0.35rem' }}>
                    <TrendingUp size={16} />
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>Average</span>
                  </div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1f2937', lineHeight: 1.1 }}>
                    {analytics?.averageScorePercentage != null ? `${analytics.averageScorePercentage}%` : '0%'}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: '#9ca3af' }}>Cumulative score</div>
                </div>

                {/* Study Notes Read */}
                <div
                  style={{
                    backgroundColor: '#f9fafb',
                    padding: '0.9rem',
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#f59e0b', marginBottom: '0.35rem' }}>
                    <BookOpen size={16} />
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>Notes</span>
                  </div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1f2937', lineHeight: 1.1 }}>
                    {analytics?.completedMaterialsCount ?? 0}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: '#9ca3af' }}>Materials read</div>
                </div>
              </div>
            </div>

            {/* AI Study Recommendations Card */}
            <div
              className="card"
              style={{
                backgroundColor: '#ffffff',
                border: '1px solid #d1fae5',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <Sparkles size={18} color="#059669" />
                <h2 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#1f2937' }}>
                  AI Study Tutor Insights
                </h2>
              </div>
              <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '1.25rem', lineHeight: 1.4 }}>
                Diagnostic recommendations tailored to your recent examination performances:
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.5rem' }}>
                {recommendations.map((rec, i) => (
                  <div
                    key={i}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: '0.75rem',
                      padding: '0.75rem',
                      backgroundColor: '#f9fafb',
                      borderRadius: '8px',
                      border: '1px solid #e5e7eb',
                      fontSize: '0.84rem',
                      color: '#374151',
                      lineHeight: 1.4,
                    }}
                  >
                    <div
                      style={{
                        width: '20px',
                        height: '20px',
                        borderRadius: '50%',
                        backgroundColor: '#059669',
                        color: '#ffffff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        flexShrink: 0,
                        marginTop: '2px',
                      }}
                    >
                      {i + 1}
                    </div>
                    <span>{rec}</span>
                  </div>
                ))}
              </div>

              <div style={{ display: 'flex', gap: '0.65rem' }}>
                <Link
                  to="/subjects"
                  className="btn btn-secondary"
                  style={{ flex: 1, fontSize: '0.82rem', padding: '0.5rem 0.75rem' }}
                >
                  <BookOpen size={14} />
                  <span>Study Notes</span>
                </Link>
                <Link
                  to="/exams?type=PRACTICE"
                  className="btn btn-secondary"
                  style={{ flex: 1, fontSize: '0.82rem', padding: '0.5rem 0.75rem' }}
                >
                  <Award size={14} />
                  <span>Quizzes</span>
                </Link>
              </div>
            </div>
          </aside>
        </div>
      )}

      {/* Confirmation Modal to Begin Exam */}
      <ConfirmModal
        isOpen={Boolean(examToStart)}
        title={examToStart?.examType === 'FORMAL' ? 'Start Formal Examination?' : 'Start Practice Quiz?'}
        message={
          examToStart
            ? `You are about to start "${examToStart.title}" (${examToStart.durationMinutes} minutes). Once initiated, your thread-based countdown begins immediately and cannot be paused. Your answers will be automatically submitted when the deadline expires.`
            : ''
        }
        confirmLabel="I am Ready — Begin Exam"
        cancelLabel="Not Yet"
        variant="primary"
        onConfirm={handleStartExamConfirmed}
        onCancel={() => setExamToStart(null)}
      />

      {/* Embedded AI Study Assistant Drawer */}
      <AiStudyDrawer
        isOpen={aiDrawerOpen}
        onClose={() => setAiDrawerOpen(false)}
        contextTitle="General Student Study"
      />
    </div>
  );
};
