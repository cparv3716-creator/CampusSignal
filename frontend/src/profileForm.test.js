import test from 'node:test'
import assert from 'node:assert/strict'
import {
  clearSelectedProfileId,
  profilePayload,
  profileToForm,
  readSelectedProfileId,
  saveSelectedProfileId,
} from './profileForm.js'

test('prepopulates and edits interests without creating a new profile', () => {
  const form = profileToForm({
    email: 'student@example.edu',
    department: 'Computer Science',
    programme: 'BTech',
    year: 3,
    semester: null,
    interests: ['internships', 'research'],
  })
  form.interests = 'internships, scholarships'
  assert.deepEqual(profilePayload(form), {
    email: 'student@example.edu',
    department: 'Computer Science',
    programme: 'BTech',
    year: 3,
    interests: ['internships', 'scholarships'],
  })
})

test('removing all but one interest preserves supported profile fields', () => {
  assert.deepEqual(profilePayload({
    email: 'student@example.edu',
    department: 'Design',
    programme: 'BDes',
    year: '2',
    semester: '4',
    interests: 'events',
  }), {
    email: 'student@example.edu',
    department: 'Design',
    programme: 'BDes',
    year: 2,
    semester: 4,
    interests: ['events'],
  })

  test('selected profile id persists and can be cleared', () => {
    const originalStorage = globalThis.localStorage
    const values = new Map()
    globalThis.localStorage = {
      setItem: (key, value) => values.set(key, value),
      getItem: (key) => values.get(key) ?? null,
      removeItem: (key) => values.delete(key),
    }
    try {
      saveSelectedProfileId(42)
      assert.equal(readSelectedProfileId(), '42')
      clearSelectedProfileId()
      assert.equal(readSelectedProfileId(), null)
    } finally {
      globalThis.localStorage = originalStorage
    }
  })
})
