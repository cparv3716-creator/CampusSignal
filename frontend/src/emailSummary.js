const DATE_PATTERN = /\b(?:\d{4}-\d{2}-\d{2}|\d{1,2}\/\d{1,2}\/\d{4}|\d{1,2}\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{4}|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2},?\s+\d{4})\b/i
const URL_PATTERN = /https?:\/\/[^\s<>"']+/i
const HEADER_PATTERN = /^(?:from|to|cc|bcc|subject|date|sent|reply[- ]to):\s*/i
const CLOSING_PATTERN = /^(?:regards|best(?: regards)?|sincerely|thanks|thank you|warm regards|yours|the campus team)[,!.\s]*$/i
const GREETING_PATTERN = /^(?:dear\s+(?:all|students?|colleagues?|sir|madam|team)|hi|hello|greetings?)\b[,!:.]*/i
const BOILERPLATE_PATTERN = /^(?:this is an automated|please do not reply|unsubscribe|confidentiality notice|disclaimer|sent from my|follow us on|copyright)\b/i
const FOOTER_PATTERN = /(?:unsubscribe|manage preferences|email preferences|report a problem|get the app|download the app|view in browser|privacy policy|terms of use|follow us|all rights reserved)/i
const ABBREVIATION_PATTERN = /\b(?:dr|prof|mr|mrs|ms|miss|sr|jr|st|no|e\.g|i\.e|etc|vs|fig)\.$/i
const FRAGMENT_PATTERN = /^(?:and|or|but|so|also|which|that|because|as|to|for|from|with|of)\b/i

function decodeHtml(value) {
  return value
    .replace(/<a\b[^>]*href=["'](https?:\/\/[^"']+)["'][^>]*>(.*?)<\/a>/gi, '$2 $1')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/(?:p|div|li|h[1-6]|tr)>/gi, '\n')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&#39;|&apos;/gi, "'")
    .replace(/&quot;/gi, '"')
}

function normalized(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim()
}

function sentences(body) {
  const text = decodeHtml(body ?? '')
  const lines = text.split(/\r?\n+/)
  const contentLines = []
  for (const line of lines) {
    if (FOOTER_PATTERN.test(line)) break
    contentLines.push(line)
  }
  const protectedText = contentLines.join('\n')
    .replace(/\b(?:Dr|Prof|Mr|Mrs|Ms|Miss|Sr|Jr|St|No|e\.g|i\.e|etc|vs|Fig)\./gi, (match) => match.replace('.', '\u0001'))
    .replace(/(\b[A-Z])\.\s*(?=[A-Z][a-z])/g, '$1\u0001')
    .replace(/(\d)\.(\d)/g, '$1\u0001$2')
  return protectedText
    .split(/\r?\n+|(?<=[.!?])\s+(?=[A-Z0-9"'])/)
    .map((sentence) => sentence.replaceAll('\u0001', '.'))
    .map((sentence) => sentence.replace(/\s+/g, ' ').trim())
    .filter((sentence) => sentence.length >= 12)
    .filter((sentence) => !HEADER_PATTERN.test(sentence))
    .filter((sentence) => !GREETING_PATTERN.test(sentence))
    .filter((sentence) => !CLOSING_PATTERN.test(sentence))
    .filter((sentence) => !BOILERPLATE_PATTERN.test(sentence))
    .filter((sentence) => !FOOTER_PATTERN.test(sentence))
    .filter((sentence) => !FRAGMENT_PATTERN.test(sentence))
    .filter((sentence) => !/^(?:get app|unsubscribe|report a problem)\b/i.test(sentence))
}

function firstMatchingSentence(items, pattern) {
  return items.find((sentence) => pattern.test(sentence))
}

function withoutTrailingPunctuation(value) {
  return value.replace(/[),.;:!?]+$/, '')
}

function isSecurityMessage(message) {
  return /security|sign[- ]in|account|password|suspicious|verification/i.test(
    `${message.subject} ${message.body}`,
  )
}

function actionSentence(items, securityMessage) {
  const explicitAction = /action required|please\s+(?:check|review|secure|change|reset|confirm|contact|register|apply|submit|upload|attend|join)|must\s+(?:check|review|secure|change|reset|confirm|register|apply|submit|upload|attend|join)|required to\s+(?:check|review|secure|change|reset|confirm|register|apply|submit|upload|attend|join)|(?:register|apply|submit|upload|attend|join)\b/i
  return items.find((sentence) => {
    if (!explicitAction.test(sentence)) return false
    if (!securityMessage) return true
    return /security|account|password|sign[- ]in|suspicious|verification|if this was not you|if you do not recognize/i.test(sentence)
  })
}

function importantLink(items, candidate) {
  const actionContext = /register|registration|sign\s+up|apply|application|submit|submission|upload|attend|join|participate|action required|check|review|secure|password/i
  for (const sentence of items) {
    const match = sentence.match(URL_PATTERN)
    const extracted = match && withoutTrailingPunctuation(match[0])
    if (extracted && actionContext.test(sentence)
        && (!candidate?.actionUrl || candidate.actionUrl === extracted)) return extracted
  }
  return null
}

function substantiveSentence(items, subject) {
  const subjectText = normalized(subject)
  const scored = items
    .filter((sentence) => normalized(sentence) !== subjectText)
    .filter((sentence) => !subjectText || normalized(sentence) !== subjectText)
    .map((sentence, index) => {
      let score = Math.min(sentence.length, 180) / 40
      if (/\b(?:announce|opportunity|available|invited|will be held|open|launch|offer|seeking|deadline|applications?)\b/i.test(sentence)) score += 3
      if (/\b(?:complaint|issue|problem|request|resolution|refund|blocked|failed|unable|withdrawn|transaction)\b/i.test(sentence)) score += 3
      if (DATE_PATTERN.test(sentence) || URL_PATTERN.test(sentence)) score += 1
      if (URL_PATTERN.test(sentence) && !/register|registration|sign\s+up|apply|application|submit|submission|upload|attend|join|participate|action required|check|review|secure|password/i.test(sentence)) score -= 4
      if (/^(?:and|but|so|also)\b/i.test(sentence)) score -= 1
      return { sentence, score: score - index / 100 }
    })
    .sort((left, right) => right.score - left.score)
  return scored[0]?.sentence
}

function complaintSentence(items, pattern) {
  return items
    .filter((sentence) => pattern.test(sentence) && sentence.split(/\s+/).length >= 6)
    .sort((left, right) => {
      const outcome = /\b(declined|withdrawn|failed|incorrect|not received|blocked)\b/i
      return Number(outcome.test(right)) - Number(outcome.test(left))
    })[0]
}

export function summarizeEmail(message, candidate) {
  const bodySentences = sentences(message.body)
  const subject = message.subject?.trim() || 'Email received'
  const summary = [{ label: 'Announcement', text: substantiveSentence(bodySentences, subject) || subject }]

  const eligibility = firstMatchingSentence(
    bodySentences,
    /eligible|eligibility|eligibl|requirements?|qualif(?:y|ied|ication)|open to/i,
  )
  if (eligibility) summary.push({ label: 'Eligibility', text: eligibility })

  const issue = complaintSentence(
    bodySentences,
    /complaint|issue|problem|failed|unable|incorrect|not received|declined|withdrawn|transaction/i,
  )
  if (issue && !summary.some((item) => item.text === issue)) {
    summary.push({ label: 'Reported issue', text: issue })
  }
  const resolution = complaintSentence(
    bodySentences,
    /request(?:ed)?|please\s+(?:resolve|refund|reverse|investigate|help|credit|correct)|resolution|would like|kindly/i,
  )
  if (resolution && !summary.some((item) => item.text === resolution)) {
    summary.push({ label: 'Requested resolution', text: resolution })
  }

  const deadline = firstMatchingSentence(
    bodySentences,
    /deadline|last date|close(?:s|d)?\s+on|apply(?:\s+\w+){0,4}\s+by|submit(?:\s+\w+){0,4}\s+by|applications?\s+(?:close|due)/i,
  )
  if (deadline && DATE_PATTERN.test(deadline)) {
    summary.push({ label: 'Date or deadline', text: deadline })
  } else {
    const eventDate = firstMatchingSentence(bodySentences, DATE_PATTERN)
    if (eventDate && !summary.some((item) => item.text === eventDate)) {
      summary.push({ label: 'Details', text: eventDate })
    }
  }
  const contextualDetail = bodySentences.find((sentence) =>
    sentence !== summary[0].text
    && (/\b\d{1,2}\.\d{1,2}\b/.test(sentence) || /\b(?:at|in|on)\b/i.test(sentence))
    && !URL_PATTERN.test(sentence)
    && !summary.some((item) => item.text === sentence),
  )
  if (contextualDetail && summary.length < 5) {
    summary.push({ label: 'Details', text: contextualDetail })
  }

  const securityMessage = isSecurityMessage(message)
  const action = actionSentence(bodySentences, securityMessage)
  if (action) summary.push({ label: 'Action required', text: action })

  const link = importantLink(bodySentences, candidate)
  if (link) summary.push({ label: 'Important link', text: link })

  if (summary.length === 1 && bodySentences.length > 0) {
    summary.push({ label: 'Action', text: 'No action explicitly requested' })
  }

  return summary.slice(0, 5)
}
