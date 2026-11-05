export function syncResultMessage(result) {
  return `Gmail sync complete: ${result.fetched} fetched, ${result.created} new, ${result.alreadyExisted} already stored, ${result.failed} failed.`
}

export function syncHeaders(csrfToken) {
  return {
    'Content-Type': 'application/json',
    'X-CampusSignal-CSRF': csrfToken,
  }
}
