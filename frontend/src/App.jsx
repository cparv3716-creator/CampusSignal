import { useEffect, useMemo, useState } from 'react'
import StudentApp from './StudentApp.jsx'
import { request } from './api.js'
import { summarizeEmail } from './emailSummary.js'
import { syncHeaders, syncResultMessage } from './gmailSync.js'
import {
  clearSelectedProfileId,
  profilePayload,
  profileToForm,
  readSelectedProfileId,
  saveSelectedProfileId,
} from './profileForm.js'

const initialProfile = { email: '', department: '', programme: '', year: '', semester: '', interests: '' }

function formatDate(value) {
  if (!value) return 'No deadline'
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatReceived(value) {
  return new Date(value).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}

function readable(value) {
  return value?.replaceAll('_', ' ').toLowerCase() ?? ''
}

function urgencyLabel(item) {
  if (item.urgency === 'DUE_TODAY') return 'Due today'
  if (item.urgency === 'DUE_SOON') return `${item.daysRemaining} day${item.daysRemaining === 1 ? '' : 's'} left`
  if (item.urgency === 'UPCOMING') return `${item.daysRemaining} days left`
  return readable(item.urgency)
}

function App() {
  return <StudentApp />
  const [view, setView] = useState('overview')
  const [profileForm, setProfileForm] = useState(initialProfile)
  const [lookupMode, setLookupMode] = useState('id')
  const [lookupValue, setLookupValue] = useState('')
  const [profile, setProfile] = useState(null)
  const [feed, setFeed] = useState(null)
  const [emails, setEmails] = useState(null)
  const [actions, setActions] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingInbox, setLoadingInbox] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)
  const [editingProfile, setEditingProfile] = useState(false)
  const [updatingAction, setUpdatingAction] = useState(null)
  const [creatingAction, setCreatingAction] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [candidates, setCandidates] = useState({})
  const [content, setContent] = useState({})
  const [operatorCsrf, setOperatorCsrf] = useState('')
  const [syncingGmail, setSyncingGmail] = useState(false)

  const loadInbox = async () => {
    setLoadingInbox(true)
    setError('')
    try {
      const [emailRows, actionRows] = await Promise.all([
        request('/api/v1/emails'),
        request('/api/v1/actions'),
      ])
      setEmails(emailRows)
      setActions(actionRows)
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoadingInbox(false)
    }
  }

  useEffect(() => { loadInbox() }, [])

  useEffect(() => {
    const savedProfileId = readSelectedProfileId()
    if (!savedProfileId) return
    request(`/api/v1/profiles/${encodeURIComponent(savedProfileId)}`)
      .then((savedProfile) => loadFeed(savedProfile.id, savedProfile))
      .catch(() => clearSelectedProfileId())
  }, [])

  useEffect(() => {
    const marker = '#campussignal-operator-code='
    const establishSession = window.location.hash.startsWith(marker)
      ? (() => {
        const code = decodeURIComponent(window.location.hash.slice(marker.length))
        window.history.replaceState({}, document.title, window.location.pathname + window.location.search)
        return request('/api/v1/gmail/operator/session', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ code }),
        })
      })()
      : request('/api/v1/gmail/operator/session/restore', { method: 'POST' })
    establishSession.then((session) => {
      setOperatorCsrf(session.csrfToken)
      setNotice('Local Gmail sync is authorized for this browser session.')
    }).catch((reason) => {
      if (reason.status !== 401) setError(reason.message)
    })
  }, [])

  const loadFeed = async (profileId, profileToKeep = profile) => {
    setLoading(true)
    setError('')
    setProfile(profileToKeep)
    saveSelectedProfileId(profileId)
    try {
      setFeed(await request(`/api/v1/profiles/${profileId}/email-feed`))
    } catch (reason) {
      setFeed(null)
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const createProfile = async (event) => {
    event.preventDefault()
    setSavingProfile(true)
    setError('')
    setNotice('')
    const payload = profilePayload(profileForm)
    try {
      const created = await request('/api/v1/profiles', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      setProfileForm(initialProfile)
      setLookupValue(String(created.id))
      setLookupMode('id')
      setNotice('Profile created. Your personalized ranked email feed is ready.')
      setView('overview')
      await loadFeed(created.id, created)
    } catch (reason) {
      if (reason.status === 409) {
        setLookupMode('email')
        setLookupValue(profileForm.email)
        setNotice('A profile already exists for that email. Find the existing profile to open its action feed.')
        setError('')
      } else setError(reason.message)
    } finally {
      setSavingProfile(false)
    }

  }

  const editProfile = () => {
    if (!profile) return
    setProfileForm(profileToForm(profile))
    setEditingProfile(true)
    setError('')
    setNotice('')
  }

  const updateProfile = async (event) => {
    event.preventDefault()
    if (!profile) return
    setSavingProfile(true)
    setError('')
    setNotice('')
    try {
      const updated = await request(`/api/v1/profiles/${profile.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(profilePayload(profileForm)),
      })
      setEditingProfile(false)
      setProfileForm(initialProfile)
      setNotice('Profile updated. Your personalized ranked email feed has been refreshed.')
      await loadFeed(updated.id, updated)
    } catch (reason) {
      if (reason.status === 409) setError('That email is already used by another profile. Keep the existing profile email or switch profiles.')
      else setError(reason.message)
    } finally {
      setSavingProfile(false)
    }
  }

  const findProfile = async (event) => {
    event.preventDefault()
    if (!lookupValue.trim()) return
    setLoading(true)
    setError('')
    setNotice('')
    try {
      const path = lookupMode === 'id'
        ? `/api/v1/profiles/${encodeURIComponent(lookupValue.trim())}`
        : `/api/v1/profiles/by-email/${encodeURIComponent(lookupValue.trim())}`
      const found = await request(path)
      setCandidates({})
      await loadFeed(found.id, found)
    } catch (reason) {
      setProfile(null)
      setFeed(null)
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const updateStatus = async (actionId, status) => {
    if (!profile) return
    setUpdatingAction(actionId)
    setError('')
    try {
      await request(`/api/v1/actions/${actionId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
      setNotice(status === 'COMPLETED' ? 'Action marked complete.' : 'Action removed from your feed.')
      await Promise.all([loadFeed(profile.id), loadInbox()])
    } catch (reason) {
      setError(reason.message)
    } finally {
      setUpdatingAction(null)
    }
  }

  const previewCandidate = async (emailId) => {
    setCandidates((current) => ({ ...current, [emailId]: { loading: true } }))
    try {
      const candidate = await request(`/api/v1/emails/${emailId}/action-candidate`)
      setCandidates((current) => ({ ...current, [emailId]: { candidate } }))
    } catch (reason) {
      setCandidates((current) => ({ ...current, [emailId]: { error: reason.message } }))
    }
  }

  const viewContent = async (emailId) => {
    setContent((current) => ({ ...current, [emailId]: { loading: true } }))
    try {
      const message = await request(`/api/v1/emails/${emailId}/content`)
      setContent((current) => ({ ...current, [emailId]: { message } }))
    } catch (reason) {
      setContent((current) => ({ ...current, [emailId]: { error: reason.message } }))
    }
  }

  const syncGmail = async () => {
    if (!operatorCsrf) {
      setError('Run the local Gmail bootstrap command before syncing from the dashboard.')
      return
    }
    setSyncingGmail(true)
    setError('')
    setNotice('')
    try {
      const result = await request('/api/v1/gmail/sync', {
        method: 'POST',
        headers: syncHeaders(operatorCsrf),
        body: JSON.stringify({}),
      })
      setNotice(syncResultMessage(result))
      await Promise.all([
        loadInbox(),
        profile ? loadFeed(profile.id) : Promise.resolve(),
      ])
    } catch (reason) {
      setError(reason.status === 409
        ? 'A Gmail sync is already running. Please wait and try again.'
        : reason.status === 503
          ? 'Gmail sync is unavailable. Check local authorization and connectivity.'
          : reason.message)
    } finally {
      setSyncingGmail(false)
    }
  }

  const createAction = async (email, candidate) => {
    if (!candidate || actions.some((action) => action.emailId === email.id)) return
    if (!window.confirm(`Add "${candidate.actionType.toLowerCase()}" as a tracked action for this email?`)) return
    setCreatingAction(email.id)
    setError('')
    try {
      await request(`/api/v1/emails/${email.id}/actions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(candidate),
      })
      setNotice('Action confirmed and added to the personalized feed.')
      await Promise.all([loadInbox(), profile ? loadFeed(profile.id) : Promise.resolve()])
    } catch (reason) {
      setError(reason.status === 409
        ? 'This email already has a tracked action. Refreshing the Inbox to show its current status.'
        : reason.message)
      await loadInbox()
    } finally {
      setCreatingAction(null)
    }
  }

  const existingActionByEmail = useMemo(
    () => new Map(actions.map((action) => [action.emailId, action])),
    [actions],
  )
  const realEmails = emails?.filter((email) => !email.sender.toLowerCase().endsWith('@demo.example.com')) ?? null
  const urgentCount = feed?.filter((item) => item.urgency === 'DUE_TODAY' || item.urgency === 'DUE_SOON').length ?? 0
  const sourceLabel = realEmails ? 'Real Gmail messages · temporary sender heuristic' : 'Stored mailbox data'
  const emptyFeedMessage = realEmails?.length
    ? 'Emails are stored, but no confirmed pending actions are currently available.'
    : 'No persisted emails are available yet. Retrieve mailbox messages before reviewing actions.'

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand"><div className="brand-mark">CS</div><div><strong>CampusSignal</strong><span>Student intelligence hub</span></div></div>
        <div className="topbar-status"><span className="status-dot" />{sourceLabel}<span className="avatar">{profile?.email?.[0]?.toUpperCase() ?? 'S'}</span></div>
      </header>
      <div className="dashboard-layout">
        <aside className="sidebar">
          <div className="sidebar-intro"><p className="sidebar-kicker">Workspace</p><h1>Stay ahead of what matters.</h1><p>Personalized campus actions, ranked around your priorities.</p></div>
          <nav className="side-nav" aria-label="Dashboard sections">
            <button className={view === 'overview' ? 'active' : ''} onClick={() => setView('overview')}><span>⌂</span>Overview</button>
            <button className={view === 'inbox' ? 'active' : ''} onClick={() => { setView('inbox'); loadInbox() }}><span>▣</span>Inbox <b>{realEmails?.length ?? '—'}</b></button>
            <button className={view === 'profile' ? 'active' : ''} onClick={() => setView('profile')}><span>◎</span>Student profile</button>
            <button className={view === 'create' ? 'active' : ''} onClick={() => setView('create')}><span>＋</span>Create profile</button>
          </nav>
          <div className="sidebar-help"><span>✦</span><div><strong>Smart ranking</strong><p>Every action is ordered by relevance and urgency.</p></div></div>
        </aside>
        <section className="content" aria-label="CampusSignal dashboard">
          <div className="welcome-row"><div><p className="eyebrow">{viewLabel(view)}</p><h2>{view === 'inbox' ? 'Review your campus inbox.' : `Good to see you${profile ? `, ${profile.email.split('@')[0]}` : ''}.`}</h2><p className="muted">{view === 'inbox' ? 'Preview and confirm actions from persisted email data.' : 'Here is what is happening across your campus.'}</p></div><div className="date-chip">● Today · {new Date().toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</div></div>
          {error && <div className="message error"><strong>Couldn’t complete that request.</strong><span>{error}</span></div>}
          {notice && <div className="message success"><span>{notice}</span>{notice.includes('profile already exists') && <button className="notice-link" onClick={findProfile}>Find existing profile</button>}</div>}
          {view === 'inbox' ? <Inbox emails={realEmails} actions={existingActionByEmail} candidates={candidates} content={content} loading={loadingInbox} creatingAction={creatingAction} syncingGmail={syncingGmail} onRefresh={loadInbox} onSync={syncGmail} onPreview={previewCandidate} onViewContent={viewContent} onCreate={createAction} /> : view === 'create' ? <CreateProfile form={profileForm} setForm={setProfileForm} saving={savingProfile} onSubmit={createProfile} /> : view === 'profile' ? <ProfileView profile={profile} editing={editingProfile} form={profileForm} setForm={setProfileForm} lookupMode={lookupMode} setLookupMode={setLookupMode} lookupValue={lookupValue} setLookupValue={setLookupValue} loading={loading} saving={savingProfile} onSubmit={findProfile} onEdit={editProfile} onSave={updateProfile} onCancel={() => { setEditingProfile(false); setProfileForm(initialProfile) }} /> : <Overview feed={feed} loading={loading} profile={profile} emails={realEmails} urgentCount={urgentCount} emptyMessage={emptyFeedMessage} actions={existingActionByEmail} content={content} onViewContent={viewContent} onTrack={createAction} onRefresh={() => profile && loadFeed(profile.id)} onInbox={() => setView('inbox')} />}
        </section>
      </div>
    </main>
  )
}

function viewLabel(view) { return { overview: 'Overview', inbox: 'Inbox', profile: 'Student profile', create: 'Get started' }[view] }

function Overview({ feed, loading, profile, emails, urgentCount, emptyMessage, actions, content, onViewContent, onTrack, onRefresh, onInbox }) {
  return <><div className="summary-grid"><div className="summary-card purple"><span className="summary-icon">✦</span><div><span className="summary-label">Ranked emails</span><strong>{feed?.length ?? '—'}</strong><small>Personalized for you</small></div></div><div className="summary-card"><span className="summary-icon urgent-icon">!</span><div><span className="summary-label">Needs attention</span><strong>{feed ? urgentCount : '—'}</strong><small>Due today or soon</small></div></div><div className="summary-card"><span className="summary-icon green-icon">▣</span><div><span className="summary-label">Inbox</span><strong>{emails?.length ?? '—'}</strong><small>Persisted emails</small></div></div></div><div className="main-grid"><section id="feed" className="feed-column"><div className="section-title"><div><p className="eyebrow">Priority queue</p><h2>For You — Ranked Emails</h2></div><button className="refresh" onClick={onRefresh} disabled={!profile || loading}>↻ <span>{loading ? 'Refreshing…' : 'Refresh'}</span></button></div>{loading && <div className="loading-state"><span className="spinner" />Loading personalized emails…</div>}{profile && !loading && feed?.length === 0 && <div className="empty-state"><div className="empty-icon">▣</div><h3>No ranked emails available</h3><p>{emptyMessage}</p><button className="primary" onClick={onInbox}>Open Inbox review</button></div>}{!profile && !loading && <div className="empty-state"><div className="empty-icon">✦</div><h3>Choose a student profile</h3><p>Find an existing profile to see its personalized ranked email feed.</p></div>}{!loading && feed?.map((item, index) => <RankedEmailCard key={item.emailId} rank={index + 1} item={item} tracked={actions.get(item.emailId)} messageState={content[item.emailId]} onViewContent={onViewContent} onTrack={onTrack} />)}</section><ProfileCard profile={profile} /></div></>
}

function ProfileCard({ profile }) {
  return <section className="profile-card">{profile ? <><div className="profile-heading"><div className="profile-avatar">{profile.email[0].toUpperCase()}</div><div><p className="eyebrow">Student profile</p><h3>{profile.email}</h3></div></div><div className="profile-facts"><span><small>Programme</small><strong>{profile.programme}</strong></span><span><small>Department</small><strong>{profile.department}</strong></span><span><small>Year</small><strong>{profile.year}</strong></span></div><div className="interest-row">{profile.interests.map((interest) => <span key={interest}>{interest}</span>)}</div></> : <div className="profile-empty"><div className="profile-avatar">S</div><h3>No profile selected</h3><p>Find a profile to personalize this dashboard.</p></div>}</section>
}

function RankedEmailCard({ rank, item, tracked, messageState, onViewContent, onTrack }) {
  const candidate = item.actionIndicated && !tracked
    ? { actionType: item.actionType, deadline: item.deadline, actionUrl: item.actionUrl }
    : null
  return <article className="action-card"><div className="card-topline"><span className="score">#{rank}</span><span className={`badge urgency ${item.urgency.toLowerCase()}`}>{urgencyLabel(item)}</span><span className="score">{item.rankingScore} priority</span></div><h3>{item.emailSubject}</h3><div className="badges"><span className="badge category">{readable(item.emailCategory)}</span><span className={`badge relevance ${item.relevanceScore >= 50 ? 'high' : item.relevanceScore >= 30 ? 'medium' : 'low'}`}>{item.relevanceScore} relevance</span>{item.actionIndicated && <span className="badge type">{readable(item.actionType)} indicated</span>}</div><dl className="details"><div><dt>Deadline</dt><dd>{formatDate(item.deadline)}</dd></div><div><dt>Why this email matters</dt><dd>{item.rankingExplanation}</dd></div></dl><div className="actions-row"><button className="preview" onClick={() => onViewContent(item.emailId)} disabled={messageState?.loading}>{messageState?.loading ? 'Loading message…' : 'View message'}</button>{candidate && <button className="complete" onClick={() => onTrack({ id: item.emailId }, candidate)}>Track action</button>}{tracked && <span className="muted">Tracked separately: {readable(tracked.status)}</span>}</div>{messageState?.error && <div className="candidate error">{messageState.error}</div>}{messageState?.message && <EmailContent message={messageState.message} />}</article>
}

function Inbox({ emails, actions, candidates, content, loading, creatingAction, syncingGmail, onRefresh, onSync, onPreview, onViewContent, onCreate }) {
  return <section className="inbox-panel"><div className="section-title"><div><p className="eyebrow">Persisted email review</p><h2>Inbox</h2><p className="heuristic-note">Showing non-demo senders only. Temporary @demo.example.com heuristic; provider source is not yet persisted.</p></div><div className="inbox-controls"><button className="refresh" onClick={onSync} disabled={syncingGmail}>{syncingGmail ? 'Syncing Gmail…' : 'Sync Gmail'}</button><button className="refresh" onClick={onRefresh} disabled={loading}>↻ <span>{loading ? 'Refreshing…' : 'Refresh'}</span></button></div></div>{loading && <div className="loading-state"><span className="spinner" />Loading persisted emails…</div>}{!loading && emails?.length === 0 && <div className="empty-state"><div className="empty-icon">▣</div><h3>No real Gmail messages found</h3><p>Retrieve mailbox messages locally, then refresh this review.</p></div>}{!loading && emails?.map((email) => <EmailCard key={email.id} email={email} existingAction={actions.get(email.id)} state={candidates[email.id]} messageState={content[email.id]} creating={creatingAction === email.id} onPreview={onPreview} onViewContent={onViewContent} onCreate={onCreate} />)}</section>
}

function EmailCard({ email, existingAction, state, messageState, creating, onPreview, onViewContent, onCreate }) {
  return <article className="email-card"><div className="email-heading"><div><h3>{email.subject}</h3><p>{email.sender}</p></div><span className="badge category">{readable(email.category)}</span></div><div className="email-meta"><span>Received {formatReceived(email.receivedAt)}</span><span>Classification {Math.round(email.classificationConfidence * 100)}%</span></div><div className="candidate-area"><button className="preview" onClick={() => onViewContent(email.id)} disabled={messageState?.loading}>{messageState?.loading ? 'Loading message…' : 'View message'}</button>{messageState?.error && <span className="candidate-error">{messageState.error}</span>}{messageState?.message &&   <EmailContent message={messageState.message} candidate={state?.candidate} />}</div>{existingAction ? <div className="review-status"><strong>Action already tracked</strong><span>{readable(existingAction.status)} · {readable(existingAction.actionType)}</span></div> : <div className="candidate-area">{!state && <button className="preview" onClick={() => onPreview(email.id)}>Preview detected action</button>}{state?.loading && <span className="muted">Checking for a candidate…</span>}{state?.error && <span className="candidate-error">{state.error}</span>}{state?.candidate && <CandidatePreview candidate={state.candidate} creating={creating} onCreate={() => onCreate(email, state.candidate)} />}{state && !state.loading && !state.error && !state.candidate && <span className="muted">No action candidate was detected.</span>}</div>}</article>
}

function EmailContent({ message, candidate }) {
  const summary = summarizeEmail(message, candidate)
  return <div className="email-content"><div className="email-content-heading"><strong>Quick summary</strong><span>Deterministic summary from stored message content</span></div><ul className="email-summary">{summary.map((item) => <li key={`${item.label}-${item.text}`}><strong>{item.label}:</strong> {item.text}</li>)}</ul><details className="full-message"><summary>Read full message</summary><dl className="email-content-meta"><div><dt>Subject</dt><dd>{message.subject}</dd></div><div><dt>From</dt><dd>{message.sender}</dd></div><div><dt>Received</dt><dd>{formatReceived(message.receivedAt)}</dd></div></dl><pre>{message.body}</pre>{message.truncated && <span className="truncation-note">Full message preview limited to 12,000 characters.</span>}</details></div>
}

function CandidatePreview({ candidate, creating, onCreate }) {
  return <div className="candidate"><strong>Detected {readable(candidate.actionType)}</strong><span>{candidate.deadline ? formatDate(candidate.deadline) : 'No deadline'}</span>{candidate.actionUrl && <a href={candidate.actionUrl} target="_blank" rel="noreferrer">Open action link ↗</a>}<button className="complete" onClick={onCreate} disabled={creating}>{creating ? 'Adding…' : 'Confirm action'}</button></div>
}

function ActionCard({ item, candidate, updating, onPreview, onUpdate }) {
  return <article className="action-card"><div className="card-topline"><span className={`badge urgency ${item.urgency.toLowerCase()}`}>{urgencyLabel(item)}</span><span className="score">#{item.rankingScore} priority</span></div><h3>{item.emailSubject}</h3><div className="badges"><span className="badge category">{readable(item.emailCategory)}</span><span className={`badge relevance ${item.relevance.toLowerCase()}`}>{readable(item.relevance)} relevance</span><span className="badge type">{readable(item.actionType)}</span></div><dl className="details"><div><dt>Deadline</dt><dd>{formatDate(item.deadline)}</dd></div><div><dt>Why it appears</dt><dd>{item.rankingExplanation}</dd></div></dl><div className="actions-row">{item.actionUrl && <a className="text-link" href={item.actionUrl} target="_blank" rel="noreferrer">Open action link ↗</a>}<button className="preview" onClick={() => onPreview(item.emailId)} disabled={candidate?.loading}>{candidate?.loading ? 'Checking…' : 'Preview detected action'}</button></div>{candidate?.error && <div className="candidate error">{candidate.error}</div>}<div className="card-controls"><button className="complete" onClick={() => onUpdate(item.id, 'COMPLETED')} disabled={updating}>{updating ? 'Saving…' : 'Mark complete'}</button><button className="dismiss" onClick={() => onUpdate(item.id, 'NOT_INTERESTED')} disabled={updating}>Not interested</button></div></article>
}

function ProfileView({ profile, editing, form, setForm, lookupMode, setLookupMode, lookupValue, setLookupValue, loading, saving, onSubmit, onEdit, onSave, onCancel }) {
  const update = (key, value) => setForm({ ...form, [key]: value })
  return <section className="form-card profile-view"><div className="section-title"><div><p className="eyebrow">Returning student</p><h2>Find a profile</h2></div></div><form onSubmit={onSubmit}><div className="toggle" role="group" aria-label="Profile lookup method"><button type="button" className={lookupMode === 'id' ? 'selected' : ''} onClick={() => setLookupMode('id')}>Profile ID</button><button type="button" className={lookupMode === 'email' ? 'selected' : ''} onClick={() => setLookupMode('email')}>Email</button></div><label>{lookupMode === 'id' ? 'Profile ID' : 'Student email'}<input required value={lookupValue} onChange={(event) => setLookupValue(event.target.value)} placeholder={lookupMode === 'id' ? 'e.g. 12' : 'student@university.edu'} /></label><button className="secondary" disabled={loading}>{loading ? 'Loading…' : 'Open action feed'}</button></form>{profile && !editing && <><ProfileCard profile={profile} /><button className="secondary" onClick={onEdit}>Edit Profile</button></>}{profile && editing && <form className="form-grid profile-edit-form" onSubmit={onSave}><label>Email<input required type="email" value={form.email} onChange={(event) => update('email', event.target.value)} /></label><label>Department<input required value={form.department} onChange={(event) => update('department', event.target.value)} /></label><label>Programme<input required value={form.programme} onChange={(event) => update('programme', event.target.value)} /></label><div className="split-fields"><label>Year<input required min="1" type="number" value={form.year} onChange={(event) => update('year', event.target.value)} /></label><label>Semester<input min="1" type="number" value={form.semester} onChange={(event) => update('semester', event.target.value)} /></label></div><label>Interests<input required value={form.interests} onChange={(event) => update('interests', event.target.value)} /><small>Separate interests with commas. Remove an interest by deleting it.</small></label><div className="form-actions"><button className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save Changes'}</button><button type="button" className="secondary" onClick={onCancel} disabled={saving}>Cancel</button></div></form>}</section>
}

function CreateProfile({ form, setForm, saving, onSubmit }) {
  const update = (key, value) => setForm({ ...form, [key]: value })
  return <section className="form-card create-view"><div className="section-title"><div><p className="eyebrow">Get started</p><h2>Create a profile</h2></div><span className="step-badge">01</span></div><form className="form-grid" onSubmit={onSubmit}><label>Email<input required type="email" value={form.email} onChange={(event) => update('email', event.target.value)} /></label><label>Department<input required value={form.department} onChange={(event) => update('department', event.target.value)} placeholder="Computer Science" /></label><label>Programme<input required value={form.programme} onChange={(event) => update('programme', event.target.value)} placeholder="BTech" /></label><div className="split-fields"><label>Year<input required min="1" type="number" value={form.year} onChange={(event) => update('year', event.target.value)} /></label><label>Semester<input min="1" type="number" value={form.semester} onChange={(event) => update('semester', event.target.value)} placeholder="Optional" /></label></div><label>Interests<input required value={form.interests} onChange={(event) => update('interests', event.target.value)} placeholder="internships, hackathons" /><small>Separate interests with commas.</small></label><button className="primary" disabled={saving}>{saving ? 'Creating…' : 'Create profile'}</button></form></section>
}

export default App
