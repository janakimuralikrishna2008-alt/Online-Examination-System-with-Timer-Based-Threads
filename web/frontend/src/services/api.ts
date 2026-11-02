import axios from 'axios';
import type {
  User,
  AuthResponse,
  Subject,
  Topic,
  StudyMaterial,
  Exam,
  Question,
  StudentExamAttempt,
  StartAttemptResponse,
  AttemptResultResponse,
  StudentAnalytics,
  AdminAnalytics,
  AiDraftQuestionResponse,
  StudentRegisterData,
  AdminRegisterData
} from '../types';

const rawEnvUrl = import.meta.env.VITE_API_BASE_URL;

const resolveApiBaseUrl = (): string => {
  if (rawEnvUrl && typeof rawEnvUrl === 'string' && rawEnvUrl.trim()) {
    const trimmed = rawEnvUrl.trim().replace(/\/+$/, '');
    return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
  }
  // In production builds, default to the live Render backend rather than localhost
  if (import.meta.env.PROD) {
    return 'https://online-examination-backend-t146.onrender.com/api';
  }
  return 'http://127.0.0.1:8080/api';
};

export const API_BASE_URL = resolveApiBaseUrl();

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Authorization token to every request if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercept responses for 401 handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Don't auto-redirect if we are checking login/register endpoint
      const url = error.config?.url || '';
      if (!url.includes('/auth/login') && !url.includes('/auth/register')) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

// ================= Health Endpoint =================
export const healthApi = {
  check: async (): Promise<{ status: string; service: string; database?: string }> => {
    const res = await api.get<{ status: string; service: string; database?: string }>('/health');
    return res.data;
  },
};

// ================= Auth Endpoints =================
export const authApi = {
  login: async (credentials: { usernameOrEmail: string; password: string }): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/login', {
      username: credentials.usernameOrEmail,
      usernameOrEmail: credentials.usernameOrEmail,
      password: credentials.password,
    });
    return res.data;
  },
  register: async (data: { username: string; email: string; password: string; fullName: string }): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/register', data);
    return res.data;
  },
  registerStudent: async (data: StudentRegisterData): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/register/student', data);
    return res.data;
  },
  registerAdmin: async (data: AdminRegisterData): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/register/admin', data);
    return res.data;
  },
  getMe: async (): Promise<User> => {
    const res = await api.get<User>('/auth/me');
    return res.data;
  },
};

// ================= Subjects & Topics =================
export const subjectsApi = {
  getAll: async (): Promise<Subject[]> => {
    const res = await api.get<Subject[]>('/subjects');
    return res.data;
  },
  getById: async (id: number): Promise<Subject> => {
    const res = await api.get<Subject>(`/subjects/${id}`);
    return res.data;
  },
  create: async (data: { name: string; code?: string; description: string; isPublished?: boolean }): Promise<Subject> => {
    const res = await api.post<Subject>('/subjects', {
      ...data,
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  update: async (id: number, data: { name: string; code?: string; description: string; isPublished?: boolean }): Promise<Subject> => {
    const res = await api.put<Subject>(`/subjects/${id}`, {
      ...data,
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  delete: async (id: number, options?: { force?: boolean }): Promise<void> => {
    const url = options?.force ? `/subjects/${id}?force=true` : `/subjects/${id}`;
    await api.delete(url);
  },
};

export const topicsApi = {
  getBySubject: async (subjectId: number): Promise<Topic[]> => {
    const res = await api.get<Topic[]>(`/topics/subject/${subjectId}`);
    return res.data;
  },
  create: async (data: { subjectId: number; name: string; description: string; orderIndex?: number; displayOrder?: number; isPublished?: boolean }): Promise<Topic> => {
    const res = await api.post<Topic>('/topics', {
      ...data,
      displayOrder: data.displayOrder ?? data.orderIndex ?? 1,
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  update: async (id: number, data: { name: string; description: string; orderIndex?: number; displayOrder?: number; isPublished?: boolean }): Promise<Topic> => {
    const res = await api.put<Topic>(`/topics/${id}`, {
      ...data,
      displayOrder: data.displayOrder ?? data.orderIndex ?? 1,
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/topics/${id}`);
  },
};

// ================= Study Materials =================
export const materialsApi = {
  getByTopic: async (topicId: number): Promise<StudyMaterial[]> => {
    const res = await api.get<StudyMaterial[]>(`/materials/topic/${topicId}`);
    return res.data;
  },
  getById: async (id: number): Promise<StudyMaterial> => {
    const res = await api.get<StudyMaterial>(`/materials/${id}`);
    return res.data;
  },
  create: async (data: { topicId: number; title: string; contentType?: string; content: string; orderIndex?: number; displayOrder?: number; isPublished?: boolean }): Promise<StudyMaterial> => {
    const res = await api.post<StudyMaterial>('/materials', {
      ...data,
      displayOrder: data.displayOrder ?? data.orderIndex ?? 1,
      attachmentType: data.contentType ?? 'NOTES',
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  update: async (id: number, data: { title: string; contentType?: string; content: string; orderIndex?: number; displayOrder?: number; isPublished?: boolean }): Promise<StudyMaterial> => {
    const res = await api.put<StudyMaterial>(`/materials/${id}`, {
      ...data,
      displayOrder: data.displayOrder ?? data.orderIndex ?? 1,
      attachmentType: data.contentType ?? 'NOTES',
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/materials/${id}`);
  },
  markProgress: async (materialId: number, completed: boolean): Promise<void> => {
    await api.post(`/materials/${materialId}/progress`, { completed });
  },
};

// ================= Exams =================
export const examsApi = {
  getAll: async (params?: { subjectId?: number; examType?: string }): Promise<Exam[]> => {
    const res = await api.get<Exam[]>('/exams', { params });
    return res.data;
  },
  getById: async (id: number): Promise<Exam> => {
    const res = await api.get<Exam>(`/exams/${id}`);
    return res.data;
  },
  create: async (data: {
    subjectId: number;
    title: string;
    description: string;
    examType: string;
    durationMinutes: number;
    totalMarks: number;
    passingMarks: number;
    isActive?: boolean;
    topicId?: number;
  }): Promise<Exam> => {
    const res = await api.post<Exam>('/exams', {
      ...data,
      isActive: data.isActive ?? true,
      assessmentType: data.examType === 'PRACTICE' || data.examType === 'PRACTICE_QUIZ' ? 'PRACTICE_QUIZ' : 'FORMAL_EXAM',
    });
    return res.data;
  },
  update: async (
    id: number,
    data: {
      subjectId: number;
      title: string;
      description: string;
      examType: string;
      durationMinutes: number;
      totalMarks: number;
      passingMarks: number;
      isActive?: boolean;
      topicId?: number;
    }
  ): Promise<Exam> => {
    const res = await api.put<Exam>(`/exams/${id}`, {
      ...data,
      isActive: data.isActive ?? true,
      assessmentType: data.examType === 'PRACTICE' || data.examType === 'PRACTICE_QUIZ' ? 'PRACTICE_QUIZ' : 'FORMAL_EXAM',
    });
    return res.data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/exams/${id}`);
  },
};

// ================= Questions =================
export const questionsApi = {
  getByExam: async (examId: number): Promise<Question[]> => {
    const res = await api.get<Question[]>(`/questions/exam/${examId}`);
    return res.data;
  },
  getAll: async (params?: { topicId?: number; examId?: number }): Promise<Question[]> => {
    const res = await api.get<Question[]>('/questions', { params });
    return res.data;
  },
  create: async (data: {
    examId?: number;
    topicId?: number;
    questionText: string;
    optionA: string;
    optionB: string;
    optionC: string;
    optionD: string;
    correctOption: string;
    marks: number;
    explanation?: string;
    isPublished?: boolean;
  }): Promise<Question> => {
    const res = await api.post<Question>('/questions', {
      ...data,
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  update: async (
    id: number,
    data: {
      examId?: number;
      topicId?: number;
      questionText: string;
      optionA: string;
      optionB: string;
      optionC: string;
      optionD: string;
      correctOption: string;
      marks: number;
      explanation?: string;
      isPublished?: boolean;
    }
  ): Promise<Question> => {
    const res = await api.put<Question>(`/questions/${id}`, {
      ...data,
      isPublished: data.isPublished ?? true,
    });
    return res.data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/questions/${id}`);
  },
};

// ================= Attempts & Timer =================
export const attemptsApi = {
  startAttempt: async (examId: number): Promise<StartAttemptResponse> => {
    const res = await api.post<StartAttemptResponse>('/attempts/start', { examId });
    return res.data;
  },
  saveAnswer: async (attemptId: number, questionId: number, selectedOption: string | null): Promise<void> => {
    await api.post(`/attempts/${attemptId}/answers`, { questionId, selectedOption });
  },
  submitAttempt: async (attemptId: number): Promise<AttemptResultResponse> => {
    const res = await api.post<AttemptResultResponse>(`/attempts/${attemptId}/submit`);
    return res.data;
  },
  getHistory: async (): Promise<StudentExamAttempt[]> => {
    const res = await api.get<StudentExamAttempt[]>('/attempts/history');
    return res.data;
  },
  getResult: async (attemptId: number): Promise<AttemptResultResponse> => {
    const res = await api.get<AttemptResultResponse>(`/attempts/${attemptId}/result`);
    return res.data;
  },
};

// ================= Analytics =================
export const analyticsApi = {
  getStudentOverview: async (): Promise<StudentAnalytics> => {
    const res = await api.get<StudentAnalytics>('/analytics/student/overview');
    return res.data;
  },
  getAdminOverview: async (): Promise<AdminAnalytics> => {
    const res = await api.get<AdminAnalytics>('/analytics/admin/overview');
    return res.data;
  },
};

// ================= AI Assistant & Drafting =================
export const aiApi = {
  askStudyAssistant: async (message: string, context?: string): Promise<{ reply: string; provider: string; model: string }> => {
    const res = await api.post<{ reply: string; provider: string; model: string }>('/ai/study-assistant', { message, context });
    return res.data;
  },
  draftQuestion: async (params: { topic: string; difficulty: string; marks: number }): Promise<AiDraftQuestionResponse> => {
    const res = await api.post<AiDraftQuestionResponse>('/ai/draft-question', params);
    return res.data;
  },
  getRecommendations: async (): Promise<{ recommendations: string[] }> => {
    const res = await api.get<{ recommendations: string[] }>('/ai/recommendations');
    return res.data;
  },
  getPerformanceInsights: async (attemptId: number): Promise<{ insights: string }> => {
    const res = await api.get<{ insights: string }>(`/ai/performance-insights/${attemptId}`);
    return res.data;
  },
};

export default api;
