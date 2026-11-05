import test from 'node:test'
import assert from 'node:assert/strict'
import { syncHeaders, syncResultMessage } from './gmailSync.js'

test('builds a CSRF-only sync request header', () => {
  assert.deepEqual(syncHeaders('csrf-token'), {
    'Content-Type': 'application/json',
    'X-CampusSignal-CSRF': 'csrf-token',
  })
})

test('formats only synchronization counts for the user', () => {
  assert.equal(
    syncResultMessage({ fetched: 10, created: 3, alreadyExisted: 6, failed: 1 }),
    'Gmail sync complete: 10 fetched, 3 new, 6 already stored, 1 failed.',
  )
})
