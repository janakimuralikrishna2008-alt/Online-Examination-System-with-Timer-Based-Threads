export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: 'STUDENT' | 'ADMIN';
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Subject {
  id: number;
  name: string;
  code: string;
  description: string;
  topicsCount?: number;
  completedMaterialsCount?: number;
  totalMaterialsCount?: number;
}

export interface Topic {
  id: number;
  subjectId: number;
  subjectName?: string;
  name: string;
  description: string;
  orderIndex: number;
  completedMaterialsCount?: number;
  totalMaterialsCount?: number;
  progressPercentage?: number;
}

export interface StudyMaterial {
  id: number;
  topicId: number;
  topicName?: string;
  subjectId?: number;
  subjectName?: string;
  title: string;
  contentType: 'NOTES' | 'CODE_SNIPPET' | 'KEY_POINTS' | 'EXTERNAL_LINK';
  content: string;
  orderIndex: number;
  completed?: boolean;
  completedAt?: string;
}

export interface Exam {
  id: number;
  subjectId: number;
  subjectName?: string;
  title: string;
  description: string;
  examType: 'FORMAL' | 'PRACTICE';
  durationMinutes: number;
  totalMarks: number;
  passingMarks: number;
  isActive?: boolean;
  questionCount?: number;
  createdAt?: string;
}

export interface Question {
  id: number;
  examId?: number;
  topicId?: number;
  topicName?: string;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctOption: 'A' | 'B' | 'C' | 'D';
  marks: number;
  explanation?: string;
}

export interface StudentExamAttempt {
  id?: number;
  attemptId?: number;
  studentId?: number;
  studentName?: string;
  examId: number;
  examTitle: string;
  examType?: string;
  assessmentType?: string;
  subjectName?: string;
  startTime?: string;
  startedAt?: string;
  endTime?: string;
  completedAt?: string;
  deadlineTime?: string;
  remainingSeconds?: number;
  totalScore?: number;
  score?: number;
  totalMarks?: number;
  percentage?: number;
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'AUTO_SUBMITTED';
  passed?: boolean;
  isPassed?: boolean;
}

export interface AttemptQuestionView {
  questionId: number;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  marks: number;
  selectedOption?: 'A' | 'B' | 'C' | 'D' | null;
  isFlagged?: boolean;
}

export interface StartAttemptResponse {
  attemptId: number;
  examId: number;
  examTitle: string;
  examType: 'FORMAL' | 'PRACTICE';
  subjectName: string;
  durationMinutes: number;
  remainingSeconds: number;
  startTime: string;
  deadlineTime: string;
  questions: AttemptQuestionView[];
}

export interface QuestionResultReview {
  questionId: number;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  selectedOption: 'A' | 'B' | 'C' | 'D' | null;
  correctOption: 'A' | 'B' | 'C' | 'D';
  isCorrect: boolean;
  marksAwarded: number;
  possibleMarks: number;
  explanation: string;
}

export interface AttemptResultResponse {
  attemptId: number;
  examId: number;
  examTitle: string;
  subjectName: string;
  examType: 'FORMAL' | 'PRACTICE';
  startTime: string;
  endTime: string;
  status: 'SUBMITTED' | 'AUTO_SUBMITTED';
  earnedMarks: number;
  totalPossibleMarks: number;
  percentage: number;
  passed: boolean;
  passingMarks: number;
  questionReviews: QuestionResultReview[];
}

export interface StudentAnalytics {
  totalAttempts: number;
  passedAttempts: number;
  averageScorePercentage: number;
  completedMaterialsCount: number;
  recentAttempts: StudentExamAttempt[];
}

export interface AdminAnalytics {
  totalStudents: number;
  totalExams: number;
  totalQuestions: number;
  totalAttempts: number;
  averagePassingRate: number;
  recentAttempts: StudentExamAttempt[];
}

export interface AiChatMessage {
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
}

export interface AiDraftQuestionResponse {
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctOption: 'A' | 'B' | 'C' | 'D';
  explanation: string;
  marks: number;
}

export interface StudentRegisterData {
  username: string;
  email: string;
  password: string;
  fullName: string;
}

export interface AdminRegisterData {
  username: string;
  email: string;
  password: string;
  fullName: string;
  invitationCode: string;
}
