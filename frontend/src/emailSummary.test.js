import test from 'node:test'
import assert from 'node:assert/strict'
import { summarizeEmail } from './emailSummary.js'

test('summarizes long opportunity emails into at most five supported bullets', () => {
  const summary = summarizeEmail({
    subject: 'Research internship applications',
    body: 'Applications are open for eligible final-year students. Submit your CV by 30 November 2026. Apply here: https://example.com/apply. This extra sentence should not be shown.',
  })

  assert.ok(summary.length <= 5)
  assert.deepEqual(summary.map((item) => item.label), [
    'Announcement',
    'Eligibility',
    'Date or deadline',
    'Action required',
    'Important link',
  ])
  assert.ok(summary.some((item) => item.text.includes('30 November 2026')))
  assert.ok(summary.some((item) => item.text.includes('https://example.com/apply')))
  assert.ok(!summary.some((item) => item.text.includes('extra sentence')))
})

test('uses fewer bullets for a short email', () => {
  const summary = summarizeEmail({ subject: 'Library hours', body: 'The campus library is open until 10 PM.' })

  assert.equal(summary.length, 2)
  assert.equal(summary[1].text, 'No action explicitly requested')
})

test('omits unsupported eligibility, date, action, and link fields', () => {
  const summary = summarizeEmail({
    subject: 'Campus update',
    body: 'The student centre has moved to a new building. More information may be shared later.',
  })

  assert.deepEqual(summary.map((item) => item.label), ['Announcement', 'Action'])
  assert.ok(!summary.some((item) => /deadline|eligible|http|action required/i.test(item.text)))
})

test('only highlights an explicit security action', () => {
  const passive = summarizeEmail({
    subject: 'Security alert',
    body: 'We noticed a new sign-in on a Windows device. If this was you, you do not need to do anything.',
  })
  assert.ok(!passive.some((item) => item.label === 'Action required'))

  const explicit = summarizeEmail({
    subject: 'Security alert',
    body: 'We noticed a suspicious sign-in. Please review your account security at https://example.com/security.',
  })
  assert.ok(explicit.some((item) => item.label === 'Action required'))
  assert.ok(explicit.some((item) => item.label === 'Important link'))
})

test('does not trust an unrelated URL or candidate detail as an email fact', () => {
  const summary = summarizeEmail({
    subject: 'Workshop announcement',
    body: 'The workshop will be held on campus. Visit https://example.com/news for general news.',
  }, { deadline: '2026-12-01', actionUrl: 'https://example.com/apply' })

  assert.ok(!summary.some((item) => item.label === 'Important link'))
  assert.ok(!summary.some((item) => item.label === 'Date or deadline'))
})

test('removes greetings, repeated subject text, headers, and signatures', () => {
  const summary = summarizeEmail({
    subject: 'Scholarship applications open',
    body: 'Subject: Scholarship applications open\nDear All,\nScholarships are available for students facing financial hardship.\nRegards,\nThe campus team',
  })

  assert.equal(summary[0].text, 'Scholarships are available for students facing financial hardship.')
  assert.ok(!summary.some((item) => /dear all|regards|subject:/i.test(item.text)))
})

test('distinguishes an event date from an application deadline', () => {
  const summary = summarizeEmail({
    subject: 'Research talk',
    body: 'The research talk will be held on 20 December 2026 in Lecture Hall A. Registration is open to all students.',
  })

  assert.ok(!summary.some((item) => item.label === 'Date or deadline'))
  assert.ok(summary.some((item) => item.text.includes('20 December 2026')))
})

test('extracts meaningful content from short HTML-derived messages', () => {
  const summary = summarizeEmail({
    subject: 'Internship opportunity',
    body: '<p>Dear students,</p><p>Eligible final-year students are invited to apply for a summer internship.</p><p>Applications close on 15 January 2027.</p><p>Apply at <a href="https://example.com/apply">the portal</a>.</p><p>Regards,<br>Career Cell</p>',
  })

  assert.ok(summary.some((item) => item.label === 'Eligibility'))
  assert.ok(summary.some((item) => item.label === 'Date or deadline'))
  assert.ok(summary.some((item) => item.label === 'Important link'))
  assert.ok(!summary.some((item) => /dear students|career cell/i.test(item.text)))
})

test('does not promote a misleading date or unrelated link', () => {
  const summary = summarizeEmail({
    subject: 'Campus newsletter',
    body: 'The newsletter archive was updated on 1 January 2027. Read general news at https://example.com/news. No registration is requested.',
  })

  assert.ok(!summary.some((item) => item.label === 'Date or deadline'))
  assert.ok(!summary.some((item) => item.label === 'Important link'))
  assert.ok(!summary.some((item) => item.text.includes('https://example.com/news')))
})

test('does not split honorifics, initials, or decimal values into fragments', () => {
  const summary = summarizeEmail({
    subject: 'NCC announcement',
    body: 'We are pleased to announce that Dr. Anita Rao will lead the NCC orientation. The session begins at 10.30 AM in the auditorium.',
  })

  assert.ok(summary.some((item) => item.text.includes('Dr. Anita Rao')))
  assert.ok(!summary.some((item) => item.text === 'Dr.'))
  assert.ok(summary.some((item) => item.text.includes('10.30')))
})

test('removes promotional navigation and footer contamination', () => {
  const summary = summarizeEmail({
    subject: 'Career opportunities',
    body: 'Naukri is sharing current opportunities for students seeking internships.\nGet App\nDownload the app\nUnsubscribe\nReport a Problem\nPrivacy Policy',
  })

  assert.ok(summary.some((item) => item.text.includes('current opportunities')))
  assert.ok(!summary.some((item) => /get app|unsubscribe|report a problem|privacy policy/i.test(item.text)))
})

test('summarizes a complaint with the reported issue and requested resolution', () => {
  const summary = summarizeEmail({
    subject: 'Complaint regarding cash withdrawal',
    body: 'I am writing to report an issue with a cash withdrawal from my account at an SBI ATM. The transaction was declined but the amount was withdrawn from my balance. Please investigate and reverse the transaction at the earliest.',
  })

  assert.ok(summary.some((item) => item.label === 'Reported issue' && /declined|withdrawn/i.test(item.text)))
  assert.ok(summary.some((item) => item.label === 'Requested resolution' && /investigate|reverse/i.test(item.text)))
  assert.ok(!summary.some((item) => item.text.split(/\s+/).length < 5))
})
