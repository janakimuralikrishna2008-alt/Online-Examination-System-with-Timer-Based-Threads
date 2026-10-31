import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  BookOpen,
  GraduationCap,
  ShieldCheck,
  LogOut,
  History,
  CheckSquare,
  LayoutDashboard,
  Menu,
  X,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  const navLinkStyle = (path: string): React.CSSProperties => ({
    color: isActive(path) ? '#6ee7b7' : '#a7f3d0',
    fontWeight: isActive(path) ? 700 : 500,
    fontSize: '0.88rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.4rem',
    textDecoration: 'none',
    padding: '0.4rem 0.65rem',
    borderRadius: '6px',
    background: isActive(path) ? 'rgba(110,231,183,0.15)' : 'transparent',
    border: isActive(path) ? '1px solid rgba(110,231,183,0.3)' : '1px solid transparent',
    transition: 'all 0.18s',
  });

  return (
    <header style={{
      background: '#064e3b',
      color: '#ffffff',
      borderBottom: '1px solid #065f46',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      boxShadow: '0 2px 12px rgba(6,78,59,0.35)',
    }}>
      <div style={{
        maxWidth: '1350px',
        margin: '0 auto',
        padding: '0 1.5rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: '68px',
      }}>

        {/* ── Brand ── */}
        <Link
          to="/"
          style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none', color: '#fff', flexShrink: 0 }}
        >
          <div style={{
            background: 'linear-gradient(135deg, #059669, #10b981)',
            padding: '0.5rem',
            borderRadius: '9px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 12px rgba(5,150,105,0.4)',
          }}>
            <GraduationCap size={22} color="#fff" />
          </div>
          <div>
            <div style={{ fontSize: '0.98rem', fontWeight: 700, letterSpacing: '-0.02em', lineHeight: 1.2, color: '#ecfdf5' }}>
              Online Examination System
            </div>
            <div style={{ fontSize: '0.65rem', color: '#6ee7b7', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
              Timer-Based Threads
            </div>
          </div>
        </Link>

        {/* ── Center Nav (authenticated) ── */}
        {isAuthenticated && (
          <nav style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }} className="navbar-center-nav">
            {isAdmin ? (
              <Link to="/admin" style={navLinkStyle('/admin')}>
                <ShieldCheck size={16} color={isActive('/admin') ? '#6ee7b7' : '#a7f3d0'} />
                Admin Console
              </Link>
            ) : (
              <>
                <Link to="/student" style={navLinkStyle('/student')}>
                  <LayoutDashboard size={16} />
                  Dashboard
                </Link>
                <Link to="/subjects" style={navLinkStyle('/subjects')}>
                  <BookOpen size={16} />
                  Subjects
                </Link>
                <Link to="/exams" style={navLinkStyle('/exams')}>
                  <CheckSquare size={16} />
                  Exams
                </Link>
                <Link to="/history" style={navLinkStyle('/history')}>
                  <History size={16} />
                  Scorecards
                </Link>
              </>
            )}
          </nav>
        )}

        {/* ── Right ── */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {isAuthenticated && user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              {/* User chip */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: '0.55rem',
                background: 'rgba(110,231,183,0.1)',
                padding: '0.35rem 0.85rem 0.35rem 0.45rem',
                borderRadius: '9999px',
                border: '1px solid rgba(110,231,183,0.2)',
              }}>
                <div style={{
                  width: '30px', height: '30px', borderRadius: '50%',
                  background: isAdmin
                    ? 'linear-gradient(135deg, #047857, #059669)'
                    : 'linear-gradient(135deg, #059669, #10b981)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '0.78rem', fontWeight: 700, flexShrink: 0,
                  boxShadow: '0 2px 6px rgba(0,0,0,0.2)',
                }}>
                  {user.fullName ? user.fullName[0].toUpperCase() : 'U'}
                </div>
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#ecfdf5', lineHeight: 1.15 }}>
                    {user.fullName || user.username}
                  </div>
                  <div style={{ fontSize: '0.65rem', color: '#6ee7b7', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    {user.role}
                  </div>
                </div>
              </div>

              {/* Logout */}
              <button
                onClick={handleLogout}
                style={{
                  background: 'transparent',
                  color: '#f87171',
                  border: '1px solid rgba(248,113,113,0.3)',
                  padding: '0.4rem 0.8rem',
                  borderRadius: '7px',
                  fontSize: '0.82rem',
                  display: 'flex', alignItems: 'center', gap: '0.35rem',
                  transition: 'all 0.2s', cursor: 'pointer',
                }}
                title="Logout"
              >
                <LogOut size={14} />
                <span>Logout</span>
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <Link to="/login" style={{
                color: '#a7f3d0', fontSize: '0.88rem', fontWeight: 600,
                padding: '0.45rem 0.9rem', borderRadius: '7px',
                border: '1px solid rgba(167,243,208,0.3)',
                textDecoration: 'none', transition: 'all 0.2s',
              }}>
                Sign In
              </Link>
              <Link to="/register" style={{
                background: '#059669', color: '#fff',
                fontSize: '0.88rem', fontWeight: 600,
                padding: '0.45rem 0.9rem', borderRadius: '7px',
                textDecoration: 'none', transition: 'all 0.2s',
                boxShadow: '0 2px 8px rgba(5,150,105,0.4)',
              }}>
                Register
              </Link>
            </div>
          )}

          {/* Mobile hamburger */}
          <button
            className="navbar-hamburger"
            onClick={() => setMobileOpen(!mobileOpen)}
            style={{
              background: 'transparent', border: 'none',
              color: '#6ee7b7', cursor: 'pointer', padding: '0.35rem',
              display: 'none',
            }}
          >
            {mobileOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      {/* ── Mobile menu ── */}
      {mobileOpen && isAuthenticated && (
        <div style={{
          background: '#064e3b', borderTop: '1px solid #065f46',
          padding: '0.75rem 1.5rem 1rem',
        }} className="navbar-mobile-menu">
          <nav style={{ display: 'flex', flexDirection: 'column' as const, gap: '0.25rem' }}>
            {isAdmin ? (
              <Link to="/admin" onClick={() => setMobileOpen(false)} style={{ ...navLinkStyle('/admin'), padding: '0.65rem 0.75rem' }}>
                <ShieldCheck size={16} /> Admin Console
              </Link>
            ) : (
              <>
                <Link to="/student" onClick={() => setMobileOpen(false)} style={{ ...navLinkStyle('/student'), padding: '0.65rem 0.75rem' }}>
                  <LayoutDashboard size={16} /> Dashboard
                </Link>
                <Link to="/subjects" onClick={() => setMobileOpen(false)} style={{ ...navLinkStyle('/subjects'), padding: '0.65rem 0.75rem' }}>
                  <BookOpen size={16} /> Subjects
                </Link>
                <Link to="/exams" onClick={() => setMobileOpen(false)} style={{ ...navLinkStyle('/exams'), padding: '0.65rem 0.75rem' }}>
                  <CheckSquare size={16} /> Exams
                </Link>
                <Link to="/history" onClick={() => setMobileOpen(false)} style={{ ...navLinkStyle('/history'), padding: '0.65rem 0.75rem' }}>
                  <History size={16} /> Scorecards
                </Link>
              </>
            )}
          </nav>
        </div>
      )}
    </header>
  );
};
