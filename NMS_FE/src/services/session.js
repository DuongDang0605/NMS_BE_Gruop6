export function clearSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('authExpiresAt');
  localStorage.removeItem('user');
}

export function getSession() {
  try {
    const token = localStorage.getItem('accessToken');
    const expiresAt = Number(localStorage.getItem('authExpiresAt'));
    const refreshToken = localStorage.getItem('refreshToken');
    const user = JSON.parse(localStorage.getItem('user'));
    return token && (expiresAt > Date.now() || refreshToken) && user?.accountId
      ? { token, refreshToken, user }
      : null;
  } catch {
    return null;
  }
}

export function saveSession(login) {
  const { accountId, accountName, accountEmail, accountRole, roleName, isVerified } = login;
  if (accountId) {
    saveUser({
      accountId, accountName, accountEmail, accountRole, roleName, isVerified,
    });
  }
  localStorage.setItem('accessToken', login.accessToken);
  if (login.refreshToken) localStorage.setItem('refreshToken', login.refreshToken);
  localStorage.setItem('authExpiresAt', String(Date.now() + login.expiresIn * 1000));
}

export function saveUser(user) {
  localStorage.setItem('user', JSON.stringify(user));
}
