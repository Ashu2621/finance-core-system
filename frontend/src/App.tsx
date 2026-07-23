import {
  ArrowDownRight,
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  CircleDollarSign,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Menu,
  Pencil,
  Plus,
  ReceiptText,
  Search,
  ShieldCheck,
  Trash2,
  TrendingDown,
  TrendingUp,
  Users,
  WalletCards,
  X
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { api, ApiClientError } from "./api";
import type {
  AuthSession,
  DashboardSummary,
  FinancialRecord,
  Page,
  RecordInput,
  RecordType,
  User,
  UserRole
} from "./types";

const SESSION_KEY = "finaxis.session";
type View = "overview" | "records" | "team";

const emptyPage = <T,>(): Page<T> => ({
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
  first: true,
  last: true
});

function loadStoredSession(): AuthSession | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as AuthSession;
    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    const fields = error.body?.fieldErrors;
    if (fields && Object.keys(fields).length) {
      return Object.values(fields).join(" · ");
    }
    return error.message;
  }
  return "Something went wrong. Please try again.";
}

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(value);
}

function shortDate(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(new Date(`${value}T00:00:00`));
}

export default function App() {
  const [session, setSession] = useState<AuthSession | null>(loadStoredSession);
  const [view, setView] = useState<View>("overview");
  const [menuOpen, setMenuOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const onAuthenticated = (next: AuthSession) => {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
    setSession(next);
  };

  const logout = useCallback(() => {
    sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
    setView("overview");
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 3500);
    return () => window.clearTimeout(timer);
  }, [toast]);

  if (!session) {
    return <AuthScreen onAuthenticated={onAuthenticated} />;
  }

  const navigate = (next: View) => {
    setView(next);
    setMenuOpen(false);
  };

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <div className="brand">
          <div className="brand-mark"><TrendingUp size={21} /></div>
          <div>
            <strong>Finaxis</strong>
            <span>Finance intelligence</span>
          </div>
        </div>

        <nav aria-label="Primary navigation">
          <NavButton
            active={view === "overview"}
            icon={<LayoutDashboard size={19} />}
            label="Overview"
            onClick={() => navigate("overview")}
          />
          <NavButton
            active={view === "records"}
            icon={<ReceiptText size={19} />}
            label="Transactions"
            onClick={() => navigate("records")}
          />
          {session.user.role === "ADMIN" && (
            <NavButton
              active={view === "team"}
              icon={<Users size={19} />}
              label="Team access"
              onClick={() => navigate("team")}
            />
          )}
        </nav>

        <div className="sidebar-foot">
          <div className="user-card">
            <div className="avatar">{session.user.name.charAt(0).toUpperCase()}</div>
            <div>
              <strong>{session.user.name}</strong>
              <span>{session.user.role}</span>
            </div>
          </div>
          <button className="icon-button" onClick={logout} aria-label="Sign out">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      {menuOpen && <button className="scrim" onClick={() => setMenuOpen(false)} aria-label="Close menu" />}

      <main className="main-content">
        <header className="topbar">
          <button className="mobile-menu" onClick={() => setMenuOpen(true)} aria-label="Open menu">
            <Menu size={21} />
          </button>
          <div>
            <p className="eyebrow">FINANCIAL COMMAND CENTER</p>
            <h1>
              {view === "overview" && "Your money, in focus."}
              {view === "records" && "Transactions"}
              {view === "team" && "Team access"}
            </h1>
          </div>
          <div className="secure-pill"><ShieldCheck size={16} /> Encrypted session</div>
        </header>

        {view === "overview" && (
          <Overview token={session.token} onManage={() => navigate("records")} />
        )}
        {view === "records" && (
          <Records
            token={session.token}
            canWrite={session.user.role !== "VIEWER"}
            notify={setToast}
            onUnauthorized={logout}
          />
        )}
        {view === "team" && session.user.role === "ADMIN" && (
          <Team token={session.token} currentUser={session.user} notify={setToast} />
        )}
      </main>

      {toast && <div className="toast" role="status">{toast}</div>}
    </div>
  );
}

function NavButton({
  active,
  icon,
  label,
  onClick
}: {
  active: boolean;
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button className={`nav-button ${active ? "active" : ""}`} onClick={onClick}>
      {icon}<span>{label}</span>
    </button>
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (session: AuthSession) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    const data = new FormData(event.currentTarget);
    try {
      const result = mode === "login"
        ? await api.login(String(data.get("email")), String(data.get("password")))
        : await api.register(
            String(data.get("name")),
            String(data.get("email")),
            String(data.get("password"))
          );
      onAuthenticated(result);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-story">
        <div className="auth-brand">
          <div className="brand-mark"><TrendingUp size={21} /></div>
          <strong>Finaxis</strong>
        </div>
        <div className="story-copy">
          <p className="eyebrow">CLARITY COMPOUNDS</p>
          <h1>Turn every transaction into a better decision.</h1>
          <p>
            A secure finance workspace that makes cash flow, spending patterns,
            and team access immediately understandable.
          </p>
        </div>
        <div className="trust-grid">
          <span><ShieldCheck size={18} /> Role-based access</span>
          <span><CircleDollarSign size={18} /> Live analytics</span>
          <span><ReceiptText size={18} /> Searchable ledger</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <p className="eyebrow">{mode === "login" ? "WELCOME BACK" : "CREATE YOUR WORKSPACE"}</p>
          <h2>{mode === "login" ? "Sign in to Finaxis" : "Start with a secure account"}</h2>
          <p className="muted">
            {mode === "login"
              ? "Use your credentials to open the dashboard."
              : "New accounts start with safe, read-only access."}
          </p>

          <form onSubmit={submit}>
            {mode === "register" && (
              <label>
                Full name
                <input name="name" minLength={2} maxLength={100} required autoComplete="name" />
              </label>
            )}
            <label>
              Email address
              <input name="email" type="email" required autoComplete="email" />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                minLength={8}
                maxLength={72}
                required
                autoComplete={mode === "login" ? "current-password" : "new-password"}
              />
              {mode === "register" && (
                <small>Use uppercase, lowercase, and a number.</small>
              )}
            </label>
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="primary-button full" disabled={loading}>
              {loading && <LoaderCircle className="spin" size={18} />}
              {mode === "login" ? "Sign in securely" : "Create account"}
            </button>
          </form>

          <button
            className="text-button"
            onClick={() => {
              setMode(mode === "login" ? "register" : "login");
              setError(null);
            }}
          >
            {mode === "login"
              ? "New to Finaxis? Create an account"
              : "Already have an account? Sign in"}
          </button>
        </div>
      </section>
    </main>
  );
}

function Overview({ token, onManage }: { token: string; onManage: () => void }) {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [records, setRecords] = useState<FinancialRecord[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([api.dashboard(token), api.records(token, 0, 5)])
      .then(([dashboard, page]) => {
        setSummary(dashboard);
        setRecords(page.content);
      })
      .catch((requestError) => setError(errorMessage(requestError)));
  }, [token]);

  if (error) return <ErrorState message={error} />;
  if (!summary) return <LoadingState />;

  const categoryEntries = Object.entries(summary.categoryBreakdown);
  const maxCategory = Math.max(1, ...categoryEntries.map(([, amount]) => amount));

  return (
    <div className="page-stack">
      <section className="metric-grid">
        <MetricCard
          label="Net balance"
          value={money(summary.balance)}
          icon={<WalletCards size={21} />}
          tone="primary"
          detail={`${summary.transactionCount} total transactions`}
        />
        <MetricCard
          label="Income"
          value={money(summary.totalIncome)}
          icon={<ArrowUpRight size={21} />}
          tone="positive"
          detail="All recorded inflows"
        />
        <MetricCard
          label="Expenses"
          value={money(summary.totalExpense)}
          icon={<ArrowDownRight size={21} />}
          tone="negative"
          detail="All recorded outflows"
        />
      </section>

      <section className="dashboard-grid">
        <article className="panel category-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">EXPENSE MIX</p>
              <h2>Where your money goes</h2>
            </div>
            <TrendingDown size={22} />
          </div>
          {categoryEntries.length === 0 ? (
            <EmptyInline message="Add expense transactions to see category insights." />
          ) : (
            <div className="category-list">
              {categoryEntries.map(([category, amount], index) => (
                <div className="category-row" key={category}>
                  <div className="category-meta">
                    <span><i style={{ background: `hsl(${155 + index * 42} 65% 58%)` }} />{category}</span>
                    <strong>{money(amount)}</strong>
                  </div>
                  <div className="bar-track">
                    <span style={{ width: `${(amount / maxCategory) * 100}%` }} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </article>

        <article className="panel recent-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">LATEST ACTIVITY</p>
              <h2>Recent transactions</h2>
            </div>
            <button className="text-button compact" onClick={onManage}>View all</button>
          </div>
          {records.length === 0 ? (
            <EmptyInline message="No transactions yet." />
          ) : (
            <div className="recent-list">
              {records.map((record) => (
                <div className="recent-item" key={record.id}>
                  <div className={`record-icon ${record.type.toLowerCase()}`}>
                    {record.type === "INCOME"
                      ? <ArrowUpRight size={18} />
                      : <ArrowDownRight size={18} />}
                  </div>
                  <div className="recent-copy">
                    <strong>{record.category}</strong>
                    <span>{shortDate(record.date)}{record.note ? ` · ${record.note}` : ""}</span>
                  </div>
                  <strong className={record.type === "INCOME" ? "positive-text" : ""}>
                    {record.type === "EXPENSE" ? "−" : "+"}{money(record.amount)}
                  </strong>
                </div>
              ))}
            </div>
          )}
        </article>
      </section>
    </div>
  );
}

function MetricCard({
  label,
  value,
  icon,
  tone,
  detail
}: {
  label: string;
  value: string;
  icon: React.ReactNode;
  tone: "primary" | "positive" | "negative";
  detail: string;
}) {
  return (
    <article className={`metric-card ${tone}`}>
      <div className="metric-top"><span>{label}</span><i>{icon}</i></div>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function Records({
  token,
  canWrite,
  notify,
  onUnauthorized
}: {
  token: string;
  canWrite: boolean;
  notify: (message: string) => void;
  onUnauthorized: () => void;
}) {
  const [page, setPage] = useState<Page<FinancialRecord>>(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [editor, setEditor] = useState<FinancialRecord | "new" | null>(null);

  const load = useCallback(async (pageNumber = 0, query = search) => {
    setLoading(true);
    setError(null);
    try {
      const result = query.trim()
        ? await api.searchRecords(token, query.trim(), pageNumber)
        : await api.records(token, pageNumber);
      setPage(result);
    } catch (requestError) {
      if (requestError instanceof ApiClientError && requestError.status === 401) {
        onUnauthorized();
        return;
      }
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [onUnauthorized, search, token]);

  useEffect(() => {
    void load(0, "");
  }, [token]);

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    void load(0, search);
  };

  const remove = async (record: FinancialRecord) => {
    if (!window.confirm(`Delete the ${record.category} transaction?`)) return;
    try {
      await api.deleteRecord(token, record.id);
      notify("Transaction deleted");
      void load(page.number);
    } catch (requestError) {
      notify(errorMessage(requestError));
    }
  };

  return (
    <div className="page-stack">
      <section className="toolbar panel">
        <form className="search-box" onSubmit={submitSearch}>
          <Search size={18} />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search category or note"
            aria-label="Search transactions"
          />
          {search && (
            <button type="button" onClick={() => { setSearch(""); void load(0, ""); }} aria-label="Clear search">
              <X size={17} />
            </button>
          )}
        </form>
        {canWrite ? (
          <button className="primary-button" onClick={() => setEditor("new")}>
            <Plus size={18} /> Add transaction
          </button>
        ) : (
          <span className="role-note"><ShieldCheck size={16} /> Read-only access</span>
        )}
      </section>

      <section className="panel table-panel">
        {loading ? <LoadingState compact /> : error ? <ErrorState message={error} /> : (
          <>
            <div className="table-meta">
              <span>{page.totalElements} transactions</span>
              <span>Page {page.totalPages === 0 ? 0 : page.number + 1} of {page.totalPages}</span>
            </div>
            {page.content.length === 0 ? (
              <EmptyInline message="No matching transactions found." />
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr><th>Date</th><th>Category</th><th>Type</th><th>Note</th><th>Amount</th><th aria-label="Actions" /></tr>
                  </thead>
                  <tbody>
                    {page.content.map((record) => (
                      <tr key={record.id}>
                        <td>{shortDate(record.date)}</td>
                        <td><strong>{record.category}</strong></td>
                        <td><span className={`type-badge ${record.type.toLowerCase()}`}>{record.type}</span></td>
                        <td className="note-cell">{record.note || "—"}</td>
                        <td className={`amount-cell ${record.type === "INCOME" ? "positive-text" : ""}`}>
                          {record.type === "EXPENSE" ? "−" : "+"}{money(record.amount)}
                        </td>
                        <td>
                          {canWrite && (
                            <div className="row-actions">
                              <button onClick={() => setEditor(record)} aria-label={`Edit ${record.category}`}><Pencil size={16} /></button>
                              <button className="danger" onClick={() => void remove(record)} aria-label={`Delete ${record.category}`}><Trash2 size={16} /></button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="pagination">
              <button disabled={page.first} onClick={() => void load(page.number - 1)}>
                <ChevronLeft size={17} /> Previous
              </button>
              <button disabled={page.last} onClick={() => void load(page.number + 1)}>
                Next <ChevronRight size={17} />
              </button>
            </div>
          </>
        )}
      </section>

      {editor && (
        <RecordEditor
          token={token}
          record={editor === "new" ? undefined : editor}
          onClose={() => setEditor(null)}
          onSaved={() => {
            setEditor(null);
            notify(editor === "new" ? "Transaction added" : "Transaction updated");
            void load(page.number);
          }}
        />
      )}
    </div>
  );
}

function RecordEditor({
  token,
  record,
  onClose,
  onSaved
}: {
  token: string;
  record?: FinancialRecord;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    const data = new FormData(event.currentTarget);
    const input: RecordInput = {
      amount: Number(data.get("amount")),
      type: String(data.get("type")) as RecordType,
      category: String(data.get("category")),
      date: String(data.get("date")),
      note: String(data.get("note") ?? "") || undefined
    };
    try {
      if (record) await api.updateRecord(token, record.id, input);
      else await api.createRecord(token, input);
      onSaved();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-layer" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="record-title">
        <div className="modal-heading">
          <div><p className="eyebrow">LEDGER ENTRY</p><h2 id="record-title">{record ? "Edit transaction" : "Add transaction"}</h2></div>
          <button className="icon-button" onClick={onClose} aria-label="Close"><X size={19} /></button>
        </div>
        <form onSubmit={submit} className="record-form">
          <div className="field-row">
            <label>Type
              <select name="type" defaultValue={record?.type ?? "EXPENSE"}>
                <option value="EXPENSE">Expense</option>
                <option value="INCOME">Income</option>
              </select>
            </label>
            <label>Amount
              <input name="amount" type="number" min="0.01" step="0.01" defaultValue={record?.amount} required />
            </label>
          </div>
          <label>Category
            <input name="category" maxLength={80} defaultValue={record?.category} placeholder="e.g. Groceries" required />
          </label>
          <label>Date
            <input
              name="date"
              type="date"
              max={new Date().toISOString().slice(0, 10)}
              defaultValue={record?.date ?? new Date().toISOString().slice(0, 10)}
              required
            />
          </label>
          <label>Note <span>(optional)</span>
            <textarea name="note" maxLength={500} defaultValue={record?.note} placeholder="Add context for this transaction" />
          </label>
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={onClose}>Cancel</button>
            <button className="primary-button" disabled={loading}>
              {loading && <LoaderCircle className="spin" size={18} />}
              {record ? "Save changes" : "Add transaction"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function Team({
  token,
  currentUser,
  notify
}: {
  token: string;
  currentUser: User;
  notify: (message: string) => void;
}) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    api.users(token)
      .then((result) => setUsers(result.content))
      .catch((requestError) => setError(errorMessage(requestError)))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(load, [load]);

  const changeRole = async (user: User, role: UserRole) => {
    try {
      await api.setUserRole(token, user.id, role);
      notify(`${user.name}'s role updated`);
      load();
    } catch (requestError) {
      notify(errorMessage(requestError));
    }
  };

  const toggleActive = async (user: User) => {
    try {
      await api.setUserActive(token, user.id, !user.active);
      notify(`${user.name} ${user.active ? "deactivated" : "activated"}`);
      load();
    } catch (requestError) {
      notify(errorMessage(requestError));
    }
  };

  return (
    <section className="panel table-panel">
      <div className="panel-heading">
        <div><p className="eyebrow">RBAC CONTROL</p><h2>Users and permissions</h2></div>
        <span className="role-note"><ShieldCheck size={16} /> Admin only</span>
      </div>
      {loading ? <LoadingState compact /> : error ? <ErrorState message={error} /> : (
        <div className="table-scroll">
          <table>
            <thead><tr><th>User</th><th>Status</th><th>Role</th><th>Joined</th><th /></tr></thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className="identity-cell">
                      <div className="avatar small">{user.name.charAt(0).toUpperCase()}</div>
                      <div><strong>{user.name}</strong><span>{user.email}</span></div>
                    </div>
                  </td>
                  <td><span className={`status-dot ${user.active ? "online" : ""}`}>{user.active ? "Active" : "Inactive"}</span></td>
                  <td>
                    <select
                      className="role-select"
                      value={user.role}
                      disabled={user.id === currentUser.id}
                      onChange={(event) => void changeRole(user, event.target.value as UserRole)}
                    >
                      <option value="VIEWER">Viewer</option>
                      <option value="ANALYST">Analyst</option>
                      <option value="ADMIN">Admin</option>
                    </select>
                  </td>
                  <td>{shortDate(user.createdAt.slice(0, 10))}</td>
                  <td>
                    <button
                      className="secondary-button compact"
                      disabled={user.id === currentUser.id}
                      onClick={() => void toggleActive(user)}
                    >
                      {user.active ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function LoadingState({ compact = false }: { compact?: boolean }) {
  return <div className={`state-box ${compact ? "compact" : ""}`}><LoaderCircle className="spin" size={24} /><span>Loading secure data…</span></div>;
}

function ErrorState({ message }: { message: string }) {
  return <div className="state-box error-state"><ShieldCheck size={24} /><strong>Unable to load data</strong><span>{message}</span></div>;
}

function EmptyInline({ message }: { message: string }) {
  return <div className="empty-inline"><CircleDollarSign size={25} /><span>{message}</span></div>;
}
