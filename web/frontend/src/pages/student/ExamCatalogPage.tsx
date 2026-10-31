import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { examsApi, subjectsApi, attemptsApi } from '../../services/api';
import type { Exam, Subject, StudentExamAttempt } from '../../types';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
  Clock,
  FileQuestion,
  Filter,
  Play,
  Award,
  RotateCcw
} from 'lucide-react';

export const ExamCatalogPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialType = searchParams.get('type') || 'ALL';
  const initialSubject = searchParams.get('subjectId') ? parseInt(searchParams.get('subjectId')!, 10) : undefined;

  const [exams, setExams] = useState<Exam[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [attempts, setAttempts] = useState<StudentExamAttempt[]>([]);
  const [filterType, setFilterType] = useState<string>(initialType);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(initialSubject);
  const [loading, setLoading] = useState<boolean>(true);

  // Modal for starting an exam
  const [examToStart, setExamToStart] = useState<Exam | null>(null);

  const navigate = useNavigate();

  const loadData = async () => {
    setLoading(true);
    try {
      const [subs, allExams, history] = await Promise.all([
        subjectsApi.getAll(),
        examsApi.getAll({
          subjectId: selectedSubjectId,
          examType: filterType === 'ALL' ? undefined : filterType,
        }),
        attemptsApi.getHistory().catch(() => []),
      ]);
      setSubjects(subs);
      setExams(allExams);
      setAttempts(history);
    } catch (err) {
      console.error('Failed to load exams', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [filterType, selectedSubjectId]);

  const handleStartExamConfirmed = () => {
    if (examToStart) {
      const targetId = examToStart.id;
      setExamToStart(null);
      navigate(`/take-exam/${targetId}`);
    }
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '2rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      {/* Header */}
      <div style={{ marginBottom: '2rem' }}>
        <span className="badge badge-primary" style={{ marginBottom: '0.4rem' }}>Assessment Center</span>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#ffffff' }}>
          Available Examinations & Practice Quizzes
        </h1>
        <p style={{ color: '#9ca3af', fontSize: '0.92rem' }}>
          Choose between formal timed assessments with strict passing standards or self-paced practice quizzes.
        </p>
      </div>

      {/* Filter Controls Bar */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '12px',
        padding: '1rem 1.25rem',
        border: '1px solid #d1fae5',
        marginBottom: '2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem',
      }}>
        {/* Type Filter Buttons */}
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {['ALL', 'FORMAL', 'PRACTICE'].map((type) => (
            <button
              key={type}
              type="button"
              onClick={() => {
                setFilterType(type);
                if (type === 'ALL') {
                  searchParams.delete('type');
                } else {
                  searchParams.set('type', type);
                }
                setSearchParams(searchParams);
              }}
              style={{
                padding: '0.45rem 0.9rem',
                borderRadius: '8px',
                fontSize: '0.85rem',
                fontWeight: 600,
                cursor: 'pointer',
                backgroundColor: filterType === type ? '#059669' : '#f9fafb',
                color: filterType === type ? '#ffffff' : '#374151',
                border: filterType === type ? '1px solid #059669' : '1px solid #d1d5db',
                transition: 'all 0.15s ease',
              }}
            >
              {type === 'ALL' ? 'All Assessments' : type === 'FORMAL' ? 'Formal Exams' : 'Practice Quizzes'}
            </button>
          ))}
        </div>

        {/* Subject Dropdown Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Filter size={16} color="#64748b" />
          <select
            value={selectedSubjectId ?? ''}
            onChange={(e) => {
              const val = e.target.value ? parseInt(e.target.value, 10) : undefined;
              setSelectedSubjectId(val);
              if (val) {
                searchParams.set('subjectId', val.toString());
              } else {
                searchParams.delete('subjectId');
              }
              setSearchParams(searchParams);
            }}
            style={{ fontSize: '0.85rem', padding: '0.45rem 0.75rem', minWidth: '180px' }}
          >
            <option value="">All Subjects</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.code})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Exams Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
          <p>Loading available examinations...</p>
        </div>
      ) : exams.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <FileQuestion size={48} color="#94a3b8" style={{ margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>No Examinations Found</h3>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '0.5rem' }}>
            There are currently no active exams matching your filter selection.
          </p>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '1.5rem',
        }}>
          {exams.map((exam) => {
            const isFormal = exam.examType === 'FORMAL';
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
              <div
                key={exam.id}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  borderTop: isFormal ? '4px solid #064e3b' : '4px solid #059669',
                  backgroundColor: '#ffffff',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <span className={`badge ${isFormal ? 'badge-primary' : 'badge-success'}`}>
                        {isFormal ? 'Formal Exam' : 'Practice Quiz'}
                      </span>
                      {isCompleted ? (
                        <span className={`badge ${isPassed ? 'badge-success' : 'badge-danger'}`}>
                          {isPassed ? 'Completed (Passed)' : 'Completed (Did Not Pass)'}
                        </span>
                      ) : isInProgress ? (
                        <span className="badge badge-warning">
                          In Progress
                        </span>
                      ) : null}
                    </div>
                    <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>
                      {exam.subjectName}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
                    {exam.title}
                  </h3>
                  <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.5, marginBottom: '1.25rem' }}>
                    {exam.description || 'Standard timed assessment covering course objectives.'}
                  </p>

                  {/* Exam Parameters Specs */}
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(3, 1fr)',
                    gap: '0.75rem',
                    backgroundColor: '#f9fafb',
                    padding: '0.75rem',
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                    marginBottom: '1.5rem',
                    textAlign: 'center',
                    fontSize: '0.8rem',
                  }}>
                    <div>
                      <div style={{ color: '#64748b', fontSize: '0.72rem', textTransform: 'uppercase', fontWeight: 600 }}>
                        Duration
                      </div>
                      <div style={{ fontWeight: 700, color: '#1f2937', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.25rem', marginTop: '0.15rem' }}>
                        <Clock size={13} color="#059669" />
                        <span>{exam.durationMinutes}m</span>
                      </div>
                    </div>

                    <div>
                      <div style={{ color: '#64748b', fontSize: '0.72rem', textTransform: 'uppercase', fontWeight: 600 }}>
                        Total Marks
                      </div>
                      <div style={{ fontWeight: 700, color: '#1f2937', marginTop: '0.15rem' }}>
                        {exam.totalMarks} pts
                      </div>
                    </div>

                    <div>
                      <div style={{ color: '#64748b', fontSize: '0.72rem', textTransform: 'uppercase', fontWeight: 600 }}>
                        Pass Mark
                      </div>
                      <div style={{ fontWeight: 700, color: '#059669', marginTop: '0.15rem' }}>
                        {exam.passingMarks} pts
                      </div>
                    </div>
                  </div>
                </div>

                {/* Card CTA Footer */}
                {isCompleted && attemptId ? (
                  <Link
                    to={`/scorecard/${attemptId}`}
                    className="btn btn-secondary"
                    style={{ width: '100%', fontSize: '0.92rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem' }}
                  >
                    <Award size={16} color="#059669" />
                    <span>View Scorecard</span>
                  </Link>
                ) : isInProgress ? (
                  <Link
                    to={`/take-exam/${exam.id}`}
                    className="btn btn-warning"
                    style={{ width: '100%', fontSize: '0.92rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem' }}
                  >
                    <RotateCcw size={16} />
                    <span>Resume Assessment</span>
                  </Link>
                ) : (
                  <button
                    onClick={() => setExamToStart(exam)}
                    className="btn btn-primary"
                    style={{ width: '100%', fontSize: '0.92rem', backgroundColor: '#059669' }}
                  >
                    <Play size={16} />
                    <span>{isFormal ? 'Start Formal Examination' : 'Start Practice Quiz'}</span>
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal before launching exam */}
      <ConfirmModal
        isOpen={!!examToStart}
        title={examToStart?.examType === 'FORMAL' ? 'Start Formal Examination?' : 'Start Practice Quiz?'}
        message={
          examToStart
            ? `You are about to start "${examToStart.title}" (${examToStart.durationMinutes} minutes). Once started, your countdown timer begins immediately and cannot be paused. Your answers will be automatically submitted when time expires.`
            : ''
        }
        confirmLabel="I am Ready — Begin Exam"
        cancelLabel="Not Yet"
        variant={examToStart?.examType === 'FORMAL' ? 'primary' : 'warning'}
        onConfirm={handleStartExamConfirmed}
        onCancel={() => setExamToStart(null)}
      />
    </div>
  );
};
