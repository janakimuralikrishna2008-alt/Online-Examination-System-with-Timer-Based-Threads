import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { subjectsApi, topicsApi } from '../../services/api';
import type { Subject, Topic } from '../../types';
import { BookOpen, Layers, Sparkles } from 'lucide-react';
import { AiStudyDrawer } from '../../components/AiStudyDrawer';

export const SubjectCatalogPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [subjectTopics, setSubjectTopics] = useState<Record<number, Topic[]>>({});
  const [loading, setLoading] = useState(true);
  const [aiDrawerOpen, setAiDrawerOpen] = useState(false);
  const [selectedSubjectForAi, setSelectedSubjectForAi] = useState<Subject | null>(null);

  useEffect(() => {
    const loadSubjectsAndTopics = async () => {
      try {
        const subs = await subjectsApi.getAll();
        setSubjects(subs);

        // Load topics for all subjects in parallel
        const topicPromises = subs.map(async (s) => {
          try {
            const tops = await topicsApi.getBySubject(s.id);
            return { subjectId: s.id, topics: tops };
          } catch {
            return { subjectId: s.id, topics: [] };
          }
        });

        const topicsResults = await Promise.all(topicPromises);
        const mapping: Record<number, Topic[]> = {};
        topicsResults.forEach((res) => {
          mapping[res.subjectId] = res.topics;
        });
        setSubjectTopics(mapping);
      } catch (err) {
        console.error('Failed to load subjects', err);
      } finally {
        setLoading(false);
      }
    };

    loadSubjectsAndTopics();
  }, []);

  const handleOpenAiForSubject = (subject: Subject) => {
    setSelectedSubjectForAi(subject);
    setAiDrawerOpen(true);
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '2rem 1.5rem', background: '#111827', minHeight: 'calc(100vh - 68px)' }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem',
        marginBottom: '2rem'
      }}>
        <div>
          <span className="badge badge-primary" style={{ marginBottom: '0.4rem' }}>Course Catalog</span>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#ffffff' }}>
            Subjects & Study Materials
          </h1>
          <p style={{ color: '#9ca3af', fontSize: '0.92rem' }}>
            Select a subject to access organized topics, code snippets, notes, and topic-specific practice quizzes.
          </p>
        </div>

        <button
          onClick={() => { setSelectedSubjectForAi(null); setAiDrawerOpen(true); }}
          className="btn"
          style={{
            background: '#047857',
            color: '#ffffff',
            boxShadow: '0 4px 12px rgba(4, 120, 87, 0.4)',
          }}
        >
          <Sparkles size={17} />
          <span>Ask AI Study Tutor</span>
        </button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#6ee7b7' }}>
          <p>Loading course catalog...</p>
        </div>
      ) : subjects.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem', backgroundColor: '#ffffff' }}>
          <BookOpen size={48} color="#94a3b8" style={{ margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#1f2937' }}>No Subjects Available</h3>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '0.5rem' }}>
            Check back later or contact your instructor.
          </p>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '1.5rem',
        }}>
          {subjects.map((sub) => {
            const topics = subjectTopics[sub.id] || [];
            const totalMaterials = sub.totalMaterialsCount ?? 0;
            const completedMaterials = sub.completedMaterialsCount ?? 0;
            const progressPct = totalMaterials > 0 ? Math.round((completedMaterials / totalMaterials) * 100) : 0;

            return (
              <div
                key={sub.id}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                    <span className="badge badge-purple" style={{ fontSize: '0.75rem' }}>
                      {sub.code}
                    </span>
                    <button
                      onClick={() => handleOpenAiForSubject(sub)}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#059669',
                        cursor: 'pointer',
                        fontSize: '0.78rem',
                        fontWeight: 600,
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.25rem',
                      }}
                      title="Ask AI questions about this subject"
                    >
                      <Sparkles size={14} />
                      <span>AI Insights</span>
                    </button>
                  </div>

                  <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
                    {sub.name}
                  </h3>
                  <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.5, marginBottom: '1.25rem' }}>
                    {sub.description || 'Comprehensive modular course covering theoretical and practical topics.'}
                  </p>

                  {/* Progress Bar */}
                  <div style={{ marginBottom: '1.25rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
                      <span>Study Progress</span>
                      <span>{progressPct}% ({completedMaterials}/{totalMaterials} read)</span>
                    </div>
                    <div style={{ height: '7px', background: '#e2e8f0', borderRadius: '9999px', overflow: 'hidden' }}>
                      <div style={{
                        height: '100%',
                        width: `${progressPct}%`,
                        background: progressPct === 100 ? '#10b981' : '#059669',
                        borderRadius: '9999px',
                        transition: 'width 0.4s ease',
                      }} />
                    </div>
                  </div>

                  {/* Topics List Snippet */}
                  <div style={{ marginBottom: '1.5rem' }}>
                    <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
                      Topics ({topics.length}):
                    </div>
                    {topics.length === 0 ? (
                      <div style={{ fontSize: '0.82rem', color: '#94a3b8', fontStyle: 'italic' }}>
                        No topics listed yet for this subject.
                      </div>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                        {topics.slice(0, 4).map((t) => (
                          <Link
                            key={t.id}
                            to={`/study/topic/${t.id}`}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              padding: '0.45rem 0.65rem',
                              background: '#f8fafc',
                              border: '1px solid #f1f5f9',
                              borderRadius: '6px',
                              fontSize: '0.82rem',
                              color: '#334155',
                              textDecoration: 'none',
                              transition: 'background 0.15s'
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                              <Layers size={14} color="#64748b" />
                              <span style={{ fontWeight: 500 }}>{t.name}</span>
                            </div>
                            <span style={{ fontSize: '0.72rem', color: '#059669', fontWeight: 600 }}>Study →</span>
                          </Link>
                        ))}
                        {topics.length > 4 && (
                          <div style={{ fontSize: '0.75rem', color: '#94a3b8', textAlign: 'center', marginTop: '0.2rem' }}>
                            + {topics.length - 4} more topics
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Card CTA Footer */}
                <div style={{ display: 'flex', gap: '0.5rem', paddingTop: '1rem', borderTop: '1px solid #f1f5f9' }}>
                  {topics.length > 0 ? (
                    <Link
                      to={`/study/topic/${topics[0].id}`}
                      className="btn btn-primary"
                      style={{ flex: 1, fontSize: '0.85rem' }}
                    >
                      <BookOpen size={16} />
                      <span>Start Studying</span>
                    </Link>
                  ) : (
                    <button disabled className="btn btn-secondary" style={{ flex: 1, fontSize: '0.85rem' }}>
                      No Topics
                    </button>
                  )}
                  <Link
                    to={`/exams?subjectId=${sub.id}`}
                    className="btn btn-secondary"
                    style={{ fontSize: '0.85rem' }}
                  >
                    <span>View Exams</span>
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Embedded AI Study Assistant Drawer */}
      <AiStudyDrawer
        isOpen={aiDrawerOpen}
        onClose={() => setAiDrawerOpen(false)}
        contextTitle={selectedSubjectForAi ? selectedSubjectForAi.name : 'General Course Material'}
        contextText={selectedSubjectForAi?.description}
      />
    </div>
  );
};
