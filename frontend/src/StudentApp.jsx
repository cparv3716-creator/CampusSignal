import { useEffect, useMemo, useState } from "react";
import { request } from "./api.js";
import { summarizeEmail } from "./emailSummary.js";
import { safeHttpUrl, splitEmailBody } from "./emailReader.js";
import {
  filterOpportunities,
  isOpportunity,
  selectOpportunities,
} from "./opportunities.js";

const interestChoices = [
  "Internships",
  "Placements",
  "Scholarships",
  "Research opportunities",
  "Workshops",
  "Competitions",
  "Clubs and societies",
  "Sports",
  "Cultural events",
  "Entrepreneurship",
  "Higher studies",
  "Hackathons",
];
const blank = {
  name: "",
  college: "",
  department: "",
  programme: "",
  year: "",
  graduationYear: "",
  semester: "",
  interests: [],
};
const headers = (csrf) => ({
  "Content-Type": "application/json",
  "X-CampusSignal-CSRF": csrf,
});
const readable = (value) =>
  value?.replaceAll("_", " ").toLowerCase() || "uncategorized";
const title = (value) =>
  readable(value).replace(/\b\w/g, (letter) => letter.toUpperCase());
const date = (value) =>
  value
    ? new Date(String(value) + "T00:00:00").toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : "No deadline identified";
const received = (value) =>
  value
    ? new Date(value).toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : "Unknown date";
const urgency = (item) =>
  item?.urgency === "DUE_TODAY"
    ? "Due today"
    : item?.urgency === "DUE_SOON"
      ? `${item.daysRemaining} day${item.daysRemaining === 1 ? "" : "s"} left`
      : item?.urgency === "UPCOMING"
        ? `${item.daysRemaining} days left`
        : item?.urgency === "OVERDUE"
          ? "Overdue"
          : "No deadline";
const urgencyTone = (item) =>
  `badge urgency ${(item?.urgency || "NO_DEADLINE").toLowerCase()}`;
const preferredName = (value) =>
  String(value || "")
    .replace(
      /\s*\((?:student\s*)?(?:id|roll(?:\s*(?:no|number))?|registration(?:\s*(?:no|number))?)[^)]*\)\s*$/i,
      "",
    )
    .replace(
      /\s*[-|–—]\s*(?:student\s*)?(?:id|roll(?:\s*(?:no|number))?|registration(?:\s*(?:no|number))?)\s*[:#-]?\s*[\w-]+\s*$/i,
      "",
    )
    .trim();

export default function StudentApp() {
  const [session, setSession] = useState(undefined);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(blank);
  const [emails, setEmails] = useState([]);
  const [feed, setFeed] = useState([]);
  const [actions, setActions] = useState([]);
  const [content, setContent] = useState({});
  const [candidates, setCandidates] = useState({});
  const [view, setView] = useState("overview");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [theme, setTheme] = useState(
    () => localStorage.getItem("campussignal-theme") || "light",
  );
  const [menuOpen, setMenuOpen] = useState(false);
  const [detailId, setDetailId] = useState(null);
  const [inboxFilters, setInboxFilters] = useState({
    query: "",
    category: "",
    urgency: "",
    priority: "",
    order: "newest",
  });
  const [priorityFilter, setPriorityFilter] = useState("All");
  const [actionFilter, setActionFilter] = useState("PENDING");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [previewing, setPreviewing] = useState(false);
  const [previewFeed, setPreviewFeed] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [spotlight, setSpotlight] = useState(null);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [demoMode, setDemoMode] = useState(false);
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("campussignal-theme", theme);
  }, [theme]);
  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape") {
        setDetailId(null);
        setMenuOpen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  useEffect(() => {
    const recover = () => {
      setSessionExpired(true);
      setSession(null);
      setProfile(null);
      setDetailId(null);
      setMenuOpen(false);
      setError("");
    };
    window.addEventListener("campussignal:session-expired", recover);
    return () =>
      window.removeEventListener("campussignal:session-expired", recover);
  }, []);
  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(""), 5000);
    return () => window.clearTimeout(timer);
  }, [notice]);
  const load = async (active) => {
    const current = active || session;
    if (!current) return [];
    setLoading(true);
    try {
      const [saved, ranked, inbox, tracked] = await Promise.all([
        request("/api/v1/me/profile").catch((reason) => {
          if (reason.status === 404 && !current.profileComplete) return null;
          if (reason.status === 404)
            throw new Error(
              "Your account profile could not be restored. Refresh once, or sign in again to continue securely.",
            );
          throw reason;
        }),
        request("/api/v1/me/email-feed").catch((reason) =>
          reason.status === 404 ? [] : Promise.reject(reason),
        ),
        request("/api/v1/me/emails"),
        request("/api/v1/me/actions"),
      ]);
      setProfile(saved);
      setForm(saved || { ...blank, name: preferredName(current.name) });
      setFeed(ranked);
      setEmails(inbox);
      setActions(tracked);
      return ranked;
    } finally {
      setLoading(false);
    }
  };
  const applyDemo = (workspace) => {
    setProfile(workspace.profile);
    setForm(workspace.profile);
    setEmails(workspace.emails);
    setFeed(workspace.feed);
    setActions(workspace.actions);
    setContent({});
    setCandidates({});
  };
  const loadDemo = async () => {
    setLoading(true);
    try {
      const workspace = await request("/api/v1/me/demo");
      applyDemo(workspace);
      return workspace.feed;
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    request("/api/v1/auth/session")
      .then((active) => {
        setSession(active);
        return load(active);
      })
      .catch((reason) => {
        setSession(null);
        if (reason.status !== 401) setError(reason.message);
      });
  }, []);
  const refresh = async () => {
    setError("");
    try {
      await (demoMode ? loadDemo() : load());
      setNotice(
        demoMode
          ? "Demo workspace refreshed."
          : "Your live workspace is up to date.",
      );
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    }
  };
  const syncGmail = async () => {
    if (demoMode) {
      setError(
        "Gmail Sync is unavailable in Demo Mode. Switch to your live workspace to sync your own inbox.",
      );
      return;
    }
    if (!session.gmailConnected) {
      setError(
        "Gmail is not connected for this account. Reconnect Gmail before syncing.",
      );
      return;
    }
    if (
      !window.confirm(
        "Sync up to 10 recent Gmail messages for this signed-in account? Existing messages stay private and duplicates will not be imported again.",
      )
    )
      return;
    setSyncing(true);
    setError("");
    try {
      const result = await request("/api/v1/me/gmail/sync", {
        method: "POST",
        headers: headers(session.csrfToken),
      });
      await load();
      setNotice(
        `Gmail sync complete: ${result.fetched} fetched, ${result.created} newly imported, ${result.alreadyExisted} already stored${result.failed ? `, ${result.failed} could not be imported` : ""}.`,
      );
    } catch (reason) {
      if (reason.status === 403) {
        setSession((old) => ({ ...old, gmailConnected: false }));
        setError(
          "Gmail authorization needs to be renewed before syncing. Select Reconnect Gmail and approve access.",
        );
      } else if (reason.status !== 401)
        setError(
          reason.message ||
            "Gmail sync could not be completed. Please try again.",
        );
    } finally {
      setSyncing(false);
    }
  };
  const save = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    const previous = new Map(
      feed.map((item, index) => [item.emailId, index + 1]),
    );
    try {
      if (demoMode) {
        const workspace = await request("/api/v1/me/demo/profile", {
          method: "PUT",
          headers: headers(session.csrfToken),
          body: JSON.stringify({
            ...form,
            year: Number(form.year),
            graduationYear: Number(form.graduationYear),
            semester: form.semester ? Number(form.semester) : null,
          }),
        });
        applyDemo(workspace);
        const moved = workspace.feed
          .map((item, index) => ({
            ...item,
            before: previous.get(item.emailId),
            after: index + 1,
          }))
          .filter((item) => item.before && item.before !== item.after)
          .slice(0, 3);
        setSpotlight({ interests: workspace.profile.interests, moved });
        setView("priority");
        setNotice(
          "Demo interests saved — the fixture ranking was recalculated.",
        );
        return;
      }
      const saved = await request("/api/v1/me/profile", {
        method: "PUT",
        headers: headers(session.csrfToken),
        body: JSON.stringify({
          ...form,
          year: Number(form.year),
          graduationYear: Number(form.graduationYear),
          semester: form.semester ? Number(form.semester) : null,
        }),
      });
      setProfile(saved);
      setForm(saved);
      const ranked = await load();
      const moved = ranked
        .map((item, index) => ({
          ...item,
          before: previous.get(item.emailId),
          after: index + 1,
        }))
        .filter((item) => item.before && item.before !== item.after)
        .slice(0, 3);
      setSpotlight({ interests: saved.interests, moved });
      setView("priority");
      setNotice(
        "Profile saved — your personalized ranking has been refreshed.",
      );
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    } finally {
      setBusy(false);
    }
  };
  const previewRanking = async (interests) => {
    if (demoMode) {
      setError(
        "Use My Profile in Demo Mode to change fixture interests and recalculate its ranking.",
      );
      return;
    }
    setPreviewing(true);
    setError("");
    try {
      const preview = await request("/api/v1/me/email-feed/preview", {
        method: "POST",
        headers: headers(session.csrfToken),
        body: JSON.stringify({
          ...form,
          interests,
          year: Number(form.year),
          graduationYear: Number(form.graduationYear),
          semester: form.semester ? Number(form.semester) : null,
        }),
      });
      setPreviewFeed(preview);
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    } finally {
      setPreviewing(false);
    }
  };
  const savePreviewInterests = async (interests) => {
    const next = { ...form, interests };
    setBusy(true);
    setError("");
    const previous = new Map(
      feed.map((item, index) => [item.emailId, index + 1]),
    );
    try {
      if (demoMode) {
        const workspace = await request("/api/v1/me/demo/profile", {
          method: "PUT",
          headers: headers(session.csrfToken),
          body: JSON.stringify({
            ...next,
            year: Number(next.year),
            graduationYear: Number(next.graduationYear),
            semester: next.semester ? Number(next.semester) : null,
          }),
        });
        applyDemo(workspace);
        const moved = workspace.feed
          .map((item, index) => ({
            ...item,
            before: previous.get(item.emailId),
            after: index + 1,
          }))
          .filter((item) => item.before && item.before !== item.after)
          .slice(0, 3);
        setSpotlight({ interests: workspace.profile.interests, moved });
        setPreviewFeed(null);
        setNotice("Demo interests saved and ranking recalculated.");
        return;
      }
      const saved = await request("/api/v1/me/profile", {
        method: "PUT",
        headers: headers(session.csrfToken),
        body: JSON.stringify({
          ...next,
          year: Number(next.year),
          graduationYear: Number(next.graduationYear),
          semester: next.semester ? Number(next.semester) : null,
        }),
      });
      setProfile(saved);
      setForm(saved);
      setPreviewFeed(null);
      const ranked = await load();
      const moved = ranked
        .map((item, index) => ({
          ...item,
          before: previous.get(item.emailId),
          after: index + 1,
        }))
        .filter((item) => item.before && item.before !== item.after)
        .slice(0, 3);
      setSpotlight({ interests: saved.interests, moved });
      setView("priority");
      setNotice("Saved interests and refreshed your personalized ranking.");
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    } finally {
      setBusy(false);
    }
  };
  const openDetail = async (id) => {
    setDetailId(id);
    if (content[id]?.message || content[id]?.loading) return;
    setContent((old) => ({ ...old, [id]: { loading: true } }));
    try {
      const message = await request(
        demoMode
          ? `/api/v1/me/demo/emails/${id}/content`
          : `/api/v1/me/emails/${id}/content`,
      );
      setContent((old) => ({ ...old, [id]: { message } }));
    } catch (reason) {
      if (reason.status !== 401)
        setContent((old) => ({ ...old, [id]: { error: reason.message } }));
    }
  };
  const candidate = async (id) => {
    setCandidates((old) => ({ ...old, [id]: { loading: true } }));
    try {
      const result = await request(
        demoMode
          ? `/api/v1/me/demo/emails/${id}/action-candidate`
          : `/api/v1/me/emails/${id}/action-candidate`,
      );
      setCandidates((old) => ({
        ...old,
        [id]: { checked: true, candidate: result },
      }));
    } catch (reason) {
      if (reason.status !== 401)
        setCandidates((old) => ({
          ...old,
          [id]: { checked: true, error: reason.message },
        }));
    }
  };
  const track = async (id, action) => {
    setBusy(true);
    setError("");
    try {
      if (demoMode) {
        const workspace = await request(
          `/api/v1/me/demo/emails/${id}/actions`,
          { method: "POST", headers: headers(session.csrfToken) },
        );
        applyDemo(workspace);
        setNotice(
          "Demo action added. It is isolated from your real action list.",
        );
        return;
      }
      await request(`/api/v1/me/emails/${id}/actions`, {
        method: "POST",
        headers: headers(session.csrfToken),
        body: JSON.stringify(action),
      });
      setNotice("Action added to My Actions.");
      await load();
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    } finally {
      setBusy(false);
    }
  };
  const updateStatus = async (id, status) => {
    setBusy(true);
    setError("");
    try {
      if (demoMode) {
        const workspace = await request(
          `/api/v1/me/demo/actions/${id}/status`,
          {
            method: "PATCH",
            headers: headers(session.csrfToken),
            body: JSON.stringify({ status }),
          },
        );
        applyDemo(workspace);
        setNotice(
          status === "COMPLETED"
            ? "Demo action marked complete."
            : "Demo action reopened.",
        );
        return;
      }
      await request(`/api/v1/me/actions/${id}/status`, {
        method: "PATCH",
        headers: headers(session.csrfToken),
        body: JSON.stringify({ status }),
      });
      setNotice(
        status === "COMPLETED"
          ? "Action marked complete."
          : status === "PENDING"
            ? "Action reopened in your pending queue."
            : "Action marked as not interested.",
      );
      await load();
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    } finally {
      setBusy(false);
    }
  };
  const logout = async () => {
    try {
      await request("/api/v1/auth/logout", {
        method: "POST",
        headers: headers(session.csrfToken),
      });
    } finally {
      setSession(null);
      setProfile(null);
      setMenuOpen(false);
    }
  };
  const disconnect = async () => {
    if (
      !window.confirm(
        "Disconnect Gmail? Imported emails remain safely in your account.",
      )
    )
      return;
    try {
      await request("/api/v1/auth/gmail/disconnect", {
        method: "POST",
        headers: headers(session.csrfToken),
      });
      setSession((old) => ({ ...old, gmailConnected: false }));
      setNotice("Gmail disconnected. Your stored email remains available.");
    } catch (reason) {
      setError(reason.message);
    }
  };
  const goInbox = (filters) => {
    setInboxFilters({
      query: "",
      category: "",
      urgency: "",
      priority: "",
      order: "newest",
      ...filters,
    });
    setView("inbox");
  };
  const switchWorkspace = async (nextDemoMode) => {
    setError("");
    setDetailId(null);
    setDemoMode(nextDemoMode);
    try {
      if (nextDemoMode) {
        await loadDemo();
        setNotice(
          "Demo Mode is on. All messages and actions are fictional, isolated fixtures.",
        );
      } else {
        await load();
        setNotice("Returned to your private live workspace.");
      }
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    }
  };
  const resetDemo = async () => {
    setBusy(true);
    try {
      const workspace = await request("/api/v1/me/demo/reset", {
        method: "POST",
        headers: headers(session.csrfToken),
      });
      applyDemo(workspace);
      setNotice(
        "Demo fixtures, interests, and demo actions were reset. Your live workspace was not changed.",
      );
    } catch (reason) {
      if (reason.status !== 401) setError(reason.message);
    } finally {
      setBusy(false);
    }
  };
  const actionsByEmail = useMemo(
    () => new Map(actions.map((item) => [item.emailId, item])),
    [actions],
  );
  const feedByEmail = useMemo(
    () => new Map(feed.map((item) => [item.emailId, item])),
    [feed],
  );
  const metrics = useMemo(() => {
    const categories = Object.entries(
      emails.reduce(
        (all, item) => ({
          ...all,
          [item.category || "UNCATEGORIZED"]:
            (all[item.category || "UNCATEGORIZED"] || 0) + 1,
        }),
        {},
      ),
    ).sort((a, b) => b[1] - a[1]);
    return {
      total: emails.length,
      high: feed.filter((item) => item.rankingScore >= 60).length,
      opportunities: selectOpportunities(feed).length,
      deadlines: feed.filter((item) => item.deadline).length,
      pending: actions.filter((item) => item.effectiveStatus === "PENDING")
        .length,
      categories,
    };
  }, [actions, emails, feed]);
  const deadlines = useMemo(() => {
    const byEmail = new Map();
    feed
      .filter((item) => item.deadline)
      .forEach((item) =>
        byEmail.set(item.emailId, {
          ...item,
          kind: "Email",
          status:
            actionsByEmail.get(item.emailId)?.effectiveStatus || "UNTRACKED",
        }),
      );
    actions
      .filter((item) => item.deadline)
      .forEach((item) =>
        byEmail.set(item.emailId, {
          ...feedByEmail.get(item.emailId),
          ...item,
          kind: "Action",
          status: item.effectiveStatus,
        }),
      );
    return [...byEmail.values()].sort((a, b) =>
      String(a.deadline).localeCompare(String(b.deadline)),
    );
  }, [actions, actionsByEmail, feed, feedByEmail]);
  if (session === undefined)
    return (
      <main className="login-page">
        <div className="loading-state">
          <span className="spinner" />
          Restoring your secure session...
        </div>
      </main>
    );
  if (!session)
    return (
      <Login
        error={error}
        expired={sessionExpired}
        theme={theme}
        setTheme={setTheme}
      />
    );
  if (!profile)
    return (
      <Onboarding
        session={session}
        form={form}
        setForm={setForm}
        save={save}
        busy={busy}
        logout={logout}
        error={error}
        theme={theme}
        setTheme={setTheme}
      />
    );
  const currentEmail = emails.find((item) => item.id === detailId);
  return (
    <main
      className={`app-shell ${sidebarCollapsed ? "sidebar-collapsed" : ""}`}
    >
      <header className="topbar">
        <Brand />
        <div className="top-actions">
          <button
            className="demo-mode-toggle"
            onClick={() => switchWorkspace(!demoMode)}
          >
            {demoMode ? "Return to live workspace" : "Try Demo Mode"}
          </button>
          <span className="connection">
            <i />
            {demoMode
              ? "Demo fixtures only"
              : session.gmailConnected
                ? "Gmail connected"
                : "Gmail paused"}
          </span>
          <button
            className="icon-button"
            aria-label="Toggle color theme"
            onClick={() =>
              setTheme((old) => (old === "light" ? "dark" : "light"))
            }
          >
            {theme === "light" ? "◐" : "☀"}
          </button>
          <button
            className="avatar-button"
            aria-expanded={menuOpen}
            aria-label="Open profile menu"
            onClick={() => setMenuOpen((old) => !old)}
          >
            {(profile.name || session.name || "S").slice(0, 1).toUpperCase()}
          </button>
          {menuOpen && (
            <div className="profile-menu">
              <strong>{profile.name}</strong>
              <span>{session.email}</span>
              <button
                onClick={() => {
                  setView("profile");
                  setMenuOpen(false);
                }}
              >
                Edit profile
              </button>
              {session.gmailConnected && (
                <button onClick={disconnect}>Disconnect Gmail</button>
              )}
              <button onClick={logout}>Log out</button>
            </div>
          )}
        </div>
      </header>
      <div className="dashboard-layout">
        <aside className="sidebar">
          <button
            className="collapse-button"
            aria-label="Toggle sidebar"
            onClick={() => setSidebarCollapsed((old) => !old)}
          >
            {sidebarCollapsed ? "›" : "‹"}
          </button>
          <div className="sidebar-intro">
            <p className="sidebar-kicker">Your workspace</p>
            <h1>Stay ahead of what matters.</h1>
            <p>Private campus information, prioritized for your profile.</p>
          </div>
          <nav className="side-nav" aria-label="CampusSignal sections">
            <Nav
              view={view}
              setView={setView}
              value="overview"
              icon="⌂"
              label="Overview"
            />
            <Nav
              view={view}
              setView={setView}
              value="briefing"
              icon="☀"
              label="Daily Briefing"
            />
            <Nav
              view={view}
              setView={setView}
              value="inbox"
              icon="✉"
              label="Smart Inbox"
              count={emails.length}
            />
            <Nav
              view={view}
              setView={setView}
              value="priority"
              icon="✦"
              label="Priority Center"
            />
            <Nav
              view={view}
              setView={setView}
              value="opportunities"
              icon="↗"
              label="Opportunities"
              count={metrics.opportunities}
            />
            <Nav
              view={view}
              setView={setView}
              value="actions"
              icon="✓"
              label="My Actions"
              count={metrics.pending}
            />
            <Nav
              view={view}
              setView={setView}
              value="deadlines"
              icon="◷"
              label="Deadlines"
            />
            <Nav
              view={view}
              setView={setView}
              value="profile"
              icon="◉"
              label="My Profile"
            />
          </nav>
          <div className="sidebar-help">
            <span>✦</span>
            <div>
              <strong>Personalization on</strong>
              <p>Ranking adapts whenever your interests change.</p>
            </div>
          </div>
        </aside>
        <section className="content" aria-live="polite">
          {demoMode && <DemoModeBanner reset={resetDemo} busy={busy} />}
          <div className="welcome-row">
            <div>
              <p className="eyebrow">
                {view === "priority"
                  ? "Personalized intelligence"
                  : title(view)}
              </p>
              <h2>{headline(view, profile)}</h2>
              <p className="muted">
                {view === "overview"
                  ? "Your private campus signals, organized around what matters now."
                  : "Everything shown is scoped to your signed-in account."}
              </p>
            </div>
            <div className="workspace-actions">
              {!demoMode && (
                <GmailSyncButton
                  connected={session.gmailConnected}
                  syncing={syncing}
                  onSync={syncGmail}
                />
              )}
              <button
                className="refresh"
                disabled={loading || syncing}
                onClick={refresh}
              >
                {loading ? "Refreshing…" : "Refresh data"}
              </button>
            </div>
          </div>
          {error && (
            <Message type="error" close={() => setError("")}>
              {error}
            </Message>
          )}
          {notice && (
            <Message type="success" close={() => setNotice("")}>
              {notice}
            </Message>
          )}
          {view === "overview" && (
            <Overview
              metrics={metrics}
              feed={feed}
              emails={emails}
              actions={actionsByEmail}
              categories={metrics.categories}
              goInbox={goInbox}
              setView={setView}
              openDetail={openDetail}
            />
          )}
          {view === "briefing" && (
            <DailyBriefing
              feed={feed}
              actions={actions}
              deadlines={deadlines}
              openDetail={openDetail}
              setView={setView}
            />
          )}
          {view === "inbox" && (
            <Inbox
              emails={emails}
              feed={feedByEmail}
              actions={actionsByEmail}
              filters={inboxFilters}
              setFilters={setInboxFilters}
              openDetail={openDetail}
              connected={session.gmailConnected}
              syncing={syncing}
              syncGmail={syncGmail}
              demoMode={demoMode}
            />
          )}
          {view === "priority" && (
            <PriorityCenter
              feed={feed}
              actions={actionsByEmail}
              filter={priorityFilter}
              setFilter={setPriorityFilter}
              spotlight={spotlight}
              openDetail={openDetail}
              track={track}
              busy={busy}
            />
          )}
          {view === "opportunities" && (
            <OpportunitiesHub
              feed={feed}
              actions={actionsByEmail}
              openDetail={openDetail}
              track={track}
              busy={busy}
            />
          )}
          {view === "actions" && (
            <Actions
              actions={actions}
              feed={feedByEmail}
              filter={actionFilter}
              setFilter={setActionFilter}
              busy={busy}
              status={updateStatus}
              openDetail={openDetail}
            />
          )}
          {view === "deadlines" && (
            <Deadlines items={deadlines} openDetail={openDetail} />
          )}
          {view === "profile" && (
            <section className="form-card profile-view">
              <p className="eyebrow">Personalization controls</p>
              <h2>Make CampusSignal yours.</h2>
              <p className="muted">
                Saving interests immediately recalculates your private ranked
                feed.
              </p>
              {!demoMode && (
                <GmailConnection
                  connected={session.gmailConnected}
                  syncing={syncing}
                  syncGmail={syncGmail}
                />
              )}
              <ProfileForm
                form={form}
                setForm={setForm}
                submit={save}
                busy={busy || syncing}
              />
            </section>
          )}
        </section>
      </div>
      {currentEmail && (
        <EmailDialog
          email={currentEmail}
          ranked={feedByEmail.get(currentEmail.id)}
          action={actionsByEmail.get(currentEmail.id)}
          state={content[currentEmail.id]}
          candidate={candidates[currentEmail.id]}
          close={() => setDetailId(null)}
          preview={() => candidate(currentEmail.id)}
          track={track}
          busy={busy}
        />
      )}
    </main>
  );
}

function DemoModeBanner({ reset, busy }) {
  return (
    <section className="demo-mode-banner" aria-label="Demo Mode active">
      <div>
        <p className="eyebrow">Demo Mode active</p>
        <strong>
          All three messages and every action here are fictional DEMO DATA.
        </strong>
        <p>
          They are isolated from Gmail, your real inbox, profile, and actions.
          Change interests to see real rule-based ranking changes.
        </p>
      </div>
      <button className="preview" disabled={busy} onClick={reset}>
        Reset demo
      </button>
    </section>
  );
}

function headline(view, profile) {
  return {
    overview: `Good to see you, ${profile.name || "student"}.`,
    briefing: "Your day at a glance.",
    inbox: "Explore your campus inbox.",
    priority: "What matters most to you.",
    opportunities: "Opportunities worth exploring.",
    actions: "Your commitments, in one place.",
    deadlines: "Deadline spotlight.",
    profile: "Your student profile.",
  }[view];
}
function Login({ error, expired, theme, setTheme }) {
  return (
    <main className="login-page">
      <button
        className="theme-login"
        aria-label="Toggle color theme"
        onClick={() => setTheme((old) => (old === "light" ? "dark" : "light"))}
      >
        {theme === "light" ? "◐" : "☀"}
      </button>
      <section className="login-card">
        <div className="brand-mark">CS</div>
        <p className="eyebrow">Student intelligence hub</p>
        <h1>
          {expired
            ? "Your session needs a refresh."
            : "Campus inbox, finally clear."}
        </h1>
        <p>
          {expired
            ? "For your privacy, CampusSignal ended this secure session. Sign in again to restore your personal workspace."
            : "CampusSignal brings your relevant academic, opportunity, and deadline emails to the top."}
        </p>
        <p className="privacy-copy">
          Google handles your password and account recovery. CampusSignal uses
          your authorized account and keeps the workspace private.
        </p>
        {error && <Message type="error">{error}</Message>}
        <a className="google-button" href="/api/v1/auth/google/login">
          Continue with Google <span>→</span>
        </a>
      </section>
    </main>
  );
}
function Onboarding({
  session,
  form,
  setForm,
  save,
  busy,
  logout,
  error,
  theme,
  setTheme,
}) {
  return (
    <main className="onboarding">
      <header className="topbar">
        <Brand />
        <div className="top-actions">
          <button
            className="icon-button"
            onClick={() =>
              setTheme((old) => (old === "light" ? "dark" : "light"))
            }
          >
            {theme === "light" ? "◐" : "☀"}
          </button>
          <button className="text-link" onClick={logout}>
            Log out
          </button>
        </div>
      </header>
      <section className="onboarding-card">
        <p className="eyebrow">Your private workspace</p>
        <h1>Tell us what matters.</h1>
        <p>
          Welcome, <strong>{preferredName(session.name) || "student"}</strong>.
          These choices tune your ranked emails; you can change them anytime.
        </p>
        {error && <Message type="error">{error}</Message>}
        <ProfileForm form={form} setForm={setForm} submit={save} busy={busy} />
      </section>
    </main>
  );
}
function GmailSyncButton({ connected, syncing, onSync }) {
  return connected ? (
    <button className="primary sync-button" disabled={syncing} onClick={onSync}>
      {syncing ? "Syncing Gmail…" : "Sync Gmail"}
    </button>
  ) : (
    <a className="refresh reconnect-button" href="/api/v1/auth/google/login">
      Reconnect Gmail
    </a>
  );
}
function GmailConnection({ connected, syncing, syncGmail }) {
  return (
    <section className="gmail-connection">
      <div>
        <p className="eyebrow">Gmail connection</p>
        <h3>{connected ? "Gmail is connected" : "Gmail needs reconnecting"}</h3>
        <p>
          {connected
            ? "Sync up to 10 recent messages for this account. Stored messages remain private and duplicates are skipped."
            : "Authorize Gmail read-only access for this account before importing messages."}
        </p>
      </div>
      <GmailSyncButton
        connected={connected}
        syncing={syncing}
        onSync={syncGmail}
      />
    </section>
  );
}
function Brand() {
  return (
    <div className="brand">
      <div className="brand-mark">CS</div>
      <div>
        <strong>CampusSignal</strong>
        <span>Student intelligence hub</span>
      </div>
    </div>
  );
}
function Nav({ view, setView, value, label, icon, count }) {
  return (
    <button
      className={view === value ? "active" : ""}
      aria-current={view === value ? "page" : undefined}
      onClick={() => setView(value)}
    >
      <span>{icon}</span>
      <em>{label}</em>
      {count !== undefined && <b>{count}</b>}
    </button>
  );
}
function Message({ type, children, close }) {
  return (
    <div className={`message ${type}`}>
      <span>{children}</span>
      {close && (
        <button aria-label="Dismiss notification" onClick={close}>
          ×
        </button>
      )}
    </div>
  );
}
function Metric({ label, value, note, tone, onClick }) {
  return (
    <button className={`summary-card ${tone || ""}`} onClick={onClick}>
      <span className="summary-label">{label}</span>
      <strong>{value}</strong>
      <small>{note}</small>
      <span className="metric-arrow">↗</span>
    </button>
  );
}
function Empty({ title: emptyTitle, text }) {
  return (
    <div className="empty-state">
      <span>✦</span>
      <h3>{emptyTitle}</h3>
      <p>{text}</p>
    </div>
  );
}
function Overview({
  metrics,
  feed,
  emails,
  actions,
  categories,
  goInbox,
  setView,
  openDetail,
}) {
  return (
    <>
      <div className="summary-grid">
        <Metric
          tone="purple"
          label="Total emails"
          value={metrics.total}
          note="Your stored inbox"
          onClick={() => goInbox({})}
        />
        <Metric
          label="High priority"
          value={metrics.high}
          note="Ranked 60+"
          onClick={() => goInbox({ priority: "high", order: "priority" })}
        />
        <Metric
          label="Opportunities"
          value={metrics.opportunities}
          note="Career & research"
          onClick={() =>
            goInbox({
              category: "",
              priority: "opportunity",
              order: "priority",
            })
          }
        />
        <Metric
          label="Pending actions"
          value={metrics.pending}
          note="Your confirmed work"
          onClick={() => setView("actions")}
        />
      </div>
      <div className="overview-grid">
        <section className="panel priority-preview">
          <div className="section-title">
            <div>
              <p className="eyebrow">Priority Center</p>
              <h2>Top signals for you</h2>
            </div>
            <button className="refresh" onClick={() => setView("priority")}>
              See all
            </button>
          </div>
          {feed.length ? (
            feed.slice(0, 4).map((item, index) => (
              <button
                className="rank-preview"
                key={item.emailId}
                onClick={() => openDetail(item.emailId)}
              >
                <span className="rank-number">{index + 1}</span>
                <span>
                  <strong>{item.emailSubject}</strong>
                  <small>{item.rankingExplanation}</small>
                </span>
                <b>{item.rankingScore}</b>
              </button>
            ))
          ) : (
            <Empty
              title="No ranked emails yet"
              text="Your feed appears after authorized Gmail emails are available."
            />
          )}
        </section>
        <section className="panel chart-panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Live analytics</p>
              <h2>Emails by category</h2>
            </div>
          </div>
          {categories.length ? (
            <CategoryChart
              rows={categories}
              total={emails.length}
              onPick={(category) => goInbox({ category })}
            />
          ) : (
            <Empty
              title="No email data"
              text="Categories appear when stored messages are available."
            />
          )}
        </section>
      </div>
      <section className="panel distribution">
        <div className="section-title">
          <div>
            <p className="eyebrow">Priority distribution</p>
            <h2>Where attention is needed</h2>
          </div>
          <button className="text-link" onClick={() => setView("deadlines")}>
            View deadlines
          </button>
        </div>
        <div className="distribution-bars">
          <Bar
            label="High priority"
            value={metrics.high}
            total={Math.max(feed.length, 1)}
            tone="high"
            onClick={() => goInbox({ priority: "high", order: "priority" })}
          />
          <Bar
            label="Upcoming deadlines"
            value={metrics.deadlines}
            total={Math.max(feed.length, 1)}
            tone="deadline"
            onClick={() => setView("deadlines")}
          />
          <Bar
            label="Tracked actions"
            value={actions.size}
            total={Math.max(feed.length, 1)}
            tone="action"
            onClick={() => setView("actions")}
          />
        </div>
      </section>
    </>
  );
}
function DailyBriefing({ feed, actions, deadlines, openDetail, setView }) {
  const top = feed.filter((item) => item.rankingScore >= 60).slice(0, 3);
  const opportunity = feed.find((item) => isOpportunity(item.emailCategory));
  const due = deadlines.find((item) => item.status !== "COMPLETED");
  const pending = actions.filter((item) => item.effectiveStatus === "PENDING");
  return (
    <section className="briefing">
      <section className="briefing-hero">
        <p className="eyebrow">Personalized daily briefing</p>
        <h2>Your Day at a Glance</h2>
        <p>
          Built only from your stored email, actions, and verified deadline
          data.
        </p>
      </section>
      <div className="briefing-grid">
        {top.length ? (
          <BriefingCard
            title="Top priority"
            note="Highest-ranked signals for your profile"
          >
            {top.map((item) => (
              <button
                key={item.emailId}
                onClick={() => openDetail(item.emailId)}
              >
                <strong>{item.emailSubject}</strong>
                <span>{item.rankingExplanation}</span>
              </button>
            ))}
          </BriefingCard>
        ) : (
          <BriefingCard
            title="Top priority"
            note="No high-priority email is available yet"
          >
            <p>Sync Gmail when you are ready to build your private feed.</p>
          </BriefingCard>
        )}
        {opportunity ? (
          <BriefingCard
            title="Opportunity to review"
            note={opportunityLabel(opportunity.emailCategory)}
          >
            <button onClick={() => openDetail(opportunity.emailId)}>
              <strong>{opportunity.emailSubject}</strong>
              <span>{opportunity.rankingExplanation}</span>
            </button>
          </BriefingCard>
        ) : (
          <BriefingCard
            title="Opportunities"
            note="No supported opportunity category yet"
          >
            <p>
              Career, research, scholarship, and participation items appear here
              when classified.
            </p>
          </BriefingCard>
        )}
        {due ? (
          <BriefingCard title="Verified deadline" note={urgency(due)}>
            <button onClick={() => openDetail(due.emailId)}>
              <strong>{due.emailSubject}</strong>
              <span>{date(due.deadline)}</span>
            </button>
          </BriefingCard>
        ) : (
          <BriefingCard
            title="Verified deadlines"
            note="No supported deadline detected"
          >
            <p>
              CampusSignal does not infer a date from urgency wording alone.
            </p>
          </BriefingCard>
        )}
        {pending.length ? (
          <BriefingCard
            title="Pending actions"
            note={`${pending.length} item${pending.length === 1 ? "" : "s"} to review`}
          >
            <button onClick={() => setView("actions")}>
              <strong>Open My Actions</strong>
              <span>Review, complete, or reopen your tracked commitments.</span>
            </button>
          </BriefingCard>
        ) : (
          <BriefingCard title="Pending actions" note="Nothing is waiting">
            <p>
              Add a supported action from an email when you want to track it.
            </p>
          </BriefingCard>
        )}
      </div>
    </section>
  );
}
function BriefingCard({ title, note, children }) {
  return (
    <article className="briefing-card">
      <p className="eyebrow">{title}</p>
      <h3>{note}</h3>
      <div>{children}</div>
    </article>
  );
}
const opportunityLabel = (category) =>
  ({
    CAREER: "Career opportunity",
    ACADEMIC_RESEARCH: "Research opportunity",
    RECOGNITION_ACHIEVEMENT: "Recognition or scholarship-related",
    CALL_FOR_PARTICIPATION: "Participation opportunity",
    EVENT_ACTIVITY: "Event or workshop opportunity",
  })[category] || "Campus opportunity";
function OpportunitiesHub({ feed, actions, openDetail, track, busy }) {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [order, setOrder] = useState("relevance");
  const opportunities = useMemo(
    () =>
      filterOpportunities(feed, { query, category }).sort((left, right) =>
        order === "recent"
          ? new Date(right.receivedAt) - new Date(left.receivedAt)
          : right.rankingScore - left.rankingScore,
      ),
    [feed, category, order, query],
  );
  const categories = [
    ...new Set(selectOpportunities(feed).map((item) => item.emailCategory)),
  ];
  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Opportunities Hub</p>
          <h2>Explore real student opportunities.</h2>
          <p className="muted">
            Only emails with supported opportunity classifications appear here.
          </p>
        </div>
        <span className="result-count">{opportunities.length} shown</span>
      </div>
      <div className="inbox-controls">
        <label className="search">
          <span>⌕</span>
          <input
            aria-label="Search opportunities"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search opportunities"
          />
        </label>
        <select
          aria-label="Filter opportunity type"
          value={category}
          onChange={(event) => setCategory(event.target.value)}
        >
          <option value="">All opportunity types</option>
          {categories.map((value) => (
            <option key={value} value={value}>
              {opportunityLabel(value)}
            </option>
          ))}
        </select>
        <select
          aria-label="Sort opportunities"
          value={order}
          onChange={(event) => setOrder(event.target.value)}
        >
          <option value="relevance">Most relevant</option>
          <option value="recent">Most recent</option>
        </select>
      </div>
      {opportunities.length ? (
        <div className="priority-list">
          {opportunities.map((item, index) => (
            <article className="priority-card" key={item.emailId}>
              <div className="priority-card-top">
                <span className="rank-number">{index + 1}</span>
                <span className="badge category">
                  {opportunityLabel(item.emailCategory)}
                </span>
                <span className="priority-score">
                  {item.rankingScore}
                  <small>priority</small>
                </span>
              </div>
              <h3>{item.emailSubject}</h3>
              <div className="reason">
                <span>Why it may matter</span>
                <p>{item.rankingExplanation}</p>
              </div>
              <div className="badges">
                {item.deadline ? (
                  <span className={urgencyTone(item)}>
                    {date(item.deadline)}
                  </span>
                ) : (
                  <span className="badge urgency no_deadline">
                    Deadline not identified
                  </span>
                )}
                {item.classificationConfidence === "LOW" && (
                  <span className="badge category">
                    Classification evidence is limited
                  </span>
                )}
              </div>
              <div className="priority-actions">
                <button
                  className="preview"
                  onClick={() => openDetail(item.emailId)}
                >
                  View details
                </button>
                {item.actionIndicated && !actions.get(item.emailId) && (
                  <button
                    className="complete"
                    disabled={busy}
                    onClick={() =>
                      track(item.emailId, {
                        actionType: item.actionType,
                        deadline: item.deadline,
                        actionUrl: item.actionUrl,
                      })
                    }
                  >
                    Add to My Actions
                  </button>
                )}
                {actions.get(item.emailId) && (
                  <span className="tracked-label">Already tracked</span>
                )}
              </div>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          title="No matching opportunities"
          text="Sync or adjust filters to explore your own classified opportunity emails."
        />
      )}
    </section>
  );
}
function CategoryChart({ rows, total, onPick }) {
  const max = Math.max(...rows.map(([, count]) => count), 1);
  return (
    <div className="category-chart">
      {rows.map(([category, count]) => (
        <button
          key={category}
          className="chart-row"
          title={`${title(category)}: ${count} email${count === 1 ? "" : "s"}`}
          onClick={() => onPick(category)}
        >
          <span>{title(category)}</span>
          <i>
            <b style={{ width: `${(count / max) * 100}%` }} />
          </i>
          <strong>
            {count}
            <small> / {total}</small>
          </strong>
        </button>
      ))}
    </div>
  );
}
function Bar({ label, value, total, tone, onClick }) {
  return (
    <button
      className="distribution-bar"
      title={`${label}: ${value}`}
      onClick={onClick}
    >
      <span>{label}</span>
      <i>
        <b
          className={tone}
          style={{ width: `${Math.min(100, (value / total) * 100)}%` }}
        />
      </i>
      <strong>{value}</strong>
    </button>
  );
}
function Inbox({
  emails,
  feed,
  actions,
  filters,
  setFilters,
  openDetail,
  connected,
  syncing,
  syncGmail,
  demoMode,
}) {
  const categories = useMemo(
    () => [...new Set(emails.map((item) => item.category).filter(Boolean))],
    [emails],
  );
  const visible = useMemo(
    () =>
      emails
        .filter((item) => {
          const ranked = feed.get(item.id);
          const search =
            `${item.subject || ""} ${item.sender || ""}`.toLowerCase();
          return (
            search.includes(filters.query.toLowerCase()) &&
            (!filters.category || item.category === filters.category) &&
            (!filters.urgency || ranked?.urgency === filters.urgency) &&
            (!filters.priority ||
              (filters.priority === "high"
                ? (ranked?.rankingScore || 0) >= 60
                : isOpportunity(ranked || item.category)))
          );
        })
        .sort((a, b) =>
          filters.order === "priority"
            ? (feed.get(b.id)?.rankingScore || 0) -
              (feed.get(a.id)?.rankingScore || 0)
            : new Date(b.receivedAt) - new Date(a.receivedAt),
        ),
    [emails, feed, filters],
  );
  const update = (patch) => setFilters((old) => ({ ...old, ...patch }));
  return (
    <section className="inbox-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Smart Inbox</p>
          <h2>Explore every email.</h2>
          <p className="muted">
            Search, filter and open details without leaving your workspace.
          </p>
        </div>
        <div className="workspace-actions">
          <span className="result-count">{visible.length} shown</span>
          {!demoMode && (
            <GmailSyncButton
              connected={connected}
              syncing={syncing}
              onSync={syncGmail}
            />
          )}
        </div>
      </div>
      <div className="inbox-controls">
        <label className="search">
          <span>⌕</span>
          <input
            aria-label="Search subject or sender"
            value={filters.query}
            onChange={(event) => update({ query: event.target.value })}
            placeholder="Search subject or sender"
          />
        </label>
        <select
          aria-label="Filter category"
          value={filters.category}
          onChange={(event) => update({ category: event.target.value })}
        >
          <option value="">All categories</option>
          {categories.map((value) => (
            <option key={value} value={value}>
              {title(value)}
            </option>
          ))}
        </select>
        <select
          aria-label="Filter urgency"
          value={filters.urgency}
          onChange={(event) => update({ urgency: event.target.value })}
        >
          <option value="">Any urgency</option>
          <option value="DUE_TODAY">Due today</option>
          <option value="DUE_SOON">Due soon</option>
          <option value="UPCOMING">Upcoming</option>
        </select>
        <select
          aria-label="Filter priority"
          value={filters.priority}
          onChange={(event) => update({ priority: event.target.value })}
        >
          <option value="">Any priority</option>
          <option value="high">High priority</option>
          <option value="opportunity">Opportunities</option>
        </select>
        <select
          aria-label="Sort emails"
          value={filters.order}
          onChange={(event) => update({ order: event.target.value })}
        >
          <option value="newest">Newest first</option>
          <option value="priority">Most relevant</option>
        </select>
        {Object.values(filters).some(
          (value) => value && value !== "newest",
        ) && (
          <button
            className="text-link"
            onClick={() =>
              setFilters({
                query: "",
                category: "",
                urgency: "",
                priority: "",
                order: "newest",
              })
            }
          >
            Clear filters
          </button>
        )}
      </div>
      <div className="email-list">
        {visible.length ? (
          visible.map((email) => (
            <EmailRow
              key={email.id}
              email={email}
              ranked={feed.get(email.id)}
              action={actions.get(email.id)}
              openDetail={openDetail}
            />
          ))
        ) : (
          <Empty
            title="No matching emails"
            text="Try changing the filters or clearing your search."
          />
        )}
      </div>
    </section>
  );
}
function EmailRow({ email, ranked, action, openDetail }) {
  return (
    <button className="email-row" onClick={() => openDetail(email.id)}>
      <span className="email-row-main">
        <strong>{email.subject}</strong>
        <small>
          {email.sender} · {received(email.receivedAt)}
        </small>
      </span>
      <span className="email-row-tags">
        <span className="badge category">{title(email.category)}</span>
        {ranked && (
          <span className={urgencyTone(ranked)}>{urgency(ranked)}</span>
        )}
        {action && (
          <span className="badge type">
            Action {title(action.effectiveStatus)}
          </span>
        )}
      </span>
      {ranked && (
        <b className="priority-score" title="Personalized priority score">
          {ranked.rankingScore}
        </b>
      )}
      <span className="row-arrow">›</span>
    </button>
  );
}
function PriorityCenter({
  feed,
  actions,
  filter,
  setFilter,
  spotlight,
  openDetail,
  track,
  busy,
}) {
  const categories = [
    ...new Set(feed.map((item) => item.emailCategory).filter(Boolean)),
  ];
  const filters = ["All", "High priority", ...categories.map(title)];
  const visible = feed.filter(
    (item) =>
      filter === "All" ||
      (filter === "High priority"
        ? item.rankingScore >= 60
        : title(item.emailCategory) === filter),
  );
  return (
    <section>
      <div className="priority-hero">
        <div>
          <p className="eyebrow">Priority Center</p>
          <h2>Personalized, explainable ranking.</h2>
          <p>
            Your interests, profile context, category fit, and supported urgency
            shape this order.
          </p>
        </div>
        <span>✦</span>
      </div>
      {spotlight && (
        <section className="spotlight">
          <div>
            <p className="eyebrow">Personalization Spotlight</p>
            <h3>
              Your ranking just adapted to {spotlight.interests.join(", ")}.
            </h3>
            <p>
              {spotlight.moved.length
                ? "These emails changed position because your saved profile changed their relevance."
                : "Your saved interests were applied. Current email scores did not require a visible reordering."}
            </p>
          </div>
          {spotlight.moved.length > 0 && (
            <div className="movement-list">
              {spotlight.moved.map((item) => (
                <button
                  key={item.emailId}
                  onClick={() => openDetail(item.emailId)}
                >
                  <span>
                    {item.before} → {item.after}
                  </span>
                  {item.emailSubject}
                </button>
              ))}
            </div>
          )}
        </section>
      )}
      <div className="filter-chips" aria-label="Priority filters">
        {filters.map((value) => (
          <button
            key={value}
            className={filter === value ? "selected" : ""}
            onClick={() => setFilter(value)}
          >
            {value}
          </button>
        ))}
      </div>
      <div className="priority-list">
        {visible.length ? (
          visible.map((item, index) => (
            <PriorityCard
              key={item.emailId}
              item={item}
              position={index + 1}
              action={actions.get(item.emailId)}
              openDetail={openDetail}
              track={track}
              busy={busy}
            />
          ))
        ) : (
          <Empty
            title="No emails in this priority view"
            text="Select another filter to explore your ranked email feed."
          />
        )}
      </div>
    </section>
  );
}
function PriorityCard({ item, position, action, openDetail, track, busy }) {
  const suggested =
    item.actionIndicated && !action
      ? {
          actionType: item.actionType,
          deadline: item.deadline,
          actionUrl: item.actionUrl,
        }
      : null;
  return (
    <article className="priority-card">
      <div className="priority-card-top">
        <span className="rank-number">{position}</span>
        <span className={urgencyTone(item)}>{urgency(item)}</span>
        <span className="priority-score">
          {item.rankingScore}
          <small>priority</small>
        </span>
      </div>
      <h3>{item.emailSubject}</h3>
      <div className="badges">
        <span className="badge category">{title(item.emailCategory)}</span>
        <span className="badge relevance">{item.relevanceScore} relevance</span>
        {item.actionIndicated && (
          <span className="badge type">{title(item.actionType)} available</span>
        )}
      </div>
      <div className="reason">
        <span>Why it matters</span>
        <p>{item.rankingExplanation}</p>
      </div>
      <div className="priority-actions">
        <button className="preview" onClick={() => openDetail(item.emailId)}>
          View details
        </button>
        {suggested && (
          <button
            className="complete"
            disabled={busy}
            onClick={() => track(item.emailId, suggested)}
          >
            Add to My Actions
          </button>
        )}
        {action && (
          <span className="tracked-label">
            Tracked · {title(action.effectiveStatus)}
          </span>
        )}
      </div>
    </article>
  );
}
function Actions({
  actions,
  feed,
  filter,
  setFilter,
  busy,
  status,
  openDetail,
}) {
  const visible = actions.filter(
    (action) => filter === "ALL" || action.effectiveStatus === filter,
  );
  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Action Center</p>
          <h2>Turn email into progress.</h2>
          <p className="muted">
            Only actions you explicitly add appear here; ranking stays
            independent.
          </p>
        </div>
      </div>
      <div className="filter-chips">
        {[
          ["PENDING", "Pending"],
          ["COMPLETED", "Completed"],
          ["ALL", "All actions"],
        ].map(([value, label]) => (
          <button
            key={value}
            className={filter === value ? "selected" : ""}
            onClick={() => setFilter(value)}
          >
            {label}
          </button>
        ))}
      </div>
      {visible.length ? (
        <div className="action-grid">
          {visible.map((action) => (
            <article className="action-item" key={action.id}>
              <div>
                <span className={urgencyTone(action)}>{urgency(action)}</span>
                <span className="action-status">
                  {title(action.effectiveStatus)}
                </span>
              </div>
              <h3>{action.emailSubject}</h3>
              <p>
                {title(action.actionType)} · {date(action.deadline)}
              </p>
              <small>
                {feed.get(action.emailId)?.rankingExplanation ||
                  "Created from your email."}
              </small>
              <div className="priority-actions">
                <button
                  className="preview"
                  onClick={() => openDetail(action.emailId)}
                >
                  Open linked email
                </button>
                {action.effectiveStatus === "PENDING" && (
                  <>
                    <button
                      className="complete"
                      disabled={busy}
                      onClick={() => status(action.id, "COMPLETED")}
                    >
                      Mark complete
                    </button>
                    <button
                      className="dismiss"
                      disabled={busy}
                      onClick={() => status(action.id, "NOT_INTERESTED")}
                    >
                      Not interested
                    </button>
                  </>
                )}
                {action.effectiveStatus === "COMPLETED" && (
                  <button
                    className="preview"
                    disabled={busy}
                    onClick={() => status(action.id, "PENDING")}
                  >
                    Reopen action
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          title={
            filter === "COMPLETED"
              ? "No completed actions yet"
              : "No actions here yet"
          }
          text="Open a ranked email and add an available action when you want to track it."
        />
      )}
    </section>
  );
}
function Deadlines({ items, openDetail }) {
  const groups = {
    "Due soon": items.filter(
      (item) =>
        ["DUE_TODAY", "DUE_SOON", "OVERDUE"].includes(item.urgency) &&
        item.status !== "COMPLETED",
    ),
    Upcoming: items.filter(
      (item) => item.urgency === "UPCOMING" && item.status !== "COMPLETED",
    ),
    Completed: items.filter((item) => item.status === "COMPLETED"),
  };
  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Deadline Spotlight</p>
          <h2>Dates with context.</h2>
          <p className="muted">
            Only extracted, supported deadlines are shown.
          </p>
        </div>
      </div>
      {items.length ? (
        <div className="deadline-groups">
          {Object.entries(groups).map(([group, rows]) =>
            rows.length ? (
              <section className="deadline-group" key={group}>
                <h3>
                  {group} <span>{rows.length}</span>
                </h3>
                {rows.map((item) => (
                  <button
                    key={item.emailId}
                    className="deadline-row"
                    onClick={() => openDetail(item.emailId)}
                  >
                    <time>{date(item.deadline)}</time>
                    <span>
                      <strong>{item.emailSubject}</strong>
                      <small>
                        {item.kind} · {title(item.status)}
                      </small>
                    </span>
                    <b className={urgencyTone(item)}>{urgency(item)}</b>
                  </button>
                ))}
              </section>
            ) : null,
          )}
        </div>
      ) : (
        <Empty
          title="No detected deadlines"
          text="CampusSignal only displays a deadline when it was extracted from an email or confirmed action."
        />
      )}
    </section>
  );
}
function EmailDialog({
  email,
  ranked,
  action,
  state,
  candidate,
  close,
  preview,
  track,
  busy,
}) {
  const suggested =
    candidate?.candidate ||
    (ranked?.actionIndicated && !action
      ? {
          actionType: ranked.actionType,
          deadline: ranked.deadline,
          actionUrl: ranked.actionUrl,
        }
      : null);
  return (
    <div
      className="dialog-backdrop email-reader-backdrop"
      role="presentation"
      onMouseDown={close}
    >
      <section
        className="email-dialog email-reader-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="email-detail-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button
          className="dialog-close"
          aria-label="Close email details"
          onClick={close}
        >
          ×
        </button>
        <header className="email-reader-header">
          <p className="eyebrow">Original email</p>
          <h2 id="email-detail-title">{email.subject}</h2>
          <div className="email-addresses">
            <div>
              <strong>{email.sender || "Unknown sender"}</strong>
              <span>{email.sender || "Sender address unavailable"}</span>
            </div>
            <time dateTime={email.receivedAt || undefined}>
              {received(email.receivedAt)}
            </time>
          </div>
          <p className="recipient-note">
            Recipient metadata was not retained with this imported message.
          </p>
          <p className="email-sender">
            {email.sender} · {received(email.receivedAt)}
          </p>
          <div className="badges">
            <span className="badge category">{title(email.category)}</span>
            {ranked && (
              <span className={urgencyTone(ranked)}>{urgency(ranked)}</span>
            )}
            {ranked && (
              <span className="badge relevance">
                {ranked.rankingScore} priority
              </span>
            )}
          </div>
        </header>
        {ranked && (
          <div className="reason">
            <span>Why it matters to you</span>
            <p>{ranked.rankingExplanation}</p>
          </div>
        )}
        {state?.loading && (
          <div className="loading-state">
            <span className="spinner" />
            Loading email preview…
          </div>
        )}
        {state?.error && <p className="candidate-error">{state.error}</p>}
        {state?.message && (
          <details className="email-content email-intelligence" open>
            <summary>
              CampusSignal intelligence
              <span>Personalized context from this stored message</span>
            </summary>
            <div className="email-content-heading">
              <strong>Quick summary</strong>
              <span>Derived from this stored email</span>
            </div>
            <ul className="email-summary">
              {summarizeEmail(state.message, suggested).map((item) => (
                <li key={`${item.label}-${item.text}`}>
                  <strong>{item.label}:</strong> {item.text}
                </li>
              ))}
            </ul>
            <details className="full-message" open>
              <summary>Original email</summary>
              <EmailPlainText body={state.message.body} />
              {state.message.truncated && (
                <span className="truncation-note">
                  Preview limited to 12,000 characters.
                </span>
              )}
            </details>
          </details>
        )}
        <div className="dialog-actions">
          {!action && !candidate?.checked && (
            <button
              className="preview"
              disabled={candidate?.loading}
              onClick={preview}
            >
              {candidate?.loading ? "Checking…" : "Check for action"}
            </button>
          )}
          {suggested && !action && (
            <button
              className="complete"
              disabled={busy}
              onClick={() => track(email.id, suggested)}
            >
              Add to My Actions
            </button>
          )}
          {action && (
            <span className="tracked-label">
              Already tracked · {title(action.effectiveStatus)}
            </span>
          )}
        </div>
        {candidate?.checked && !suggested && (
          <p className="muted">
            No action was confidently detected for this email.
          </p>
        )}
        {candidate?.error && (
          <p className="candidate-error">{candidate.error}</p>
        )}
      </section>
    </div>
  );
}

function EmailPlainText({ body }) {
  const { message, footer, quoted } = splitEmailBody(body);
  const renderText = (value) =>
    String(value || "")
      .split(/(https?:\/\/[^\s<>'"`]+)/g)
      .map((part, index) => {
        const url = safeHttpUrl(part);
        return url ? (
          <a
            key={`${url}-${index}`}
            href={url}
            target="_blank"
            rel="noopener noreferrer"
          >
            {part}
          </a>
        ) : (
          part
        );
      });
  return (
    <div className="email-original-body">
      <div className="email-plain-text">
        {renderText(
          message || body || "No message content was stored for this email.",
        )}
      </div>
      {footer && (
        <details className="email-collapsed">
          <summary>Promotional footer</summary>
          <div className="email-plain-text">{renderText(footer)}</div>
        </details>
      )}
      {quoted && (
        <details className="email-collapsed">
          <summary>Quoted previous replies</summary>
          <div className="email-plain-text">{renderText(quoted)}</div>
        </details>
      )}
    </div>
  );
}

function ProfileForm({ form, setForm, submit, busy }) {
  const [custom, setCustom] = useState("");
  const normalized = (value) => value.trim().toLowerCase();
  const known = (value) =>
    interestChoices.some((item) => normalized(item) === normalized(value));
  const selected = (item) =>
    form.interests.some((value) => normalized(value) === normalized(item));
  const update = (event) =>
    setForm((old) => ({ ...old, [event.target.name]: event.target.value }));
  const toggle = (item) =>
    setForm((old) => ({
      ...old,
      interests: selected(item)
        ? old.interests.filter(
            (value) => normalized(value) !== normalized(item),
          )
        : [...old.interests, item],
    }));
  const add = () => {
    const value = custom.trim();
    if (value) {
      setForm((old) =>
        old.interests.some((item) => normalized(item) === normalized(value))
          ? old
          : { ...old, interests: [...old.interests, value] },
      );
      setCustom("");
    }
  };
  return (
    <form className="profile-form" onSubmit={submit}>
      <div className="form-grid">
        <label>
          Name
          <input name="name" value={form.name} onChange={update} required />
        </label>
        <label>
          College / University
          <input
            name="college"
            value={form.college}
            onChange={update}
            required
          />
        </label>
        <label>
          Department
          <input
            name="department"
            value={form.department}
            onChange={update}
            required
          />
        </label>
        <label>
          Programme
          <input
            name="programme"
            value={form.programme}
            onChange={update}
            required
          />
        </label>
        <label>
          Current year
          <input
            type="number"
            name="year"
            min="1"
            value={form.year}
            onChange={update}
            required
          />
        </label>
        <label>
          Graduation year
          <input
            type="number"
            name="graduationYear"
            min="2000"
            value={form.graduationYear}
            onChange={update}
            required
          />
        </label>
        <label>
          Semester (optional)
          <input
            type="number"
            name="semester"
            min="1"
            value={form.semester || ""}
            onChange={update}
          />
        </label>
      </div>
      <fieldset>
        <legend>
          Your interests <small>Select at least one</small>
        </legend>
        <div className="interest-picker">
          {interestChoices.map((item) => (
            <button
              type="button"
              className={selected(item) ? "selected" : ""}
              key={item}
              onClick={() => toggle(item)}
              aria-pressed={selected(item)}
            >
              {selected(item) ? "✓ " : "+ "}
              {item}
            </button>
          ))}
        </div>
      </fieldset>
      <div className="custom-interest">
        <label>
          Add a specific interest
          <input
            value={custom}
            onChange={(event) => setCustom(event.target.value)}
            placeholder="e.g. Robotics"
          />
        </label>
        <button type="button" className="preview" onClick={add}>
          Add
        </button>
      </div>
      {form.interests.filter((item) => !known(item)).length > 0 && (
        <div className="interest-row">
          {form.interests
            .filter((item) => !known(item))
            .map((item) => (
              <button
                type="button"
                className="badge category"
                key={item}
                onClick={() => toggle(item)}
              >
                {item} ×
              </button>
            ))}
        </div>
      )}
      <button className="primary" disabled={busy}>
        {busy ? "Saving and refreshing…" : "Save profile & refresh ranking"}
      </button>
    </form>
  );
}
