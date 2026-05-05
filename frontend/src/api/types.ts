export interface User {
  id: string;
  email: string;
  displayName: string;
  role: string;
  createdAt: string;
  phone: string | null;
  avatarUrl: string | null;
  accountLocked: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface QuizSummary {
  id: string;
  title: string;
  description: string | null;
  published: boolean;
  questionCount: number;
  groupRestricted: boolean;
  classOrDirectRestricted: boolean;
  opensAt: string | null;
  closesAt: string | null;
  maxAttempts: number;
}

export interface Question {
  id: string;
  text: string;
  choices: string[];
  correctChoiceIndex: number | null;
  orderIndex: number;
  questionType: string;
  points: number;
  explanation: string | null;
}

export interface QuizDetail {
  id: string;
  title: string;
  description: string | null;
  createdByUserId: string | null;
  published: boolean;
  questions: Question[];
  createdAt: string;
  updatedAt: string;
  timeLimitSeconds: number | null;
  timeBonusMax: number | null;
  studyGroupIds: string[];
  assignedClassIds: string[];
  assignedStudentUserIds: string[];
  opensAt: string | null;
  closesAt: string | null;
  maxAttempts: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  examPasswordRequired: boolean;
  blockCopyPaste: boolean;
  maxFullscreenExits: number | null;
  showAnswersToStudents: boolean;
}

export interface ResultResponse {
  resultId: string;
  submissionId: string;
  userId: string;
  quizId: string;
  score: number;
  maxScore: number;
  percentage: number;
  calculatedAt: string;
  timeBonus: number;
  rankScore: number;
}

export type LeaderboardPeriod = 'WEEK' | 'MONTH' | 'ALL';

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  displayName: string;
  score: number;
  maxScore: number;
  percentage: number;
  timeBonus: number;
  rankScore: number;
}

export interface PublicStatus {
  maintenanceMode: boolean;
  announcementMessage: string | null;
}

export interface MySubmissionSummary {
  submissionId: string;
  quizId: string;
  quizTitle: string;
  quizPublished: boolean;
  attemptNumber: number;
  submittedAt: string;
  score: number;
  maxScore: number;
  percentage: number;
  timeBonus: number;
  rankScore: number;
}
