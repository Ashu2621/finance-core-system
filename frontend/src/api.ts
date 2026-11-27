import type {
  ApiErrorBody,
  AuthSession,
  DashboardSummary,
  FinancialRecord,
  Page,
  RecordInput,
  User,
  UserRole
} from "./types";

const API_BASE = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");

export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly body?: ApiErrorBody
  ) {
    super(message);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    let body: ApiErrorBody | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    throw new ApiClientError(
      body?.message ?? `Request failed (${response.status})`,
      response.status,
      body
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const api = {
  login: (email: string, password: string) =>
    request<AuthSession>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    }),

  register: (name: string, email: string, password: string) =>
    request<AuthSession>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password })
    }),

  dashboard: (token: string) =>
    request<DashboardSummary>("/api/dashboard/summary", {}, token),

  records: (token: string, page = 0, size = 20) =>
    request<Page<FinancialRecord>>(
      `/api/records?page=${page}&size=${size}`,
      {},
      token
    ),

  searchRecords: (token: string, search: string, page = 0) =>
    request<Page<FinancialRecord>>(
      `/api/records/filter?search=${encodeURIComponent(search)}&page=${page}&size=20`,
      {},
      token
    ),

  createRecord: (token: string, input: RecordInput) =>
    request<FinancialRecord>(
      "/api/records",
      {
        method: "POST",
        headers: { "Idempotency-Key": crypto.randomUUID() },
        body: JSON.stringify(input)
      },
      token
    ),

  updateRecord: (token: string, id: number, input: RecordInput) =>
    request<FinancialRecord>(
      `/api/records/${id}`,
      { method: "PUT", body: JSON.stringify(input) },
      token
    ),

  deleteRecord: (token: string, id: number) =>
    request<void>(`/api/records/${id}`, { method: "DELETE" }, token),

  users: (token: string, page = 0) =>
    request<Page<User>>(`/api/users?page=${page}&size=50`, {}, token),

  setUserActive: (token: string, id: number, active: boolean) =>
    request<User>(
      `/api/users/${id}/${active ? "activate" : "deactivate"}`,
      { method: "PUT" },
      token
    ),

  setUserRole: (token: string, id: number, role: UserRole) =>
    request<User>(
      `/api/users/${id}/role`,
      { method: "PUT", body: JSON.stringify({ role }) },
      token
    )
};
