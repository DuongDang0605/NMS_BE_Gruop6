export function passwordError(password, maxCharacters = 72) {
  if (!password.trim()) return 'Password must not be blank.';
  if (password.length < 6 || password.length > maxCharacters) {
    return `Password must contain 6–${maxCharacters} characters.`;
  }
  if (new TextEncoder().encode(password).length > 72) return 'Password must contain at most 72 UTF-8 bytes.';
  return '';
}

export function validatePasswordChange({ currentPassword, newPassword, confirmation }) {
  const errors = {};
  if (!currentPassword.trim()) errors.currentPassword = 'Enter your current password.';
  else if (currentPassword.length > 72 || new TextEncoder().encode(currentPassword).length > 72) {
    errors.currentPassword = 'Current password must contain at most 72 UTF-8 bytes.';
  }
  const nextError = passwordError(newPassword);
  if (nextError) errors.newPassword = nextError;
  else if (newPassword === currentPassword) errors.newPassword = 'New password must differ from the current password.';
  if (confirmation !== newPassword) errors.confirmation = 'Passwords do not match.';
  return errors;
}
