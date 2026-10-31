import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { attemptsApi } from '../../services/api';
import type { StudentExamAttempt } from '../../types';
import { FileText, ExternalLink } from 'lucide-react';

export const StudentHistoryPage: React.FC = () => {
  const [history, setHistory] = useState<StudentExamAttempt[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const loadHistory = async () => {
      try {
        const data = await attemptsApi.getHistory();
        setHistory(data);
      } catch (err) {
        console.error('Failed to load history', err);
      } finally {
        setLoading(false);
      }
    };

    loadHistory();
  }, []);

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '2rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <span className="badge badge-primary" style={{ marginBottom: '0.4rem' }}>Records & Transcripts</span>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#ffffff' }}>
            My Examination History
          </h1>
          <p style={{ color: '#9ca3af', fontSize: '0.92rem' }}>
            Review all completed examinations, auto-submissions, and open past scorecards.
          </p>
        </div>

        <Link to="/exams" className="btn btn-primary" style={{ fontSize: '0.88rem', backgroundColor: '#059669' }}>
          Take New Examination
        </Link>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#6ee7b7' }}>
          <p>Loading examination transcripts...</p>
        </div>
      ) : history.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3.5rem', backgroundColor: '#ffffff' }}>
          <FileText size={48} color="#94a3b8" style={{ margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#1f2937' }}>No Examination Records Found</h3>
          <p style={{ color: '#64748b', fontSize: '0.92rem', marginTop: '0.5rem', marginBottom: '1.5rem' }}>
            You haven't completed any examinations or quizzes yet.
          </p>
          <Link to="/exams" className="btn btn-primary" style={{ backgroundColor: '#059669' }}>
            Explore Available Examinations
          </Link>
        </div>
      ) : (
        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Exam Title</th>
                <th>Subject</th>
                <th>Type</th>
                <th>Completed At</th>
                <th>Submission Mode</th>
                <th>Score</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {history.map((att) => {
                const attemptId = att.attemptId || att.id;
                const score = att.score ?? att.totalScore ?? 0;
                const totalPossible = att.totalMarks ?? 4;
                const isPassed = att.isPassed ?? att.passed ?? false;
                const completedTime = att.completedAt || att.endTime;
                const isAuto = att.status === 'AUTO_SUBMITTED';
                const rawType = att.assessmentType || att.examType || 'FORMAL';
                const isFormal = rawType.includes('FORMAL');
                const subjectName = att.subjectName || 'Computer Science & Software Engineering';

                return (
                  <tr key={attemptId}>
                    <td style={{ fontWeight: 600, color: '#0f172a' }}>
                      {att.examTitle}
                    </td>
                    <td>{subjectName}</td>
                    <td>
                      <span className={`badge ${isFormal ? 'badge-primary' : 'badge-purple'}`} style={{ fontSize: '0.72rem' }}>
                        {isFormal ? 'Formal Exam' : 'Practice Quiz'}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.82rem', color: '#64748b' }}>
                      {completedTime
                        ? new Date(completedTime).toLocaleString()
                        : (att.status === 'IN_PROGRESS' ? 'In Progress' : 'Completed')}
                    </td>
                    <td>
                      {isAuto ? (
                        <span className="badge badge-warning" style={{ fontSize: '0.72rem' }}>
                          Auto-Submitted
                        </span>
                      ) : (
                        <span className="badge badge-success" style={{ fontSize: '0.72rem' }}>
                          Submitted on Time
                        </span>
                      )}
                    </td>
                    <td style={{ fontWeight: 700, fontSize: '0.95rem' }}>
                      {score} / {totalPossible} pts
                    </td>
                    <td>
                      <span className={`badge ${isPassed ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.72rem' }}>
                        {isPassed ? 'PASSED' : 'FAILED'}
                      </span>
                    </td>
                    <td>
                      <Link
                        to={`/scorecard/${attemptId}`}
                        className="btn btn-secondary"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem', display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
                      >
                        <span>Scorecard</span>
                        <ExternalLink size={13} />
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
