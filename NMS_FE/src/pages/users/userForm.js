export const emptyUser = () => ({
  accountId: '', accountName: '', accountEmail: '', accountRole: 2, accountPassword: '',
});

export function validateUser(user, originalUser = null) {
  const errors = {};
  if (!user.accountName.trim()) errors.accountName = 'Name is required.';
  else if (user.accountName.trim().length > 100) errors.accountName = 'Name must contain at most 100 characters.';
  if (!user.accountEmail.trim()) errors.accountEmail = 'Email is required.';
  else if (user.accountEmail.trim().length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.accountEmail.trim())) {
    errors.accountEmail = 'Enter a valid email address (at most 254 characters).';
  }
  const isExistingAdmin = !!originalUser?.accountId && Number(originalUser.accountRole) === 1;
  if (isExistingAdmin) {
    if (Number(user.accountRole) !== 1) errors.accountRole = 'The role of an Admin account cannot be changed.';
  } else if (![2, 3, 4, 5].includes(Number(user.accountRole))) {
    errors.accountRole = 'Select Contributor, Editor, Moderator or Reader. Admin cannot be assigned.';
  }
  const password = user.accountPassword;
  if (!user.accountId || password !== '') {
    if (!password.trim()) errors.accountPassword = 'Password is required.';
    else if (password.length < 6 || password.length > 72 || new TextEncoder().encode(password).length > 72) {
      errors.accountPassword = 'Password must contain 6–72 characters and at most 72 UTF-8 bytes.';
    }
  }
  return errors;
}

export function userPayload(user) {
  const payload = {
    accountName: user.accountName.trim(),
    accountEmail: user.accountEmail.trim().toLowerCase(),
    accountRole: Number(user.accountRole),
  };
  if (!user.accountId || user.accountPassword !== '') payload.accountPassword = user.accountPassword;
  return payload;
}

export function apiErrorMessage(error, fallback) {
  if (!error.response) return 'Cannot connect to the server. Check your connection and try again.';
  return error.response.data?.message || fallback;
}
