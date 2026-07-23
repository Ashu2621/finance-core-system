export type UserRole = "VIEWER" | "ANALYST" | "ADMIN";
export type RecordType = "INCOME" | "EXPENSE";

export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface AuthSession {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface DashboardSummary {
  totalIncome: number;
  totalExpense: number;
  balance: number;
  transactionCount: number;
  categoryBreakdown: Record<string, number>;
}

export interface FinancialRecord {
  id: number;
  amount: number;
  type: RecordType;
  category: string;
  date: string;
  note?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RecordInput {
  amount: number;
  type: RecordType;
  category: string;
  date: string;
  note?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorBody {
  message?: string;
  code?: string;
  fieldErrors?: Record<string, string>;
}
