import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  GraduationCap,
  Clock,
  Shield,
  Award,
  Users,
  ShieldCheck,
  CheckCircle,
  ArrowRight,
  Timer,
  FileCheck,
  BarChart3,
  BookOpen,
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  const { isAuthenticated, isAdmin } = useAuth();

  return (
    <div style={{ background: '#111827', minHeight: 'calc(100vh - 68px)' }}>

      {/* ── HERO SECTION ─────────────────────────────────────── */}
      <section style={{
        background: 'linear-gradient(160deg, #064e3b 0%, #065f46 50%, #047857 100%)',
        color: '#ffffff',
        padding: '6rem 1.5rem 8rem',
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* Radial mint glow */}
        <div style={{
          position: 'absolute', top: '-10%', left: '50%',
          transform: 'translateX(-50%)', width: '700px', height: '500px',
          background: 'radial-gradient(ellipse, rgba(110,231,183,0.2) 0%, transparent 70%)',
          pointerEvents: 'none',
        }} />
        {/* Bottom fade into #111827 */}
        <div style={{
          position: 'absolute', bottom: 0, left: 0, right: 0, height: '120px',
          background: 'linear-gradient(to bottom, transparent, #111827)',
          pointerEvents: 'none',
        }} />

        <div style={{ maxWidth: '860px', margin: '0 auto', position: 'relative', zIndex: 1 }}>

          {/* Portal badge */}
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: '0.65rem',
            background: 'rgba(110,231,183,0.15)', border: '1px solid rgba(110,231,183,0.35)',
            padding: '0.5rem 1.25rem', borderRadius: '9999px',
            fontSize: '0.82rem', fontWeight: 700, letterSpacing: '0.04em',
            marginBottom: '2rem', color: '#6ee7b7', textTransform: 'uppercase' as const,
          }}>
            <GraduationCap size={16} />
            <span>University Examination Portal</span>
          </div>

          <h1 style={{
            fontSize: 'clamp(2rem, 5vw, 3.6rem)', fontWeight: 800,
            letterSpacing: '-0.03em', lineHeight: 1.15, marginBottom: '1.4rem',
            color: '#ffffff',
          }}>
            Online Examination System
            <br />
            <span style={{
              background: 'linear-gradient(90deg, #6ee7b7, #f59e0b)',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            }}>
              with Timer-Based Threads
            </span>
          </h1>

          <p style={{
            fontSize: 'clamp(1rem, 2vw, 1.18rem)', color: '#a7f3d0',
            lineHeight: 1.7, maxWidth: '680px', margin: '0 auto 2.75rem',
          }}>
            A precision-engineered, multi-threaded examination platform with
            server-authoritative countdown timers, secure JWT authentication,
            and instant verifiable scorecards.
          </p>

          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            gap: '1rem', flexWrap: 'wrap' as const,
          }}>
            {isAuthenticated ? (
              <Link
                to={isAdmin ? '/admin' : '/student'}
                className="btn btn-primary"
                style={{
                  padding: '0.9rem 2rem', fontSize: '1rem',
                  backgroundColor: '#059669', boxShadow: '0 4px 18px rgba(5,150,105,0.5)',
                }}
              >
                <span>Go to {isAdmin ? 'Admin Console' : 'Student Portal'}</span>
                <ArrowRight size={18} />
              </Link>
            ) : (
              <>
                <Link
                  to="/login"
                  className="btn btn-primary"
                  style={{
                    padding: '0.9rem 2rem', fontSize: '1rem',
                    backgroundColor: '#059669', boxShadow: '0 4px 18px rgba(5,150,105,0.5)',
                  }}
                >
                  <GraduationCap size={20} />
                  <span>Get Started</span>
                </Link>
                <Link
                  to="/login"
                  style={{
                    display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
                    padding: '0.9rem 2rem', fontSize: '1rem',
                    background: 'rgba(255,255,255,0.1)',
                    color: '#ecfdf5', border: '1px solid rgba(255,255,255,0.25)',
                    borderRadius: '6px', fontWeight: 600, textDecoration: 'none',
                    transition: 'all 0.2s',
                  }}
                >
                  <GraduationCap size={20} color="#6ee7b7" />
                  <span>Student Login</span>
                </Link>
              </>
            )}
          </div>

          {/* Trust row */}
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            gap: '1.5rem', marginTop: '2.5rem', flexWrap: 'wrap' as const,
          }}>
            {([
              { icon: <Shield size={14} />, label: 'JWT Secured' },
              { icon: <Timer size={14} />, label: 'Thread-Based Timers' },
              { icon: <FileCheck size={14} />, label: 'Auto Submission' },
              { icon: <BarChart3 size={14} />, label: 'Instant Scorecards' },
            ] as { icon: React.ReactNode; label: string }[]).map(({ icon, label }) => (
              <div key={label} style={{
                display: 'flex', alignItems: 'center', gap: '0.4rem',
                fontSize: '0.8rem', color: '#6ee7b7', fontWeight: 500,
              }}>
                <span style={{ color: '#34d399' }}>{icon}</span>
                {label}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── FEATURES SECTION ────────────────────────────────── */}
      <section style={{ maxWidth: '1200px', margin: '-3rem auto 5rem', padding: '0 1.5rem', position: 'relative', zIndex: 10 }}>
        <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginBottom: '0.5rem' }}>
            Platform Features
          </h2>
          <p style={{ color: '#9ca3af', fontSize: '0.95rem' }}>
            Everything you need for a fair, secure, and efficient examination experience.
          </p>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '1.5rem',
        }}>

          {/* Secure Examination */}
          <div className="card" style={{ padding: '2rem 1.5rem', borderTop: '4px solid #064e3b', background: '#ffffff' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #064e3b, #059669)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.1rem',
            }}>
              <Shield size={22} color="#fff" />
            </div>
            <h3 style={{ fontSize: '1.08rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
              Secure Examination
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.6 }}>
              Every session is authenticated with JWT tokens. Role-based access ensures students only see their exams and admins retain full control.
            </p>
          </div>

          {/* Timer-Based Exams */}
          <div className="card" style={{ padding: '2rem 1.5rem', borderTop: '4px solid #059669', background: '#ffffff' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #047857, #10b981)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.1rem',
            }}>
              <Clock size={22} color="#fff" />
            </div>
            <h3 style={{ fontSize: '1.08rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
              Timer-Based Examination
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.6 }}>
              Server-authoritative countdown timers powered by background Java threads. Tamper-proof deadlines with visual threshold warnings.
            </p>
          </div>

          {/* Auto Submission */}
          <div className="card" style={{ padding: '2rem 1.5rem', borderTop: '4px solid #10b981', background: '#ffffff' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #059669, #34d399)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.1rem',
            }}>
              <FileCheck size={22} color="#fff" />
            </div>
            <h3 style={{ fontSize: '1.08rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
              Automatic Submission
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.6 }}>
              When the timer expires, the system auto-submits answered questions — preventing data loss and ensuring fair enforcement.
            </p>
          </div>

          {/* Results & Scorecards */}
          <div className="card" style={{ padding: '2rem 1.5rem', borderTop: '4px solid #f59e0b', background: '#ffffff' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #d97706, #f59e0b)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.1rem',
            }}>
              <Award size={22} color="#fff" />
            </div>
            <h3 style={{ fontSize: '1.08rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
              Results &amp; Scorecards
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.6 }}>
              Instant grading on submission. Detailed per-question review, correct answer comparison, and a celebratory pass/fail scorecard.
            </p>
          </div>

          {/* Study Library */}
          <div className="card" style={{ padding: '2rem 1.5rem', borderTop: '4px solid #047857', background: '#ffffff' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #064e3b, #059669)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.1rem',
            }}>
              <BookOpen size={22} color="#fff" />
            </div>
            <h3 style={{ fontSize: '1.08rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
              Study Library
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.6 }}>
              Curated subject catalogs with code snippets, revision notes, and self-paced progress tracking for thorough exam preparation.
            </p>
          </div>

          {/* Admin Management */}
          <div className="card" style={{ padding: '2rem 1.5rem', borderTop: '4px solid #f59e0b', background: '#ffffff' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #b45309, #f59e0b)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.1rem',
            }}>
              <Users size={22} color="#fff" />
            </div>
            <h3 style={{ fontSize: '1.08rem', fontWeight: 700, color: '#1f2937', marginBottom: '0.5rem' }}>
              Admin Management
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.6 }}>
              Administrators can create and manage exams, questions, subjects, and review student analytics from a dedicated console.
            </p>
          </div>
        </div>
      </section>

      {/* ── CTA BANNER ──────────────────────────────────────── */}
      <section style={{ maxWidth: '1000px', margin: '0 auto 6rem', padding: '0 1.5rem' }}>
        <div style={{
          background: 'linear-gradient(135deg, #064e3b 0%, #065f46 100%)',
          borderRadius: '20px',
          padding: '3rem 2.5rem',
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '2.5rem',
          alignItems: 'center',
          boxShadow: '0 20px 40px -10px rgba(0,0,0,0.5)',
          border: '1px solid rgba(110,231,183,0.2)',
        }}>
          <div>
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.4rem',
              background: 'rgba(110,231,183,0.15)', border: '1px solid rgba(110,231,183,0.3)',
              padding: '0.3rem 0.85rem', borderRadius: '9999px',
              fontSize: '0.75rem', fontWeight: 700, color: '#6ee7b7',
              letterSpacing: '0.05em', textTransform: 'uppercase' as const, marginBottom: '1rem',
            }}>
              <ShieldCheck size={12} />
              Dual Portal Architecture
            </div>
            <h2 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#ffffff', marginBottom: '0.85rem', lineHeight: 1.2 }}>
              Ready to begin your examination?
            </h2>
            <p style={{ color: '#a7f3d0', fontSize: '0.93rem', lineHeight: 1.7, marginBottom: '1.5rem' }}>
              Sign in with your credentials to access examinations, study materials, and scorecards.
              Admins get a full management dashboard.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column' as const, gap: '0.5rem' }}>
              {[
                'Secure JWT-authenticated sessions',
                'Role-based student & admin portals',
                'Auto-graded timed examinations',
                'Detailed results & analytics',
              ].map((item) => (
                <div key={item} style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', fontSize: '0.87rem', color: '#d1fae5' }}>
                  <CheckCircle size={15} color="#34d399" />
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column' as const, gap: '1rem' }}>
            <Link
              to="/login"
              className="btn btn-primary"
              style={{
                padding: '0.95rem', fontSize: '1rem', justifyContent: 'center',
                backgroundColor: '#059669', boxShadow: '0 4px 14px rgba(5,150,105,0.55)',
              }}
            >
              <GraduationCap size={18} />
              Sign In to Student Portal
            </Link>
            <Link
              to="/login?role=admin"
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                padding: '0.95rem', fontSize: '1rem',
                background: 'rgba(255,255,255,0.08)', color: '#ecfdf5',
                border: '1px solid rgba(255,255,255,0.18)', borderRadius: '6px',
                fontWeight: 600, textDecoration: 'none', transition: 'all 0.2s',
              }}
            >
              <ShieldCheck size={18} color="#6ee7b7" />
              Admin Console Login
            </Link>
            <Link
              to="/register"
              style={{
                textAlign: 'center' as const, fontSize: '0.88rem', color: '#6ee7b7',
                textDecoration: 'none', paddingTop: '0.25rem',
              }}
            >
              New student?{' '}
              <span style={{ color: '#f59e0b', fontWeight: 600 }}>Create an account →</span>
            </Link>
          </div>
        </div>
      </section>

      {/* ── FOOTER ──────────────────────────────────────────── */}
      <footer style={{
        background: '#064e3b',
        borderTop: '1px solid #065f46',
        padding: '2.5rem 1.5rem',
        color: '#6ee7b7',
        textAlign: 'center' as const,
      }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            gap: '0.65rem', marginBottom: '1rem',
          }}>
            <div style={{
              background: 'linear-gradient(135deg, #059669, #10b981)',
              padding: '0.45rem', borderRadius: '8px',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <GraduationCap size={20} color="#fff" />
            </div>
            <div style={{ textAlign: 'left' as const }}>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ecfdf5', lineHeight: 1.2 }}>
                Online Examination System
              </div>
              <div style={{ fontSize: '0.7rem', color: '#6ee7b7', textTransform: 'uppercase' as const, letterSpacing: '0.04em' }}>
                Timer-Based Multi-Threaded Platform
              </div>
            </div>
          </div>
          <p style={{ fontSize: '0.82rem', color: '#34d399', marginBottom: '0.5rem' }}>
            Secure · Reliable · Timer-Enforced
          </p>
          <p style={{ fontSize: '0.78rem', color: '#a7f3d0' }}>
            © {new Date().getFullYear()} Online Examination System with Timer-Based Threads. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
};
