import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyUser, userPayload, validateUser } from '../src/pages/users/userForm.js';

const validUser = () => ({ ...emptyUser(), accountName: 'Test User', accountEmail: 'test@example.com', accountPassword: 'secret123' });

test('create requires a password; update omits an unchanged password', () => {
  const create = { ...validUser(), accountPassword: '' };
  assert.ok(validateUser(create).accountPassword);
  const update = { ...create, accountId: 42 };
  assert.deepEqual(validateUser(update), {});
  assert.equal(Object.hasOwn(userPayload(update), 'accountPassword'), false);
});

test('password validation enforces the BCrypt UTF-8 byte limit', () => {
  assert.ok(validateUser({ ...validUser(), accountPassword: 'ệ'.repeat(25) }).accountPassword);
  assert.deepEqual(validateUser({ ...validUser(), accountPassword: 'ệ'.repeat(24) }), {});
  assert.ok(validateUser({ ...validUser(), accountPassword: '      ' }).accountPassword);
  assert.ok(validateUser({ ...validUser(), accountId: 42, accountPassword: 'short' }).accountPassword);
});

test('payload normalizes name and email, excludes ID, and preserves password spaces', () => {
  const user = { ...validUser(), accountId: 42, accountName: '  Test  ', accountEmail: ' TEST@EXAMPLE.COM ', accountPassword: ' secret123 ', accountRole: '1' };
  assert.deepEqual(userPayload(user), { accountName: 'Test', accountEmail: 'test@example.com', accountRole: 1, accountPassword: ' secret123 ' });
});

test('invalid identity fields and roles are rejected', () => {
  const errors = validateUser({ ...validUser(), accountName: '  ', accountEmail: 'invalid', accountRole: 3 });
  assert.ok(errors.accountName);
  assert.ok(errors.accountEmail);
  assert.ok(errors.accountRole);
});
