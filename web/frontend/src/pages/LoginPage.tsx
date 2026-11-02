import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  GraduationCap,
  ShieldCheck,
  Lock,
  User,
  Eye,
  EyeOff,
  AlertCircle,
  ArrowRight,
} from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const initialRole = searchParams.get('role') === 'admin' ? 'ADMIN' : 'STUDENT';

  const [activeTab, setActiveTab] = useState<'STUDENT' | 'ADMIN'>(initialRole);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const { login, isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate(isAdmin ? '/admin' : '/student', { replace: true });
    }
  }, [isAuthenticated, isAdmin, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Please enter both username and password.');
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      const res = await login(username, password);
      if (res.user.role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/student');
      }
    } catch (err: any) {
      if (err.response) {
        const status = err.response.status;
        const msg = err.response.data?.message || err.response.data?.error;
        if (msg) {
          setError(msg);
        } else if (status === 400) {
          setError('Invalid login request. Please check your username and password.');
        } else if (status === 401) {
          setError('Invalid username/email or password.');
        } else if (status === 403) {
          setError('Access denied. You do not have permission to access this portal.');
        } else if (status === 404) {
          setError('Authentication service endpoint not found (HTTP 404).');
        } else if (status >= 500) {
          setError('Backend server encountered an internal error (HTTP 500).');
        } else {
          setError(`Authentication error (HTTP ${status}).`);
        }
      } else if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        setError('Connection to backend timed out. Please verify the Spring Boot service is responsive.');
      } else {
        setError('Failed to connect to backend server. Please verify the backend service is running and accessible.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const isAdminTab = activeTab === 'ADMIN';
  const accentColor = '#059669';
  const accentHover = '#047857';

  return (
    <div style={{
      minHeight: 'calc(100vh - 68px)',
      display: 'flex',
      background: 'linear-gradient(160deg, #111827 0%, #064e3b 50%, #111827 100%)',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Background mint glow */}
      <div style={{
        position: 'absolute', top: '-10%', left: '50%',
        transform: 'translateX(-50%)', width: '800px', height: '600px',
        background: 'radial-gradient(ellipse, rgba(110,231,183,0.12) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      {/* Left panel — decorative, hidden on small screens */}
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        padding: '3rem 2rem', position: 'relative', zIndex: 1,
        minWidth: 0,
      }} className="login-left-panel">
        <div style={{ maxWidth: '380px', width: '100%' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '2.5rem',
          }}>
            <div style={{
              background: 'linear-gradient(135deg, #059669, #10b981)',
              padding: '0.6rem', borderRadius: '10px',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 4px 12px rgba(5,150,105,0.4)',
            }}>
              <GraduationCap size={26} color="#fff" />
            </div>
            <div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#ecfdf5', lineHeight: 1.2 }}>
                Online Examination System
              </div>
              <div style={{ fontSize: '0.68rem', color: '#6ee7b7', textTransform: 'uppercase' as const, letterSpacing: '0.05em' }}>
                Timer-Based Threads Platform
              </div>
            </div>
          </div>

          <h2 style={{ fontSize: '2rem', fontWeight: 800, color: '#ffffff', lineHeight: 1.2, marginBottom: '1rem' }}>
            Welcome back to your
            <span style={{
              display: 'block',
              background: 'linear-gradient(90deg, #6ee7b7, #f59e0b)',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            }}>
              Examination Portal
            </span>
          </h2>

          <p style={{ color: '#a7f3d0', fontSize: '0.92rem', lineHeight: 1.7, marginBottom: '2rem' }}>
            Secure, timer-enforced examinations with instant results and detailed analytics.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column' as const, gap: '0.85rem' }}>
            {[
              { icon: <Lock size={15} />, text: 'JWT-authenticated secure sessions' },
              { icon: <ShieldCheck size={15} />, text: 'Role-based student & admin access' },
              { icon: <GraduationCap size={15} />, text: 'Auto-graded timed examinations' },
            ].map(({ icon, text }) => (
              <div key={text} style={{
                display: 'flex', alignItems: 'center', gap: '0.65rem',
                fontSize: '0.87rem', color: '#d1fae5',
              }}>
                <span style={{
                  color: '#6ee7b7', flexShrink: 0,
                  width: '28px', height: '28px', borderRadius: '7px',
                  background: 'rgba(5,150,105,0.2)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>{icon}</span>
                {text}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right panel — login form card */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '2rem 1.5rem', position: 'relative', zIndex: 1,
        flex: '0 0 auto', width: 'min(100%, 480px)',
      }}>
        <div style={{
          width: '100%',
          backgroundColor: '#ffffff',
          borderRadius: '20px',
          border: '1px solid #d1fae5',
          boxShadow: '0 25px 50px -12px rgba(0,0,0,0.5)',
          overflow: 'hidden',
        }}>
          {/* Portal Tab Switcher */}
          <div style={{ display: 'flex', borderBottom: '1px solid #e5e7eb', background: '#f9fafb' }}>
            <button
              type="button"
              onClick={() => { setActiveTab('STUDENT'); setError(null); setShowPassword(false); }}
              style={{
                flex: 1, padding: '1.1rem',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                fontWeight: 600, fontSize: '0.9rem',
                background: activeTab === 'STUDENT' ? '#ffffff' : 'transparent',
                color: activeTab === 'STUDENT' ? '#059669' : '#64748b',
                borderBottom: activeTab === 'STUDENT' ? '3px solid #059669' : '3px solid transparent',
                cursor: 'pointer', borderRadius: 0, transition: 'all 0.2s',
              }}
            >
              <GraduationCap size={18} />
              <span>Student Portal</span>
            </button>
            <button
              type="button"
              onClick={() => { setActiveTab('ADMIN'); setError(null); setShowPassword(false); }}
              style={{
                flex: 1, padding: '1.1rem',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                fontWeight: 600, fontSize: '0.9rem',
                background: activeTab === 'ADMIN' ? '#ffffff' : 'transparent',
                color: activeTab === 'ADMIN' ? '#047857' : '#64748b',
                borderBottom: activeTab === 'ADMIN' ? '3px solid #f59e0b' : '3px solid transparent',
                cursor: 'pointer', borderRadius: 0, transition: 'all 0.2s',
              }}
            >
              <ShieldCheck size={18} color={activeTab === 'ADMIN' ? '#f59e0b' : '#64748b'} />
              <span>Admin Console</span>
            </button>
          </div>

          {/* Form Body */}
          <div style={{ padding: '2.25rem' }}>
            {/* Heading */}
            <div style={{ marginBottom: '1.75rem', textAlign: 'center' as const }}>
              <div style={{
                width: '52px', height: '52px', borderRadius: '14px', margin: '0 auto 1rem',
                background: isAdminTab
                  ? 'linear-gradient(135deg, #064e3b, #047857)'
                  : 'linear-gradient(135deg, #059669, #10b981)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                boxShadow: '0 8px 20px rgba(5,150,105,0.35)',
              }}>
                {isAdminTab
                  ? <ShieldCheck size={26} color="#6ee7b7" />
                  : <GraduationCap size={26} color="#fff" />}
              </div>
              <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#1f2937', marginBottom: '0.3rem' }}>
                {isAdminTab ? 'Administrator Sign In' : 'Student Sign In'}
              </h2>
              <p style={{ fontSize: '0.83rem', color: '#64748b' }}>
                {isAdminTab
                  ? 'Manage exams, questions, subjects, and student analytics'
                  : 'Access your examinations, study notes, and scorecards'}
              </p>
            </div>

            {/* Error */}
            {error && (
              <div style={{
                background: '#fef2f2', color: '#b91c1c',
                border: '1px solid #fecaca', padding: '0.75rem 1rem',
                borderRadius: '8px', fontSize: '0.84rem',
                display: 'flex', alignItems: 'center', gap: '0.5rem',
                marginBottom: '1.25rem',
              }}>
                <AlertCircle size={17} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column' as const, gap: '1.1rem' }}>
              {/* Username */}
              <div>
                <label style={{
                  display: 'block', fontSize: '0.8rem', fontWeight: 600,
                  color: '#374151', marginBottom: '0.4rem', letterSpacing: '0.01em',
                }}>
                  Username or Email
                </label>
                <div style={{ position: 'relative' }}>
                  <User
                    size={16} color="#94a3b8"
                    style={{ position: 'absolute', left: '13px', top: '50%', transform: 'translateY(-50%)' }}
                  />
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder={isAdminTab ? 'Enter admin username' : 'Enter username or email'}
                    required
                    autoComplete="username"
                    style={{ width: '100%', paddingLeft: '40px', paddingRight: '12px', color: '#1f2937' }}
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <label style={{
                  display: 'block', fontSize: '0.8rem', fontWeight: 600,
                  color: '#374151', marginBottom: '0.4rem', letterSpacing: '0.01em',
                }}>
                  Password
                </label>
                <div style={{ position: 'relative' }}>
                  <Lock
                    size={16} color="#94a3b8"
                    style={{ position: 'absolute', left: '13px', top: '50%', transform: 'translateY(-50%)' }}
                  />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    required
                    autoComplete="current-password"
                    style={{ width: '100%', paddingLeft: '40px', paddingRight: '44px', color: '#1f2937' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)',
                      background: 'none', border: 'none', padding: '2px',
                      cursor: 'pointer', color: '#94a3b8', display: 'flex', alignItems: 'center',
                    }}
                    title={showPassword ? 'Hide password' : 'Show password'}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              {/* Remember Me */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="checkbox"
                  id="rememberMe"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  style={{
                    width: '16px', height: '16px', cursor: 'pointer',
                    accentColor: accentColor,
                  }}
                />
                <label
                  htmlFor="rememberMe"
                  style={{ fontSize: '0.83rem', color: '#4b5563', cursor: 'pointer', userSelect: 'none' as const }}
                >
                  Remember me on this device
                </label>
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={submitting}
                style={{
                  marginTop: '0.4rem',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                  padding: '0.82rem', fontSize: '0.95rem', fontWeight: 700,
                  background: submitting ? '#9ca3af' : accentColor,
                  color: '#fff', border: 'none', borderRadius: '8px',
                  cursor: submitting ? 'not-allowed' : 'pointer',
                  transition: 'background 0.2s, transform 0.1s',
                  boxShadow: submitting ? 'none' : '0 4px 14px rgba(5,150,105,0.4)',
                }}
                onMouseEnter={(e) => { if (!submitting) (e.currentTarget as HTMLButtonElement).style.background = accentHover; }}
                onMouseLeave={(e) => { if (!submitting) (e.currentTarget as HTMLButtonElement).style.background = accentColor; }}
              >
                <span>{submitting ? 'Signing in…' : 'Sign In'}</span>
                {!submitting && <ArrowRight size={16} />}
              </button>
            </form>

            {/* Register links */}
            <div style={{ marginTop: '1.5rem', textAlign: 'center' as const, fontSize: '0.84rem', color: '#64748b' }}>
              {activeTab === 'STUDENT' ? (
                <>
                  Don't have an account yet?{' '}
                  <Link to="/register/student" style={{ fontWeight: 700, color: '#059669' }}>
                    Register as Student
                  </Link>
                </>
              ) : (
                <>
                  Need administrator access?{' '}
                  <Link to="/register/admin" style={{ fontWeight: 700, color: '#059669' }}>
                    Register with Invitation Code
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
