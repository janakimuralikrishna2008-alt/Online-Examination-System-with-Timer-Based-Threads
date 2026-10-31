import React, { useEffect, useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import confetti from 'canvas-confetti';
import { attemptsApi, aiApi } from '../../services/api';
import type { AttemptResultResponse } from '../../types';
import {
  CheckCircle,
  Sparkles,
  ArrowLeft,
  Printer,
  AlertTriangle
} from 'lucide-react';

export const ScorecardPage: React.FC = () => {
  const { attemptId } = useParams<{ attemptId: string }>();
  const [result, setResult] = useState<AttemptResultResponse | null>(null);
  const [aiInsights, setAiInsights] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [loadingAi, setLoadingAi] = useState<boolean>(false);
  const confettiFired = useRef<boolean>(false);

  const parsedId = attemptId ? parseInt(attemptId, 10) : 0;

  useEffect(() => {
    if (!parsedId) return;

    const loadResult = async () => {
      try {
        const data = await attemptsApi.getResult(parsedId);
        setResult(data);

        // If passed, fire celebration confetti once
        if (data.passed && !confettiFired.current) {
          confettiFired.current = true;
          confetti({
            particleCount: 120,
            spread: 70,
            origin: { y: 0.6 },
          });
        }

        // Fetch AI Performance Insights
        setLoadingAi(true);
        aiApi.getPerformanceInsights(parsedId)
          .then((res) => setAiInsights(res.insights))
          .catch(() => setAiInsights(
            `You achieved ${data.percentage}% in ${data.examTitle}. Review the explanations for any missed questions below to solidify your conceptual understanding.`
          ))
          .finally(() => setLoadingAi(false));

      } catch (err) {
        console.error('Failed to load scorecard', err);
      } finally {
        setLoading(false);
      }
    };

    loadResult();
  }, [parsedId]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem', color: '#64748b' }}>
        <p>Calculating exam results and generating scorecard...</p>
      </div>
    );
  }

  if (!result) {
    return (
      <div style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <div className="card" style={{ padding: '2.5rem' }}>
          <AlertTriangle size={48} color="#ef4444" style={{ margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, color: '#0f172a' }}>Scorecard Not Found</h2>
          <p style={{ color: '#64748b', margin: '1rem 0' }}>The requested examination attempt could not be retrieved.</p>
          <Link to="/student" className="btn btn-primary">Return to Dashboard</Link>
        </div>
      </div>
    );
  }

  const isAutoSubmitted = result.status === 'AUTO_SUBMITTED';

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto', padding: '2rem 1.5rem 4rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      {/* Top Action Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <Link
          to="/history"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.4rem',
            color: '#6ee7b7',
            fontSize: '0.9rem',
            fontWeight: 500,
            textDecoration: 'none',
          }}
        >
          <ArrowLeft size={16} />
          <span>Back to Exam History</span>
        </Link>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            onClick={() => window.print()}
            className="btn btn-secondary"
            style={{ fontSize: '0.85rem' }}
          >
            <Printer size={15} />
            <span>Print Scorecard</span>
          </button>
          <Link to="/exams" className="btn btn-primary" style={{ fontSize: '0.85rem', backgroundColor: '#059669' }}>
            <span>Take Another Exam</span>
          </Link>
        </div>
      </div>

      {/* Hero Scorecard Banner */}
      <div className="card" style={{
        padding: '2.5rem',
        borderRadius: '16px',
        background: result.passed
          ? 'linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%)'
          : 'linear-gradient(135deg, #fef2f2 0%, #fff1f2 100%)',
        border: result.passed ? '2px solid #a7f3d0' : '2px solid #fecaca',
        marginBottom: '2rem',
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '2rem',
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <span className={`badge ${result.passed ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.85rem', padding: '0.35rem 0.85rem' }}>
                {result.passed ? 'EXAMINATION PASSED' : 'DID NOT PASS'}
              </span>
              {isAutoSubmitted && (
                <span className="badge badge-warning" style={{ fontSize: '0.75rem' }}>
                  Auto-Submitted by Timer
                </span>
              )}
            </div>

            <h1 style={{ fontSize: '2rem', fontWeight: 800, color: '#1f2937', marginBottom: '0.35rem' }}>
              {result.examTitle}
            </h1>
            <p style={{ color: '#475569', fontSize: '0.95rem' }}>
              Subject: <strong>{result.subjectName}</strong> • Assessment Type: <strong>{result.examType}</strong>
            </p>
            <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.5rem' }}>
              Submitted: {new Date(result.endTime).toLocaleString()}
            </div>
          </div>

          {/* Large Circular / Pill Score Metric */}
          <div style={{
            backgroundColor: '#ffffff',
            padding: '1.5rem 2.25rem',
            borderRadius: '16px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.06)',
            textAlign: 'center',
            border: '1px solid #d1fae5',
          }}>
            <div style={{
              fontSize: '3rem',
              fontWeight: 900,
              lineHeight: 1,
              color: result.passed ? '#059669' : '#dc2626',
            }}>
              {result.percentage}%
            </div>
            <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#1f2937', marginTop: '0.35rem' }}>
              {result.earnedMarks} / {result.totalPossibleMarks} Marks
            </div>
            <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>
              Required to Pass: {result.passingMarks} Marks
            </div>
          </div>
        </div>
      </div>

      {/* AI Performance Insights Card */}
      <div className="card" style={{
        marginBottom: '2.5rem',
        borderLeft: '4px solid #059669',
        background: '#ffffff',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
          <Sparkles size={20} color="#059669" />
          <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#064e3b' }}>
            AI Examination Diagnostic & Insights
          </h3>
        </div>
        {loadingAi ? (
          <p style={{ color: '#64748b', fontSize: '0.9rem' }}>Generating personalized performance insights...</p>
        ) : (
          <p style={{ color: '#1f2937', fontSize: '0.95rem', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
            {aiInsights}
          </p>
        )}
      </div>

      {/* Question by Question Review Header */}
      <div style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#ffffff', marginBottom: '0.35rem' }}>
          Detailed Question Breakdown ({result.questionReviews.length} Questions)
        </h2>
        <p style={{ color: '#9ca3af', fontSize: '0.9rem' }}>
          Examine your chosen response compared to the verified answer and review conceptual explanations.
        </p>
      </div>

      {/* Questions Review List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {result.questionReviews.map((q, idx) => {
          return (
            <div
              key={q.questionId}
              className="card"
              style={{
                borderLeft: q.isCorrect ? '4px solid #10b981' : '4px solid #ef4444',
                padding: '1.75rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <span style={{
                    width: '26px',
                    height: '26px',
                    borderRadius: '50%',
                    backgroundColor: q.isCorrect ? '#ecfdf5' : '#fef2f2',
                    color: q.isCorrect ? '#059669' : '#dc2626',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                    fontSize: '0.8rem',
                  }}>
                    {idx + 1}
                  </span>
                  <span style={{ fontWeight: 700, fontSize: '0.95rem', color: '#0f172a' }}>
                    Question {idx + 1}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span className={`badge ${q.isCorrect ? 'badge-success' : 'badge-danger'}`}>
                    {q.isCorrect ? `+${q.marksAwarded} Marks` : `0 / ${q.possibleMarks} Marks`}
                  </span>
                </div>
              </div>

              {/* Question Text */}
              <div style={{ fontSize: '1.05rem', fontWeight: 600, color: '#0f172a', marginBottom: '1.25rem', lineHeight: 1.5 }}>
                {q.questionText}
              </div>

              {/* Options Breakdown */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', marginBottom: '1.25rem' }}>
                {(['A', 'B', 'C', 'D'] as const).map((opt) => {
                  const optText = q[`option${opt}` as keyof typeof q] as string;
                  const isUserSelection = q.selectedOption === opt;
                  const isCorrect = q.correctOption === opt;

                  let optBg = '#ffffff';
                  let optBorder = '1px solid #e2e8f0';
                  let optColor = '#334155';

                  if (isCorrect) {
                    optBg = '#f0fdf4';
                    optBorder = '2px solid #22c55e';
                    optColor = '#15803d';
                  } else if (isUserSelection && !q.isCorrect) {
                    optBg = '#fef2f2';
                    optBorder = '2px solid #ef4444';
                    optColor = '#b91c1c';
                  }

                  return (
                    <div
                      key={opt}
                      style={{
                        padding: '0.75rem 1rem',
                        borderRadius: '8px',
                        backgroundColor: optBg,
                        border: optBorder,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: '0.9rem',
                        color: optColor,
                        fontWeight: isCorrect || isUserSelection ? 600 : 400,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <span style={{ fontWeight: 800 }}>({opt})</span>
                        <span>{optText}</span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.75rem' }}>
                        {isCorrect && (
                          <span style={{ color: '#16a34a', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                            <CheckCircle size={14} /> Correct Answer
                          </span>
                        )}
                        {isUserSelection && (
                          <span style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            backgroundColor: q.isCorrect ? '#dcfce7' : '#fee2e2',
                            color: q.isCorrect ? '#166534' : '#991b1b',
                            fontWeight: 700
                          }}>
                            Your Choice
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Conceptual Explanation */}
              {q.explanation && (
                <div style={{
                  backgroundColor: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  padding: '0.85rem 1rem',
                  fontSize: '0.85rem',
                  color: '#475569',
                  lineHeight: 1.5,
                }}>
                  <strong style={{ color: '#1e293b' }}>Explanation: </strong>
                  {q.explanation}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
