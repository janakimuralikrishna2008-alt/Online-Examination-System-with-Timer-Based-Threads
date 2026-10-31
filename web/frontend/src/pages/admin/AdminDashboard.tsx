import React, { useEffect, useState } from 'react';
import {
  analyticsApi,
  subjectsApi,
  topicsApi,
  materialsApi,
  examsApi,
  questionsApi,
  aiApi
} from '../../services/api';
import type {
  AdminAnalytics,
  Subject,
  Topic,
  StudyMaterial,
  Exam,
  Question
} from '../../types';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
  BarChart3,
  BookOpen,
  Layers,
  FileQuestion,
  Award,
  Sparkles,
  Plus,
  Trash2,
  CheckCircle,
  Users,
  Edit2,
  X
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'analytics' | 'subjects' | 'materials' | 'questions' | 'exams'>('analytics');

  // Analytics state
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);

  // Entities state
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [topics, setTopics] = useState<Topic[]>([]);
  const [materials, setMaterials] = useState<StudyMaterial[]>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Delete modal state
  const [deleteConfirm, setDeleteConfirm] = useState<{ type: string; id: number; name: string } | null>(null);

  // Edit modal state
  const [editingExam, setEditingExam] = useState<Exam | null>(null);
  const [editingQuestion, setEditingQuestion] = useState<Question | null>(null);

  // Notifications
  const [notification, setNotification] = useState<string | null>(null);

  // Subject & Topic form state
  const [newSubject, setNewSubject] = useState({ name: '', code: '', description: '' });
  const [newTopic, setNewTopic] = useState({ subjectId: 0, name: '', description: '', orderIndex: 1 });

  // Material form state
  const [newMaterial, setNewMaterial] = useState({
    topicId: 0,
    title: '',
    contentType: 'NOTES',
    content: '',
    orderIndex: 1,
  });

  // Question form state
  const [newQuestion, setNewQuestion] = useState<{
    examId?: number;
    topicId?: number;
    questionText: string;
    optionA: string;
    optionB: string;
    optionC: string;
    optionD: string;
    correctOption: 'A' | 'B' | 'C' | 'D';
    marks: number;
    explanation: string;
  }>({
    examId: undefined,
    topicId: undefined,
    questionText: '',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    correctOption: 'A',
    marks: 1,
    explanation: '',
  });

  // AI Question Drafting state
  const [aiDraftTopic, setAiDraftTopic] = useState<string>('Java Multithreading');
  const [aiDraftDifficulty, setAiDraftDifficulty] = useState<string>('MEDIUM');
  const [aiDraftMarks, setAiDraftMarks] = useState<number>(2);
  const [draftingAi, setDraftingAi] = useState<boolean>(false);

  // Exam form state
  const [newExam, setNewExam] = useState<{
    subjectId: number;
    title: string;
    description: string;
    examType: 'FORMAL' | 'PRACTICE';
    durationMinutes: number;
    totalMarks: number;
    passingMarks: number;
  }>({
    subjectId: 0,
    title: '',
    description: '',
    examType: 'FORMAL',
    durationMinutes: 30,
    totalMarks: 20,
    passingMarks: 12,
  });

  const showNotification = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 4000);
  };

  const loadAllData = async () => {
    setLoading(true);
    try {
      const [stats, subs, exms, qs] = await Promise.all([
        analyticsApi.getAdminOverview(),
        subjectsApi.getAll(),
        examsApi.getAll(),
        questionsApi.getAll(),
      ]);
      setAnalytics(stats);
      setSubjects(subs);
      setExams(exms);
      setQuestions(qs);

      // If we have subjects, load first subject's topics
      if (subs.length > 0) {
        setNewTopic((prev) => ({ ...prev, subjectId: subs[0].id }));
        setNewExam((prev) => ({ ...prev, subjectId: subs[0].id }));
        const t = await topicsApi.getBySubject(subs[0].id);
        setTopics(t);
        if (t.length > 0) {
          setNewMaterial((prev) => ({ ...prev, topicId: t[0].id }));
          const mats = await materialsApi.getByTopic(t[0].id);
          setMaterials(mats);
        }
      }
    } catch (err) {
      console.error('Failed to load admin data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  // Subject Creation Handler
  const handleCreateSubject = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const created = await subjectsApi.create(newSubject);
      setSubjects((prev) => [...prev, created]);
      setNewSubject({ name: '', code: '', description: '' });
      showNotification(`Subject "${created.name}" created successfully!`);
    } catch (err) {
      console.error(err);
      showNotification('Failed to create subject.');
    }
  };

  // Topic Creation Handler
  const handleCreateTopic = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTopic.subjectId) return;
    try {
      const created = await topicsApi.create(newTopic);
      setTopics((prev) => [...prev, created]);
      setNewTopic({ ...newTopic, name: '', description: '' });
      showNotification(`Topic "${created.name}" created successfully!`);
    } catch (err) {
      console.error(err);
      showNotification('Failed to create topic.');
    }
  };

  // Study Material Creation Handler
  const handleCreateMaterial = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMaterial.topicId) return;
    try {
      const created = await materialsApi.create(newMaterial);
      setMaterials((prev) => [...prev, created]);
      setNewMaterial({ ...newMaterial, title: '', content: '' });
      showNotification(`Material "${created.title}" added successfully!`);
    } catch (err) {
      console.error(err);
      showNotification('Failed to create study material.');
    }
  };

  // AI Question Drafting Handler
  const handleDraftWithAi = async () => {
    if (!aiDraftTopic.trim()) return;
    setDraftingAi(true);
    try {
      const res = await aiApi.draftQuestion({
        topic: aiDraftTopic,
        difficulty: aiDraftDifficulty,
        marks: aiDraftMarks,
      });

      // Populate new question form with AI drafted output
      setNewQuestion({
        ...newQuestion,
        questionText: res.questionText,
        optionA: res.optionA,
        optionB: res.optionB,
        optionC: res.optionC,
        optionD: res.optionD,
        correctOption: res.correctOption,
        marks: res.marks || aiDraftMarks,
        explanation: res.explanation,
      });
      showNotification('Question drafted with AI! Review and click Save to Bank below.');
    } catch (err) {
      console.error('AI draft error', err);
      showNotification('Failed to draft question with AI. Fallback applied.');
    } finally {
      setDraftingAi(false);
    }
  };

  // Question Creation Handler
  const handleCreateQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newQuestion.questionText.trim()) return;

    try {
      const created = await questionsApi.create(newQuestion);
      setQuestions((prev) => [...prev, created]);
      setNewQuestion({
        examId: newQuestion.examId,
        topicId: newQuestion.topicId,
        questionText: '',
        optionA: '',
        optionB: '',
        optionC: '',
        optionD: '',
        correctOption: 'A',
        marks: 1,
        explanation: '',
      });
      showNotification('Question added to Question Bank successfully!');
    } catch (err) {
      console.error(err);
      showNotification('Failed to create question.');
    }
  };

  // Exam Creation Handler
  const handleCreateExam = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newExam.subjectId || !newExam.title.trim()) return;

    try {
      const created = await examsApi.create(newExam);
      setExams((prev) => [...prev, created]);
      setNewExam({
        ...newExam,
        title: '',
        description: '',
      });
      showNotification(`Exam "${created.title}" created successfully!`);
    } catch (err) {
      console.error(err);
      showNotification('Failed to create examination.');
    }
  };

  // Exam Update Handler
  const handleUpdateExam = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingExam) return;
    try {
      const updated = await examsApi.update(editingExam.id, {
        subjectId: editingExam.subjectId,
        title: editingExam.title,
        description: editingExam.description,
        examType: editingExam.examType,
        durationMinutes: editingExam.durationMinutes,
        totalMarks: editingExam.totalMarks,
        passingMarks: editingExam.passingMarks,
        isActive: editingExam.isActive ?? true,
      });
      setExams((prev) => prev.map((ex) => (ex.id === updated.id ? updated : ex)));
      setEditingExam(null);
      showNotification(`Exam "${updated.title}" updated successfully!`);
    } catch (err) {
      console.error(err);
      showNotification('Failed to update examination.');
    }
  };

  // Question Update Handler
  const handleUpdateQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingQuestion) return;
    try {
      const updated = await questionsApi.update(editingQuestion.id, {
        examId: editingQuestion.examId,
        topicId: editingQuestion.topicId,
        questionText: editingQuestion.questionText,
        optionA: editingQuestion.optionA,
        optionB: editingQuestion.optionB,
        optionC: editingQuestion.optionC,
        optionD: editingQuestion.optionD,
        correctOption: editingQuestion.correctOption,
        marks: editingQuestion.marks,
        explanation: editingQuestion.explanation,
      });
      setQuestions((prev) => prev.map((q) => (q.id === updated.id ? updated : q)));
      setEditingQuestion(null);
      showNotification('Question updated successfully!');
    } catch (err) {
      console.error(err);
      showNotification('Failed to update question.');
    }
  };

  // Subject dependency preview state
  const [subjectDependents, setSubjectDependents] = useState<{
    topics: Topic[];
    exams: Exam[];
    hasStudentHistory: boolean;
    loading: boolean;
    error: string | null;
  } | null>(null);

  // Load dependent info before showing subject delete modal
  const requestSubjectDelete = async (subject: Subject) => {
    setSubjectDependents({ topics: [], exams: [], hasStudentHistory: false, loading: true, error: null });
    setDeleteConfirm({ type: 'subject', id: subject.id, name: subject.name });
    try {
      const [fetchedTopics, fetchedExams] = await Promise.all([
        topicsApi.getBySubject(subject.id),
        examsApi.getAll({ subjectId: subject.id }),
      ]);
      // Heuristic: if exams exist and have attempts the backend will block with a clear message.
      // We just show the count here; the hard block happens server-side.
      setSubjectDependents({
        topics: fetchedTopics,
        exams: fetchedExams,
        hasStudentHistory: false, // only backend knows the true count
        loading: false,
        error: null,
      });
    } catch {
      setSubjectDependents(prev => prev ? { ...prev, loading: false, error: 'Could not load dependent records.' } : null);
    }
  };

  // Delete entity confirmation
  const handleDeleteConfirmed = async () => {
    if (!deleteConfirm) return;
    const { type, id, name } = deleteConfirm;
    setDeleteConfirm(null);

    try {
      if (type === 'subject') {
        // Explicitly send force=true because administrator confirmed the deletion modal
        await subjectsApi.delete(id, { force: true });
        setSubjects((prev) => prev.filter((s) => s.id !== id));
        showNotification(`Subject "${name}" deleted successfully.`);
        setSubjectDependents(null);
        // Refresh dashboard data without full page reload
        await loadAllData();
      } else if (type === 'topic') {
        await topicsApi.delete(id);
        setTopics((prev) => prev.filter((t) => t.id !== id));
        showNotification('Topic removed successfully.');
      } else if (type === 'material') {
        await materialsApi.delete(id);
        setMaterials((prev) => prev.filter((m) => m.id !== id));
        showNotification('Study material removed successfully.');
      } else if (type === 'question') {
        await questionsApi.delete(id);
        setQuestions((prev) => prev.filter((q) => q.id !== id));
        showNotification('Question removed successfully.');
      } else if (type === 'exam') {
        await examsApi.delete(id);
        setExams((prev) => prev.filter((e) => e.id !== id));
        showNotification('Examination removed successfully.');
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || `Failed to delete ${type}.`;
      showNotification(msg);
      setSubjectDependents(null);
      setDeleteConfirm(null);
    }
  };

  return (
    <div style={{ maxWidth: '1350px', margin: '0 auto', padding: '2rem 1.5rem 5rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      {/* Header Banner */}
      <div style={{
        backgroundColor: '#064e3b',
        color: '#ffffff',
        padding: '2rem',
        borderRadius: '16px',
        marginBottom: '2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem',
        border: '1px solid #065f46',
      }}>
        <div>
          <span className="badge badge-primary" style={{ marginBottom: '0.4rem', backgroundColor: 'rgba(110,231,183,0.15)', color: '#6ee7b7', border: '1px solid rgba(110,231,183,0.3)' }}>
            System Administration
          </span>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#ffffff' }}>
            Admin Console & Content Manager
          </h1>
          <p style={{ color: '#a7f3d0', fontSize: '0.92rem' }}>
            Oversee course subjects, rich learning content, AI-augmented question drafting, and timed examinations.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            onClick={loadAllData}
            className="btn btn-secondary"
            style={{ backgroundColor: 'rgba(255,255,255,0.1)', color: '#fff', border: '1px solid rgba(255,255,255,0.2)', fontSize: '0.85rem' }}
          >
            Refresh Data
          </button>
        </div>
      </div>

      {notification && (
        <div style={{
          backgroundColor: '#ecfdf5',
          color: '#047857',
          border: '1px solid #a7f3d0',
          padding: '0.85rem 1.25rem',
          borderRadius: '8px',
          marginBottom: '1.5rem',
          fontSize: '0.9rem',
          fontWeight: 600,
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
        }}>
          <CheckCircle size={18} color="#059669" />
          <span>{notification}</span>
        </div>
      )}

      {/* Main Tabs Navigation */}
      <div style={{
        display: 'flex',
        gap: '0.5rem',
        borderBottom: '1px solid #374151',
        marginBottom: '2rem',
        overflowX: 'auto',
      }}>
        {[
          { key: 'analytics', label: 'Overview & Metrics', icon: BarChart3 },
          { key: 'subjects', label: 'Subjects & Topics', icon: BookOpen },
          { key: 'materials', label: 'Study Materials', icon: Layers },
          { key: 'questions', label: 'Question Bank & AI Drafting', icon: Sparkles },
          { key: 'exams', label: 'Exams & Quizzes', icon: Award },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;

          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key as any)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.85rem 1.25rem',
                fontSize: '0.92rem',
                fontWeight: 600,
                color: isActive ? '#6ee7b7' : '#9ca3af',
                backgroundColor: isActive ? '#1f2937' : 'transparent',
                borderBottom: isActive ? '3px solid #059669' : 'none',
                borderRadius: '8px 8px 0 0',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              <Icon size={17} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: '#6ee7b7' }}>
          <p>Loading administration data & metrics...</p>
        </div>
      ) : (
        <>
          {/* TAB 1: ANALYTICS & OVERVIEW */}
          {activeTab === 'analytics' && (
            <div>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '1.25rem',
            marginBottom: '2.5rem',
          }}>
            <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem', backgroundColor: '#ffffff' }}>
              <div style={{ padding: '0.75rem', borderRadius: '10px', background: '#ecfdf5', color: '#059669' }}>
                <Users size={24} />
              </div>
              <div>
                <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#1f2937' }}>{analytics?.totalStudents ?? 0}</div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Enrolled Students</div>
              </div>
            </div>

            <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem', backgroundColor: '#ffffff' }}>
              <div style={{ padding: '0.75rem', borderRadius: '10px', background: '#ecfdf5', color: '#047857' }}>
                <Award size={24} />
              </div>
              <div>
                <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#1f2937' }}>{analytics?.totalExams ?? 0}</div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Active Exams</div>
              </div>
            </div>

            <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div style={{ padding: '0.75rem', borderRadius: '10px', background: '#fffbeb', color: '#f59e0b' }}>
                <FileQuestion size={24} />
              </div>
              <div>
                <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#0f172a' }}>{analytics?.totalQuestions ?? 0}</div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Question Bank Items</div>
              </div>
            </div>

            <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div style={{ padding: '0.75rem', borderRadius: '10px', background: '#ecfdf5', color: '#10b981' }}>
                <BarChart3 size={24} />
              </div>
              <div>
                <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#0f172a' }}>
                  {analytics?.averagePassingRate != null ? `${analytics.averagePassingRate}%` : '0%'}
                </div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Average Pass Rate</div>
              </div>
            </div>
          </div>

          {/* Recent Student Attempts Table */}
          <div className="card">
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Recent Student Examination Submissions
            </h3>
            {!analytics?.recentAttempts || analytics.recentAttempts.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '0.9rem' }}>No student attempts recorded yet.</p>
            ) : (
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Attempt ID</th>
                      <th>Student</th>
                      <th>Exam Title</th>
                      <th>Type</th>
                      <th>Score</th>
                      <th>Mode</th>
                      <th>Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.recentAttempts.map((att) => (
                      <tr key={att.id}>
                        <td>#{att.id}</td>
                        <td style={{ fontWeight: 600 }}>{att.studentName || `Student #${att.studentId}`}</td>
                        <td>{att.examTitle}</td>
                        <td>{att.examType}</td>
                        <td style={{ fontWeight: 700 }}>{att.totalScore} pts</td>
                        <td>
                          {att.status === 'AUTO_SUBMITTED' ? (
                            <span className="badge badge-warning" style={{ fontSize: '0.7rem' }}>Timer Timeout</span>
                          ) : (
                            <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>On Time</span>
                          )}
                        </td>
                        <td>
                          <span className={`badge ${att.passed ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.7rem' }}>
                            {att.passed ? 'PASSED' : 'FAILED'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: SUBJECTS & TOPICS */}
      {activeTab === 'subjects' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 380px', gap: '2rem', alignItems: 'start' }}>
          {/* Left Column: Subjects and Topics List */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div className="card">
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
                Course Subjects ({subjects.length})
              </h3>
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Code</th>
                      <th>Subject Name</th>
                      <th>Description</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {subjects.map((s) => (
                      <tr key={s.id}>
                        <td><span className="badge badge-purple">{s.code}</span></td>
                        <td style={{ fontWeight: 600 }}>{s.name}</td>
                        <td style={{ fontSize: '0.82rem', color: '#64748b' }}>{s.description}</td>
                        <td>
                          <button
                            onClick={() => requestSubjectDelete(s)}
                            style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer' }}
                            title="Delete Subject"
                          >
                            <Trash2 size={16} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Right Column: Add Forms */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* Create Subject Card */}
            <div className="card">
              <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
                Add New Subject
              </h4>
              <form onSubmit={handleCreateSubject} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Subject Name</label>
                  <input
                    type="text"
                    value={newSubject.name}
                    onChange={(e) => setNewSubject({ ...newSubject, name: e.target.value })}
                    placeholder="e.g. Distributed Systems"
                    required
                    style={{ width: '100%' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Subject Code</label>
                  <input
                    type="text"
                    value={newSubject.code}
                    onChange={(e) => setNewSubject({ ...newSubject, code: e.target.value })}
                    placeholder="e.g. CS-401"
                    required
                    style={{ width: '100%' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Description</label>
                  <textarea
                    value={newSubject.description}
                    onChange={(e) => setNewSubject({ ...newSubject, description: e.target.value })}
                    placeholder="Brief course overview"
                    rows={2}
                    style={{ width: '100%' }}
                  />
                </div>
                <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
                  <Plus size={16} />
                  <span>Create Subject</span>
                </button>
              </form>
            </div>

            {/* Create Topic Card */}
            <div className="card">
              <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
                Add Topic Under Subject
              </h4>
              <form onSubmit={handleCreateTopic} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Subject</label>
                  <select
                    value={newTopic.subjectId}
                    onChange={(e) => setNewTopic({ ...newTopic, subjectId: parseInt(e.target.value, 10) })}
                    style={{ width: '100%' }}
                  >
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Topic Name</label>
                  <input
                    type="text"
                    value={newTopic.name}
                    onChange={(e) => setNewTopic({ ...newTopic, name: e.target.value })}
                    placeholder="e.g. Raft Consensus Algorithm"
                    required
                    style={{ width: '100%' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Description</label>
                  <textarea
                    value={newTopic.description}
                    onChange={(e) => setNewTopic({ ...newTopic, description: e.target.value })}
                    placeholder="Brief summary of topic content"
                    rows={2}
                    style={{ width: '100%' }}
                  />
                </div>
                <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
                  <Plus size={16} />
                  <span>Add Topic</span>
                </button>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: STUDY MATERIALS */}
      {activeTab === 'materials' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 420px', gap: '2rem', alignItems: 'start' }}>
          {/* Materials Table */}
          <div className="card">
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Study Material Modules ({materials.length})
            </h3>
            {materials.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '0.9rem' }}>No materials found for the selected topic.</p>
            ) : (
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Type</th>
                      <th>Content Snippet</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {materials.map((m) => (
                      <tr key={m.id}>
                        <td style={{ fontWeight: 600 }}>{m.title}</td>
                        <td><span className="badge badge-primary">{m.contentType}</span></td>
                        <td style={{ fontSize: '0.8rem', color: '#64748b', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {m.content}
                        </td>
                        <td>
                          <button
                            onClick={() => setDeleteConfirm({ type: 'material', id: m.id, name: m.title })}
                            style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer' }}
                          >
                            <Trash2 size={16} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Add Study Material Form */}
          <div className="card">
            <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Add Study Material
            </h4>
            <form onSubmit={handleCreateMaterial} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Topic</label>
                <select
                  value={newMaterial.topicId}
                  onChange={(e) => {
                    const tid = parseInt(e.target.value, 10);
                    setNewMaterial({ ...newMaterial, topicId: tid });
                    materialsApi.getByTopic(tid).then(setMaterials).catch(() => {});
                  }}
                  style={{ width: '100%' }}
                >
                  {topics.map((t) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Section Title</label>
                <input
                  type="text"
                  value={newMaterial.title}
                  onChange={(e) => setNewMaterial({ ...newMaterial, title: e.target.value })}
                  placeholder="e.g. Memory Model Overview"
                  required
                  style={{ width: '100%' }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Content Type</label>
                <select
                  value={newMaterial.contentType}
                  onChange={(e) => setNewMaterial({ ...newMaterial, contentType: e.target.value })}
                  style={{ width: '100%' }}
                >
                  <option value="NOTES">NOTES (Formatted Text)</option>
                  <option value="CODE_SNIPPET">CODE_SNIPPET (Code Block)</option>
                  <option value="KEY_POINTS">KEY_POINTS (Bullet Points)</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Content Body</label>
                <textarea
                  value={newMaterial.content}
                  onChange={(e) => setNewMaterial({ ...newMaterial, content: e.target.value })}
                  placeholder="Write study notes or paste source code snippet here..."
                  rows={6}
                  required
                  style={{ width: '100%', fontFamily: newMaterial.contentType === 'CODE_SNIPPET' ? 'monospace' : 'inherit' }}
                />
              </div>

              <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
                <Plus size={16} />
                <span>Publish Material</span>
              </button>
            </form>
          </div>
        </div>
      )}

      {/* TAB 4: QUESTION BANK & AI QUESTION DRAFTING */}
      {activeTab === 'questions' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 460px', gap: '2rem', alignItems: 'start' }}>
          {/* Question Bank List */}
          <div className="card">
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Question Bank Repository ({questions.length})
            </h3>
            <div className="data-table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Question</th>
                    <th>Correct</th>
                    <th>Marks</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {questions.map((q) => (
                    <tr key={q.id}>
                      <td style={{ maxWidth: '340px' }}>
                        <div style={{ fontWeight: 600, fontSize: '0.88rem' }}>{q.questionText}</div>
                        <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>
                          A: {q.optionA} | B: {q.optionB}
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-success">({q.correctOption})</span>
                      </td>
                      <td style={{ fontWeight: 700 }}>{q.marks} pts</td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                          <button
                            onClick={() => setEditingQuestion(q)}
                            style={{ background: 'none', border: 'none', color: '#059669', cursor: 'pointer', padding: '0.2rem' }}
                            title="Edit Question"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => setDeleteConfirm({ type: 'question', id: q.id, name: q.questionText })}
                            style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', padding: '0.2rem' }}
                            title="Delete Question"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Right Column: AI Drafting & Manual Question Form */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* AI Generator Box */}
            <div className="card" style={{
              background: 'linear-gradient(135deg, #064e3b 0%, #065f46 100%)',
              border: '2px solid #059669'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <Sparkles size={20} color="#6ee7b7" />
                <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ecfdf5' }}>
                  AI Question Generator
                </h4>
              </div>
              <p style={{ fontSize: '0.82rem', color: '#a7f3d0', marginBottom: '1rem' }}>
                Generate a full multiple-choice question, 4 plausible distractors, and an explanation automatically.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#6ee7b7', textTransform: 'uppercase' }}>
                    Topic Subject
                  </label>
                  <input
                    type="text"
                    value={aiDraftTopic}
                    onChange={(e) => setAiDraftTopic(e.target.value)}
                    placeholder="e.g. Java Collections Framework"
                    style={{ width: '100%', marginTop: '0.2rem' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                  <div>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#6ee7b7', textTransform: 'uppercase' }}>
                      Difficulty
                    </label>
                    <select
                      value={aiDraftDifficulty}
                      onChange={(e) => setAiDraftDifficulty(e.target.value)}
                      style={{ width: '100%', marginTop: '0.2rem' }}
                    >
                      <option value="EASY">Easy</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HARD">Hard</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#6ee7b7', textTransform: 'uppercase' }}>
                      Marks
                    </label>
                    <input
                      type="number"
                      value={aiDraftMarks}
                      onChange={(e) => setAiDraftMarks(parseInt(e.target.value, 10))}
                      min={1}
                      max={10}
                      style={{ width: '100%', marginTop: '0.2rem' }}
                    />
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleDraftWithAi}
                  disabled={draftingAi}
                  className="btn"
                  style={{
                    backgroundColor: '#059669',
                    color: '#fff',
                    marginTop: '0.5rem',
                    boxShadow: '0 4px 12px rgba(5, 150, 105, 0.4)',
                  }}
                >
                  <Sparkles size={16} />
                  <span>{draftingAi ? 'AI Drafting in progress...' : 'Draft Question with AI'}</span>
                </button>
              </div>
            </div>

            {/* Save Question Form */}
            <div className="card">
              <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
                Review & Save to Question Bank
              </h4>
              <form onSubmit={handleCreateQuestion} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Associate with Exam (Optional)</label>
                  <select
                    value={newQuestion.examId ?? ''}
                    onChange={(e) => setNewQuestion({ ...newQuestion, examId: e.target.value ? parseInt(e.target.value, 10) : undefined })}
                    style={{ width: '100%' }}
                  >
                    <option value="">None (Pool to General Bank)</option>
                    {exams.map((ex) => (
                      <option key={ex.id} value={ex.id}>{ex.title}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Question Text</label>
                  <textarea
                    value={newQuestion.questionText}
                    onChange={(e) => setNewQuestion({ ...newQuestion, questionText: e.target.value })}
                    placeholder="Enter question statement..."
                    rows={3}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option A</label>
                  <input
                    type="text"
                    value={newQuestion.optionA}
                    onChange={(e) => setNewQuestion({ ...newQuestion, optionA: e.target.value })}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option B</label>
                  <input
                    type="text"
                    value={newQuestion.optionB}
                    onChange={(e) => setNewQuestion({ ...newQuestion, optionB: e.target.value })}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option C</label>
                  <input
                    type="text"
                    value={newQuestion.optionC}
                    onChange={(e) => setNewQuestion({ ...newQuestion, optionC: e.target.value })}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option D</label>
                  <input
                    type="text"
                    value={newQuestion.optionD}
                    onChange={(e) => setNewQuestion({ ...newQuestion, optionD: e.target.value })}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Correct Option</label>
                    <select
                      value={newQuestion.correctOption}
                      onChange={(e) => setNewQuestion({ ...newQuestion, correctOption: e.target.value as any })}
                      style={{ width: '100%' }}
                    >
                      <option value="A">Option A</option>
                      <option value="B">Option B</option>
                      <option value="C">Option C</option>
                      <option value="D">Option D</option>
                    </select>
                  </div>

                  <div>
                    <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Awarded Marks</label>
                    <input
                      type="number"
                      value={newQuestion.marks}
                      onChange={(e) => setNewQuestion({ ...newQuestion, marks: parseInt(e.target.value, 10) })}
                      min={1}
                      style={{ width: '100%' }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Explanation</label>
                  <textarea
                    value={newQuestion.explanation}
                    onChange={(e) => setNewQuestion({ ...newQuestion, explanation: e.target.value })}
                    placeholder="Provide rationale for why the answer is correct"
                    rows={2}
                    style={{ width: '100%' }}
                  />
                </div>

                <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
                  <Plus size={16} />
                  <span>Save Question to Bank</span>
                </button>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* TAB 5: EXAM MANAGEMENT */}
      {activeTab === 'exams' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 420px', gap: '2rem', alignItems: 'start' }}>
          {/* Exams Table */}
          <div className="card">
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Examinations & Practice Quizzes ({exams.length})
            </h3>
            <div className="data-table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Type</th>
                    <th>Subject</th>
                    <th>Duration</th>
                    <th>Passing</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {exams.map((ex) => (
                    <tr key={ex.id}>
                      <td style={{ fontWeight: 600 }}>{ex.title}</td>
                      <td>
                        <span className={`badge ${ex.examType === 'FORMAL' ? 'badge-primary' : 'badge-success'}`}>
                          {ex.examType}
                        </span>
                      </td>
                      <td>{ex.subjectName}</td>
                      <td>{ex.durationMinutes}m</td>
                      <td style={{ fontWeight: 700, color: '#10b981' }}>{ex.passingMarks} / {ex.totalMarks}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                          <button
                            onClick={() => setEditingExam(ex)}
                            style={{ background: 'none', border: 'none', color: '#059669', cursor: 'pointer', padding: '0.2rem' }}
                            title="Edit Exam"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => setDeleteConfirm({ type: 'exam', id: ex.id, name: ex.title })}
                            style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', padding: '0.2rem' }}
                            title="Delete Exam"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Create Exam Form */}
          <div className="card">
            <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Create New Examination
            </h4>
            <form onSubmit={handleCreateExam} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Subject</label>
                <select
                  value={newExam.subjectId}
                  onChange={(e) => setNewExam({ ...newExam, subjectId: parseInt(e.target.value, 10) })}
                  style={{ width: '100%' }}
                >
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Exam Title</label>
                <input
                  type="text"
                  value={newExam.title}
                  onChange={(e) => setNewExam({ ...newExam, title: e.target.value })}
                  placeholder="e.g. Core Java Certification Exam"
                  required
                  style={{ width: '100%' }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Assessment Type</label>
                <select
                  value={newExam.examType}
                  onChange={(e) => setNewExam({ ...newExam, examType: e.target.value as any })}
                  style={{ width: '100%' }}
                >
                  <option value="FORMAL">FORMAL (Strict Passing Standard)</option>
                  <option value="PRACTICE">PRACTICE (Self-Paced Practice Quiz)</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Duration (Minutes)</label>
                <input
                  type="number"
                  value={newExam.durationMinutes}
                  onChange={(e) => setNewExam({ ...newExam, durationMinutes: parseInt(e.target.value, 10) })}
                  min={1}
                  required
                  style={{ width: '100%' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Total Marks</label>
                  <input
                    type="number"
                    value={newExam.totalMarks}
                    onChange={(e) => setNewExam({ ...newExam, totalMarks: parseInt(e.target.value, 10) })}
                    min={1}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Passing Marks</label>
                  <input
                    type="number"
                    value={newExam.passingMarks}
                    onChange={(e) => setNewExam({ ...newExam, passingMarks: parseInt(e.target.value, 10) })}
                    min={1}
                    required
                    style={{ width: '100%' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Description</label>
                <textarea
                  value={newExam.description}
                  onChange={(e) => setNewExam({ ...newExam, description: e.target.value })}
                  placeholder="Instructions for students"
                  rows={2}
                  style={{ width: '100%' }}
                />
              </div>

              <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
                <Plus size={16} />
                <span>Launch Examination</span>
              </button>
            </form>
          </div>
        </div>
      )}
        </>
      )}

      {/* Edit Exam Modal */}
      {editingExam && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.75)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem',
        }}>
          <div className="card" style={{ maxWidth: '520px', width: '100%', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>Edit Examination #{editingExam.id}</h3>
              <button
                onClick={() => setEditingExam(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleUpdateExam} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Exam Title</label>
                <input
                  type="text"
                  value={editingExam.title}
                  onChange={(e) => setEditingExam({ ...editingExam, title: e.target.value })}
                  required
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Assessment Type</label>
                  <select
                    value={editingExam.examType}
                    onChange={(e) => setEditingExam({ ...editingExam, examType: e.target.value as any })}
                    style={{ width: '100%' }}
                  >
                    <option value="FORMAL">FORMAL</option>
                    <option value="PRACTICE">PRACTICE</option>
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Duration (Mins)</label>
                  <input
                    type="number"
                    value={editingExam.durationMinutes}
                    onChange={(e) => setEditingExam({ ...editingExam, durationMinutes: parseInt(e.target.value, 10) })}
                    min={1}
                    required
                    style={{ width: '100%' }}
                  />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Total Marks</label>
                  <input
                    type="number"
                    value={editingExam.totalMarks}
                    onChange={(e) => setEditingExam({ ...editingExam, totalMarks: parseInt(e.target.value, 10) })}
                    min={1}
                    required
                    style={{ width: '100%' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Passing Marks</label>
                  <input
                    type="number"
                    value={editingExam.passingMarks}
                    onChange={(e) => setEditingExam({ ...editingExam, passingMarks: parseInt(e.target.value, 10) })}
                    min={1}
                    required
                    style={{ width: '100%' }}
                  />
                </div>
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Description</label>
                <textarea
                  value={editingExam.description}
                  onChange={(e) => setEditingExam({ ...editingExam, description: e.target.value })}
                  rows={2}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.75rem' }}>
                <button type="button" onClick={() => setEditingExam(null)} className="btn btn-secondary">
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Question Modal */}
      {editingQuestion && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.75)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem',
        }}>
          <div className="card" style={{ maxWidth: '580px', width: '100%', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>Edit Question #{editingQuestion.id}</h3>
              <button
                onClick={() => setEditingQuestion(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleUpdateQuestion} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Question Text</label>
                <textarea
                  value={editingQuestion.questionText}
                  onChange={(e) => setEditingQuestion({ ...editingQuestion, questionText: e.target.value })}
                  rows={3}
                  required
                  style={{ width: '100%' }}
                />
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option A</label>
                <input
                  type="text"
                  value={editingQuestion.optionA}
                  onChange={(e) => setEditingQuestion({ ...editingQuestion, optionA: e.target.value })}
                  required
                  style={{ width: '100%' }}
                />
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option B</label>
                <input
                  type="text"
                  value={editingQuestion.optionB}
                  onChange={(e) => setEditingQuestion({ ...editingQuestion, optionB: e.target.value })}
                  required
                  style={{ width: '100%' }}
                />
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option C</label>
                <input
                  type="text"
                  value={editingQuestion.optionC}
                  onChange={(e) => setEditingQuestion({ ...editingQuestion, optionC: e.target.value })}
                  required
                  style={{ width: '100%' }}
                />
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Option D</label>
                <input
                  type="text"
                  value={editingQuestion.optionD}
                  onChange={(e) => setEditingQuestion({ ...editingQuestion, optionD: e.target.value })}
                  required
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Correct Option</label>
                  <select
                    value={editingQuestion.correctOption}
                    onChange={(e) => setEditingQuestion({ ...editingQuestion, correctOption: e.target.value as any })}
                    style={{ width: '100%' }}
                  >
                    <option value="A">Option A</option>
                    <option value="B">Option B</option>
                    <option value="C">Option C</option>
                    <option value="D">Option D</option>
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Awarded Marks</label>
                  <input
                    type="number"
                    value={editingQuestion.marks}
                    onChange={(e) => setEditingQuestion({ ...editingQuestion, marks: parseInt(e.target.value, 10) })}
                    min={1}
                    required
                    style={{ width: '100%' }}
                  />
                </div>
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#475569' }}>Explanation</label>
                <textarea
                  value={editingQuestion.explanation || ''}
                  onChange={(e) => setEditingQuestion({ ...editingQuestion, explanation: e.target.value })}
                  rows={2}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.75rem' }}>
                <button type="button" onClick={() => setEditingQuestion(null)} className="btn btn-secondary">
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={!!deleteConfirm}
        title={`Delete ${deleteConfirm?.type?.toUpperCase()}?`}
        message={
          deleteConfirm?.type === 'subject' && subjectDependents
            ? subjectDependents.loading
              ? 'Loading dependent records…'
              : subjectDependents.error
                ? subjectDependents.error
                : (() => {
                    const tCount = subjectDependents.topics.length;
                    const eCount = subjectDependents.exams.length;
                    if (tCount === 0 && eCount === 0) {
                      return `Are you sure you want to permanently delete "${deleteConfirm.name}"? This action cannot be undone.`;
                    }
                    const topicList = subjectDependents.topics.map(t => `• ${t.name}`).join('\n');
                    const examList = subjectDependents.exams.map(e => `• ${e.title}`).join('\n');
                    return (
                      `"${deleteConfirm.name}" has dependent content that will also be removed:\n\n` +
                      (tCount > 0 ? `Topics (${tCount}):\n${topicList}\n\n` : '') +
                      (eCount > 0 ? `Exams (${eCount}):\n${examList}\n\n` : '') +
                      `Study materials inside those topics will also be deleted.\n\n` +
                      `⚠️ If any exam has student attempt history, deletion will be blocked by the server to preserve student records.\n\n` +
                      `Click "Delete All (Force)" to proceed.`
                    );
                  })()
            : `Are you sure you want to delete "${deleteConfirm?.name}"? This action cannot be undone.`
        }
        confirmLabel={
          deleteConfirm?.type === 'subject' &&
          ((subjectDependents?.topics.length ?? 0) > 0 || (subjectDependents?.exams.length ?? 0) > 0)
            ? 'Delete All (Force)'
            : 'Yes, Delete Permanently'
        }
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDeleteConfirmed}
        onCancel={() => { setDeleteConfirm(null); setSubjectDependents(null); }}
      />
    </div>
  );
};
