export function profileToForm(value) {
  return {
    email: value.email ?? '',
    department: value.department ?? '',
    programme: value.programme ?? '',
    year: value.year ?? '',
    semester: value.semester ?? '',
    interests: (value.interests ?? []).join(', '),
  }
}

export function profilePayload(value) {
  const payload = {
    email: value.email,
    department: value.department,
    programme: value.programme,
    year: Number(value.year),
    interests: value.interests.split(',').map((interest) => interest.trim()).filter(Boolean),
  }
  if (value.semester) payload.semester = Number(value.semester)
  return payload
}

const SELECTED_PROFILE_KEY = 'campussignal.selectedProfileId'

export function saveSelectedProfileId(id) {
  globalThis.localStorage.setItem(SELECTED_PROFILE_KEY, String(id))
}

export function readSelectedProfileId() {
  return globalThis.localStorage.getItem(SELECTED_PROFILE_KEY)
}

export function clearSelectedProfileId() {
  globalThis.localStorage.removeItem(SELECTED_PROFILE_KEY)
}
