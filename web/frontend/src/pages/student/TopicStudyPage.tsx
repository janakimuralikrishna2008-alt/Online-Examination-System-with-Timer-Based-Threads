import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { materialsApi } from '../../services/api';
import type { StudyMaterial } from '../../types';
import { AiStudyDrawer } from '../../components/AiStudyDrawer';
import {
  BookOpen,
  CheckCircle2,
  Sparkles,
  ArrowLeft,
  Award,
  Copy,
  Check
} from 'lucide-react';

export const TopicStudyPage: React.FC = () => {
  const { topicId } = useParams<{ topicId: string }>();
  const [materials, setMaterials] = useState<StudyMaterial[]>([]);
  const [loading, setLoading] = useState(true);
  const [aiDrawerOpen, setAiDrawerOpen] = useState(false);
  const [copiedId, setCopiedId] = useState<number | null>(null);

  const parsedTopicId = topicId ? parseInt(topicId, 10) : 0;

  const loadMaterials = async () => {
    if (!parsedTopicId) return;
    try {
      const data = await materialsApi.getByTopic(parsedTopicId);
      setMaterials(data);
    } catch (err) {
      console.error('Failed to load topic materials', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMaterials();
  }, [parsedTopicId]);

  const handleToggleCompleted = async (materialId: number, currentCompleted?: boolean) => {
    try {
      const nextState = !currentCompleted;
      await materialsApi.markProgress(materialId, nextState);
      setMaterials((prev) =>
        prev.map((m) => (m.id === materialId ? { ...m, completed: nextState } : m))
      );
    } catch (err) {
      console.error('Failed to update progress', err);
    }
  };

  const handleCopyCode = (id: number, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const topicName = materials[0]?.topicName || 'Topic Study Notes';
  const completedCount = materials.filter((m) => m.completed).length;
  const progressPct = materials.length > 0 ? Math.round((completedCount / materials.length) * 100) : 0;

  return (
    <div style={{ maxWidth: '1080px', margin: '0 auto', padding: '2rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      {/* Top Navigation Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <Link
          to="/subjects"
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
          <span>Back to Subjects</span>
        </Link>

        <button
          onClick={() => setAiDrawerOpen(true)}
          className="btn"
          style={{
            background: '#047857',
            color: '#ffffff',
            boxShadow: '0 4px 12px rgba(4, 120, 87, 0.4)',
          }}
        >
          <Sparkles size={16} />
          <span>AI Tutor: Ask Questions</span>
        </button>
      </div>

      {/* Topic Hero Card */}
      <div className="card" style={{
        marginBottom: '2rem',
        padding: '2rem',
        backgroundColor: '#ffffff',
        border: '1px solid #d1fae5',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <span className="badge badge-primary" style={{ marginBottom: '0.5rem' }}>
              Study Module
            </span>
            <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#1f2937', marginBottom: '0.35rem' }}>
              {topicName}
            </h1>
            <p style={{ color: '#64748b', fontSize: '0.92rem' }}>
              Read through concepts, examine code snippets, and mark each section complete as you master the material.
            </p>
          </div>

          <div style={{
            backgroundColor: '#f9fafb',
            padding: '1rem 1.25rem',
            borderRadius: '12px',
            border: '1px solid #e5e7eb',
            textAlign: 'center',
            minWidth: '160px',
          }}>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: progressPct === 100 ? '#10b981' : '#059669' }}>
              {progressPct}%
            </div>
            <div style={{ fontSize: '0.78rem', color: '#64748b', fontWeight: 600 }}>
              {completedCount} of {materials.length} Sections Done
            </div>
          </div>
        </div>
      </div>

      {/* Materials List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
          <p>Loading study materials...</p>
        </div>
      ) : materials.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <BookOpen size={48} color="#94a3b8" style={{ margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>No Study Materials Added</h3>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '0.5rem' }}>
            There are currently no notes or snippets for this topic. Use the AI Assistant to generate explanations!
          </p>
          <button
            onClick={() => setAiDrawerOpen(true)}
            className="btn btn-primary"
            style={{ marginTop: '1.25rem' }}
          >
            <Sparkles size={16} />
            <span>Generate Study Summary with AI</span>
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {materials.map((mat, index) => {
            const isCode = mat.contentType === 'CODE_SNIPPET';
            const isKeyPoints = mat.contentType === 'KEY_POINTS';

            return (
              <div
                key={mat.id}
                className="card"
                style={{
                  borderLeft: mat.completed ? '4px solid #10b981' : '4px solid #cbd5e1',
                  transition: 'border-color 0.2s',
                  padding: '1.75rem',
                }}
              >
                {/* Section Header */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: '1rem',
                  flexWrap: 'wrap',
                  gap: '0.5rem',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{
                      width: '28px',
                      height: '28px',
                      borderRadius: '50%',
                      backgroundColor: mat.completed ? '#ecfdf5' : '#f1f5f9',
                      color: mat.completed ? '#059669' : '#64748b',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.82rem',
                    }}>
                      {index + 1}
                    </span>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
                      {mat.title}
                    </h3>
                    <span className="badge badge-primary" style={{ fontSize: '0.72rem' }}>
                      {mat.contentType.replace('_', ' ')}
                    </span>
                  </div>

                  {/* Mark as read button */}
                  <button
                    onClick={() => handleToggleCompleted(mat.id, mat.completed)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      padding: '0.4rem 0.8rem',
                      borderRadius: '8px',
                      backgroundColor: mat.completed ? '#ecfdf5' : '#f8fafc',
                      color: mat.completed ? '#059669' : '#64748b',
                      border: `1px solid ${mat.completed ? '#a7f3d0' : '#cbd5e1'}`,
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      transition: 'all 0.15s'
                    }}
                  >
                    <CheckCircle2 size={16} color={mat.completed ? '#10b981' : '#94a3b8'} />
                    <span>{mat.completed ? 'Completed' : 'Mark as Studied'}</span>
                  </button>
                </div>

                {/* Section Content */}
                {isCode ? (
                  <div style={{ position: 'relative' }}>
                    <button
                      onClick={() => handleCopyCode(mat.id, mat.content)}
                      style={{
                        position: 'absolute',
                        top: '12px',
                        right: '12px',
                        backgroundColor: 'rgba(255,255,255,0.15)',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '0.35rem 0.65rem',
                        fontSize: '0.75rem',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                        cursor: 'pointer',
                        zIndex: 10,
                      }}
                    >
                      {copiedId === mat.id ? <Check size={14} color="#10b981" /> : <Copy size={14} />}
                      <span>{copiedId === mat.id ? 'Copied!' : 'Copy Code'}</span>
                    </button>
                    <pre style={{
                      backgroundColor: '#0f172a',
                      color: '#f8fafc',
                      padding: '1.25rem',
                      borderRadius: '10px',
                      fontSize: '0.88rem',
                      fontFamily: 'Consolas, Monaco, "Courier New", monospace',
                      overflowX: 'auto',
                      lineHeight: 1.6,
                    }}>
                      <code>{mat.content}</code>
                    </pre>
                  </div>
                ) : isKeyPoints ? (
                  <div style={{
                    backgroundColor: '#f8fafc',
                    padding: '1.25rem',
                    borderRadius: '10px',
                    border: '1px solid #e2e8f0',
                    lineHeight: 1.7,
                    fontSize: '0.92rem',
                    color: '#334155',
                    whiteSpace: 'pre-wrap',
                  }}>
                    {mat.content}
                  </div>
                ) : (
                  <div style={{
                    fontSize: '0.95rem',
                    lineHeight: 1.7,
                    color: '#334155',
                    whiteSpace: 'pre-wrap',
                  }}>
                    {mat.content}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Bottom CTA for Practice Quiz */}
      <div style={{
        marginTop: '3rem',
        padding: '2rem',
        borderRadius: '16px',
        backgroundColor: '#f0fdf4',
        border: '1px solid #bbf7d0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1.5rem',
      }}>
        <div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#166534', marginBottom: '0.35rem' }}>
            Ready to test your comprehension?
          </h3>
          <p style={{ color: '#15803d', fontSize: '0.9rem' }}>
            Reinforce what you just studied by taking a practice quiz or formal examination.
          </p>
        </div>

        <Link to="/exams?type=PRACTICE" className="btn btn-success" style={{ padding: '0.75rem 1.5rem' }}>
          <Award size={18} />
          <span>Launch Practice Quiz</span>
        </Link>
      </div>

      {/* Embedded AI Study Assistant Drawer */}
      <AiStudyDrawer
        isOpen={aiDrawerOpen}
        onClose={() => setAiDrawerOpen(false)}
        contextTitle={topicName}
        contextText={materials.map((m) => `${m.title}: ${m.content}`).join('\n\n')}
      />
    </div>
  );
};
