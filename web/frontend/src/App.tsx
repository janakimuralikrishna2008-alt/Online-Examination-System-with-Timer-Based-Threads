import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { ProtectedRoute } from './components/ProtectedRoute';

// Pages
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { StudentDashboard } from './pages/student/StudentDashboard';
import { SubjectCatalogPage } from './pages/student/SubjectCatalogPage';
import { TopicStudyPage } from './pages/student/TopicStudyPage';
import { ExamCatalogPage } from './pages/student/ExamCatalogPage';
import { ExamTakingPage } from './pages/student/ExamTakingPage';
import { ScorecardPage } from './pages/student/ScorecardPage';
import { StudentHistoryPage } from './pages/student/StudentHistoryPage';
import { AdminDashboard } from './pages/admin/AdminDashboard';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="app-container">
          <Navbar />
          <main className="main-content">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<LandingPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage mode="choose" />} />
              <Route path="/register/student" element={<RegisterPage mode="student" />} />
              <Route path="/register/admin" element={<RegisterPage mode="admin" />} />

              {/* Student Protected Routes */}
              <Route
                path="/student"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <StudentDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/subjects"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <SubjectCatalogPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/study/topic/:topicId"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <TopicStudyPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/exams"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <ExamCatalogPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/take-exam/:examId"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <ExamTakingPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/scorecard/:attemptId"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <ScorecardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/history"
                element={
                  <ProtectedRoute requiredRole="STUDENT">
                    <StudentHistoryPage />
                  </ProtectedRoute>
                }
              />

              {/* Admin Protected Routes */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute requiredRole="ADMIN">
                    <AdminDashboard />
                  </ProtectedRoute>
                }
              />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
