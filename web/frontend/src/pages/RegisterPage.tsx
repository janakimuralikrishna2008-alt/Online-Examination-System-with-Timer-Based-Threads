import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  GraduationCap,
  ShieldCheck,
  User,
  Mail,
  Lock,
  KeyRound,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  Eye,
  EyeOff,
  Sparkles
} from 'lucide-react';

interface RegisterPageProps {
  mode?: 'choose' | 'student' | 'admin';
}

export const RegisterPage: React.FC<RegisterPageProps> = ({ mode }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { registerStudent, registerAdmin, isAuthenticated, isAdmin } = useAuth();

  // Determine active tab/view based on mode prop or URL pathname
  const computeActiveMode = (): 'choose' | 'student' | 'admin' => {
    if (mode) return mode;
    if (location.pathname.endsWith('/student')) return 'student';
    if (location.pathname.endsWith('/admin')) return 'admin';
    return 'choose';
  };

  const activeMode = computeActiveMode();

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated) {
      navigate(isAdmin ? '/admin' : '/student', { replace: true });
    }
  }, [isAuthenticated, isAdmin, navigate]);

  // Form states
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [invitationCode, setInvitationCode] = useState('');

  // UI states
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Clear errors when switching tabs
  const handleSwitchTab = (newMode: 'student' | 'admin') => {
    setError(null);
    setSuccess(null);
    navigate(`/register/${newMode}`);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    // Validation
    if (!fullName.trim() || !username.trim() || !email.trim() || !password) {
      setError('Please fill in all required fields.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match. Please re-enter both passwords.');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    if (activeMode === 'admin' && !invitationCode.trim()) {
      setError('Admin invitation code is required to register as an administrator.');
      return;
    }

    setSubmitting(true);

    try {
      if (activeMode === 'admin') {
        await registerAdmin({
          fullName: fullName.trim(),
          username: username.trim(),
          email: email.trim(),
          password,
          invitationCode: invitationCode.trim(),
        });
        setSuccess('Administrator account created successfully! Redirecting to Admin Console...');
        setTimeout(() => {
          navigate('/admin');
        }, 900);
      } else {
        await registerStudent({
          fullName: fullName.trim(),
          username: username.trim(),
          email: email.trim(),
          password,
        });
        setSuccess('Student account created successfully! Redirecting to Student Dashboard...');
        setTimeout(() => {
          navigate('/student');
        }, 900);
      }
    } catch (err: any) {
      if (err.response) {
        const status = err.response.status;
        const msg = err.response.data?.message || err.response.data?.error;
        if (status === 403) {
          setError(msg || 'Invalid invitation code. Administrative registration requires an authorized institution code.');
        } else if (status === 400) {
          setError(msg || 'Validation failed. Please verify that your username and email are unique.');
        } else if (status === 409) {
          setError(msg || 'Username or email address is already registered. Please sign in instead.');
        } else if (status >= 500) {
          setError('Backend server error during registration. Please verify server logs.');
        } else {
          setError(msg || `Registration failed (HTTP ${status}).`);
        }
      } else if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        setError('Connection timed out. Please check that the backend server is running on port 8080.');
      } else {
        setError('Cannot connect to backend server. Please ensure the backend service is active and accessible.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{
      minHeight: 'calc(100vh - 68px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2.5rem 1rem',
      backgroundColor: '#111827',
      background: 'linear-gradient(160deg, #111827 0%, #064e3b 50%, #111827 100%)',
      position: 'relative',
    }}>
      {/* Background glow overlay */}
      <div style={{
        position: 'absolute',
        top: '-15%',
        left: '50%',
        transform: 'translateX(-50%)',
        width: '900px',
        height: '650px',
        background: 'radial-gradient(ellipse, rgba(110,231,183,0.12) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      <div style={{
        maxWidth: activeMode === 'choose' ? '880px' : '520px',
        width: '100%',
        position: 'relative',
        zIndex: 1,
      }}>
        {/* ======================= CHOOSER VIEW (/register) ======================= */}
        {activeMode === 'choose' ? (
          <div>
            {/* Header */}
            <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.4rem',
                backgroundColor: 'rgba(110, 231, 183, 0.15)',
                border: '1px solid #059669',
                borderRadius: '9999px',
                padding: '0.35rem 1rem',
                color: '#6ee7b7',
                fontSize: '0.82rem',
                fontWeight: 600,
                marginBottom: '1rem',
              }}>
                <Sparkles size={14} />
                <span>Online Examination System with Timer-Based Threads</span>
              </div>
              <h1 style={{
                fontSize: '2.2rem',
                fontWeight: 800,
                color: '#ffffff',
                marginBottom: '0.6rem',
                letterSpacing: '-0.02em',
              }}>
                Choose Your <span style={{ color: '#6ee7b7' }}>Registration Portal</span>
              </h1>
              <p style={{ fontSize: '0.96rem', color: '#cbd5e1', maxWidth: '580px', margin: '0 auto' }}>
                Select the appropriate registration path to access examinee testing rooms or administrative controls.
              </p>
            </div>

            {/* Portal Option Cards */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '1.75rem',
            }}>
              {/* Option 1: Student Registration Card */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '20px',
                padding: '2.25rem 2rem',
                border: '2px solid #059669',
                boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'transform 0.2s, box-shadow 0.2s',
              }}>
                <div>
                  <div style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: '16px',
                    backgroundColor: '#ecfdf5',
                    color: '#059669',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '1.25rem',
                    boxShadow: '0 4px 12px rgba(5,150,105,0.2)',
                  }}>
                    <GraduationCap size={32} />
                  </div>

                  <span style={{
                    fontSize: '0.72rem',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    letterSpacing: '0.08em',
                    color: '#059669',
                    backgroundColor: '#ecfdf5',
                    padding: '0.2rem 0.6rem',
                    borderRadius: '6px',
                  }}>
                    Student Portal
                  </span>

                  <h3 style={{ fontSize: '1.45rem', fontWeight: 800, color: '#1f2937', marginTop: '0.6rem', marginBottom: '0.5rem' }}>
                    Register as Student
                  </h3>

                  <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.55, marginBottom: '1.5rem' }}>
                    Enroll to access categorized study modules, interactive learning materials, practice quizzes, and thread-timed formal examinations.
                  </p>

                  <ul style={{
                    listStyle: 'none',
                    padding: 0,
                    margin: '0 0 1.75rem 0',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.6rem',
                    fontSize: '0.84rem',
                    color: '#475569',
                  }}>
                    <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CheckCircle2 size={16} color="#059669" />
                      <span>Self-paced curriculum topic study</span>
                    </li>
                    <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CheckCircle2 size={16} color="#059669" />
                      <span>Live multi-threaded countdown exams</span>
                    </li>
                    <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CheckCircle2 size={16} color="#059669" />
                      <span>Instant scorecards and performance breakdown</span>
                    </li>
                  </ul>
                </div>

                <div>
                  <button
                    onClick={() => navigate('/register/student')}
                    style={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.5rem',
                      padding: '0.9rem',
                      fontSize: '0.95rem',
                      fontWeight: 700,
                      backgroundColor: '#059669',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      boxShadow: '0 4px 14px rgba(5,150,105,0.4)',
                      transition: 'background 0.2s',
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#047857'; }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#059669'; }}
                  >
                    <span>Register as Student</span>
                    <ArrowRight size={16} />
                  </button>

                  <div style={{ marginTop: '1rem', textAlign: 'center', fontSize: '0.82rem', color: '#64748b' }}>
                    Already have a student account?{' '}
                    <Link to="/login?role=student" style={{ fontWeight: 700, color: '#059669', textDecoration: 'none' }}>
                      Student Login
                    </Link>
                  </div>
                </div>
              </div>

              {/* Option 2: Admin Registration Card */}
              <div style={{
                backgroundColor: '#ffffff',
                borderRadius: '20px',
                padding: '2.25rem 2rem',
                border: '2px solid #064e3b',
                boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'transform 0.2s, box-shadow 0.2s',
              }}>
                <div>
                  <div style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: '16px',
                    backgroundColor: '#064e3b',
                    color: '#6ee7b7',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '1.25rem',
                    boxShadow: '0 4px 12px rgba(6,78,59,0.3)',
                  }}>
                    <ShieldCheck size={32} />
                  </div>

                  <span style={{
                    fontSize: '0.72rem',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    letterSpacing: '0.08em',
                    color: '#f59e0b',
                    backgroundColor: '#fef3c7',
                    padding: '0.2rem 0.6rem',
                    borderRadius: '6px',
                  }}>
                    Admin Console • Code Required
                  </span>

                  <h3 style={{ fontSize: '1.45rem', fontWeight: 800, color: '#1f2937', marginTop: '0.6rem', marginBottom: '0.5rem' }}>
                    Register as Administrator
                  </h3>

                  <p style={{ fontSize: '0.88rem', color: '#64748b', lineHeight: 1.55, marginBottom: '1.5rem' }}>
                    Authorized faculty and proctor onboarding. Create curriculum syllabi, manage question pools, draft exams with AI, and oversee examinee performance.
                  </p>

                  <ul style={{
                    listStyle: 'none',
                    padding: 0,
                    margin: '0 0 1.75rem 0',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.6rem',
                    fontSize: '0.84rem',
                    color: '#475569',
                  }}>
                    <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CheckCircle2 size={16} color="#059669" />
                      <span>Exam creation & timer configuration</span>
                    </li>
                    <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CheckCircle2 size={16} color="#059669" />
                      <span>Curriculum & question bank governance</span>
                    </li>
                    <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CheckCircle2 size={16} color="#059669" />
                      <span>Institution invitation code validation</span>
                    </li>
                  </ul>
                </div>

                <div>
                  <button
                    onClick={() => navigate('/register/admin')}
                    style={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.5rem',
                      padding: '0.9rem',
                      fontSize: '0.95rem',
                      fontWeight: 700,
                      backgroundColor: '#064e3b',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      boxShadow: '0 4px 14px rgba(6,78,59,0.4)',
                      transition: 'background 0.2s',
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#047857'; }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#064e3b'; }}
                  >
                    <span>Register as Administrator</span>
                    <ArrowRight size={16} />
                  </button>

                  <div style={{ marginTop: '1rem', textAlign: 'center', fontSize: '0.82rem', color: '#64748b' }}>
                    Already an administrator?{' '}
                    <Link to="/login?role=admin" style={{ fontWeight: 700, color: '#059669', textDecoration: 'none' }}>
                      Admin Login
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : (
          /* ======================= REGISTRATION FORM VIEW ======================= */
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '20px',
            border: activeMode === 'admin' ? '2px solid #064e3b' : '2px solid #059669',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6)',
            padding: '2.25rem',
          }}>
            {/* Top Navigation Tabs */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              backgroundColor: '#f1f5f9',
              borderRadius: '12px',
              padding: '4px',
              marginBottom: '1.75rem',
            }}>
              <button
                type="button"
                onClick={() => handleSwitchTab('student')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                  padding: '0.65rem 0.5rem',
                  borderRadius: '9px',
                  border: 'none',
                  fontSize: '0.86rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  backgroundColor: activeMode === 'student' ? '#059669' : 'transparent',
                  color: activeMode === 'student' ? '#ffffff' : '#64748b',
                  boxShadow: activeMode === 'student' ? '0 2px 8px rgba(5,150,105,0.3)' : 'none',
                  transition: 'all 0.2s',
                }}
              >
                <GraduationCap size={18} />
                <span>Student Portal</span>
              </button>

              <button
                type="button"
                onClick={() => handleSwitchTab('admin')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                  padding: '0.65rem 0.5rem',
                  borderRadius: '9px',
                  border: 'none',
                  fontSize: '0.86rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  backgroundColor: activeMode === 'admin' ? '#064e3b' : 'transparent',
                  color: activeMode === 'admin' ? '#ffffff' : '#64748b',
                  boxShadow: activeMode === 'admin' ? '0 2px 8px rgba(6,78,59,0.3)' : 'none',
                  transition: 'all 0.2s',
                }}
              >
                <ShieldCheck size={18} />
                <span>Admin Console</span>
              </button>
            </div>

            {/* Header info */}
            <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
              <div style={{
                width: '50px',
                height: '50px',
                borderRadius: '14px',
                backgroundColor: activeMode === 'admin' ? '#064e3b' : '#ecfdf5',
                color: activeMode === 'admin' ? '#6ee7b7' : '#059669',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 0.85rem auto',
                boxShadow: activeMode === 'admin' ? '0 4px 12px rgba(6,78,59,0.25)' : '0 4px 12px rgba(5,150,105,0.2)',
              }}>
                {activeMode === 'admin' ? <ShieldCheck size={28} /> : <GraduationCap size={28} />}
              </div>

              <h2 style={{ fontSize: '1.45rem', fontWeight: 800, color: '#1f2937', marginBottom: '0.35rem' }}>
                {activeMode === 'admin' ? 'Administrator Registration' : 'Student Registration'}
              </h2>
              <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                {activeMode === 'admin'
                  ? 'Institutional faculty onboarding. Server-side invitation code required.'
                  : 'Create an account to study course materials and take online examinations.'}
              </p>
            </div>

            {/* Admin Security Banner */}
            {activeMode === 'admin' && (
              <div style={{
                backgroundColor: '#fffbeb',
                border: '1px solid #fde68a',
                borderRadius: '10px',
                padding: '0.75rem 1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.6rem',
                marginBottom: '1.25rem',
                fontSize: '0.82rem',
                color: '#92400e',
              }}>
                <KeyRound size={18} color="#f59e0b" style={{ flexShrink: 0 }} />
                <span>
                  Admin privileges are protected by an authorized server invitation code. Role escalation is verified server-side.
                </span>
              </div>
            )}

            {/* Error Notification */}
            {error && (
              <div style={{
                backgroundColor: '#fef2f2',
                color: '#b91c1c',
                border: '1px solid #fecaca',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                marginBottom: '1.25rem',
              }}>
                <AlertCircle size={18} style={{ flexShrink: 0 }} />
                <span>{error}</span>
              </div>
            )}

            {/* Success Notification */}
            {success && (
              <div style={{
                backgroundColor: '#ecfdf5',
                color: '#065f46',
                border: '1px solid #a7f3d0',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                marginBottom: '1.25rem',
              }}>
                <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
                <span>{success}</span>
              </div>
            )}

            {/* Registration Form */}
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.95rem' }}>
              {/* Full Name */}
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#374151', marginBottom: '0.35rem' }}>
                  Full Name
                </label>
                <div style={{ position: 'relative' }}>
                  <User size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g. Eleanor Vance"
                    required
                    style={{ width: '100%', paddingLeft: '38px', color: '#1f2937' }}
                  />
                </div>
              </div>

              {/* Username */}
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#374151', marginBottom: '0.35rem' }}>
                  Username
                </label>
                <div style={{ position: 'relative' }}>
                  <User size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder={activeMode === 'admin' ? 'e.g. admin_proctor' : 'e.g. student_alex'}
                    required
                    minLength={3}
                    maxLength={50}
                    style={{ width: '100%', paddingLeft: '38px', color: '#1f2937' }}
                  />
                </div>
              </div>

              {/* Email Address */}
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#374151', marginBottom: '0.35rem' }}>
                  Email Address
                </label>
                <div style={{ position: 'relative' }}>
                  <Mail size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder={activeMode === 'admin' ? 'faculty@institution.edu' : 'student@example.com'}
                    required
                    style={{ width: '100%', paddingLeft: '38px', color: '#1f2937' }}
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#374151', marginBottom: '0.35rem' }}>
                  Password
                </label>
                <div style={{ position: 'relative' }}>
                  <Lock size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Minimum 6 characters"
                    required
                    minLength={6}
                    style={{ width: '100%', paddingLeft: '38px', paddingRight: '40px', color: '#1f2937' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      position: 'absolute',
                      right: '10px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: '#94a3b8',
                      cursor: 'pointer',
                      padding: '4px',
                    }}
                    title={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              {/* Confirm Password */}
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#374151', marginBottom: '0.35rem' }}>
                  Confirm Password
                </label>
                <div style={{ position: 'relative' }}>
                  <Lock size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Re-enter password"
                    required
                    minLength={6}
                    style={{ width: '100%', paddingLeft: '38px', paddingRight: '40px', color: '#1f2937' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    style={{
                      position: 'absolute',
                      right: '10px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: '#94a3b8',
                      cursor: 'pointer',
                      padding: '4px',
                    }}
                    title={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                  >
                    {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              {/* Admin Invitation Code (Admin only) */}
              {activeMode === 'admin' && (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                    <label style={{ fontSize: '0.82rem', fontWeight: 600, color: '#374151' }}>
                      Admin Invitation Code
                    </label>
                    <span style={{ fontSize: '0.74rem', color: '#059669', fontWeight: 600 }}>Required</span>
                  </div>
                  <div style={{ position: 'relative' }}>
                    <KeyRound size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                    <input
                      type="text"
                      value={invitationCode}
                      onChange={(e) => setInvitationCode(e.target.value)}
                      placeholder="Enter institution invitation code"
                      required
                      style={{ width: '100%', paddingLeft: '38px', color: '#1f2937', letterSpacing: '0.02em' }}
                    />
                  </div>
                </div>
              )}

              {/* Submit Button */}
              <button
                type="submit"
                disabled={submitting}
                style={{
                  marginTop: '0.5rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                  padding: '0.85rem',
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  backgroundColor: submitting ? '#9ca3af' : (activeMode === 'admin' ? '#064e3b' : '#059669'),
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '9px',
                  cursor: submitting ? 'not-allowed' : 'pointer',
                  boxShadow: submitting ? 'none' : (activeMode === 'admin' ? '0 4px 14px rgba(6,78,59,0.4)' : '0 4px 14px rgba(5,150,105,0.4)'),
                  transition: 'background 0.2s',
                }}
                onMouseEnter={(e) => {
                  if (!submitting) {
                    (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#047857';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!submitting) {
                    (e.currentTarget as HTMLButtonElement).style.backgroundColor = activeMode === 'admin' ? '#064e3b' : '#059669';
                  }
                }}
              >
                <span>
                  {submitting
                    ? (activeMode === 'admin' ? 'Verifying & Registering...' : 'Creating Student Account...')
                    : (activeMode === 'admin' ? 'Create Admin Account' : 'Create Student Account')}
                </span>
                {!submitting && <ArrowRight size={16} />}
              </button>
            </form>

            {/* Footer Links */}
            <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.84rem', color: '#64748b' }}>
              {activeMode === 'admin' ? (
                <>
                  Already an administrator?{' '}
                  <Link to="/login?role=admin" style={{ fontWeight: 700, color: '#059669', textDecoration: 'none' }}>
                    Sign in to Admin Console
                  </Link>
                </>
              ) : (
                <>
                  Already registered as a student?{' '}
                  <Link to="/login?role=student" style={{ fontWeight: 700, color: '#059669', textDecoration: 'none' }}>
                    Sign in to Student Portal
                  </Link>
                </>
              )}
            </div>

            <div style={{ marginTop: '0.6rem', textAlign: 'center', fontSize: '0.78rem' }}>
              <Link to="/register" style={{ color: '#94a3b8', textDecoration: 'none' }}>
                ← View all registration options
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
