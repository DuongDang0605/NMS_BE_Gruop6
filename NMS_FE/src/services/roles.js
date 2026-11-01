export const ROLES = [
  { id: 1, name: 'Admin' },
  { id: 2, name: 'Contributor' },
  { id: 3, name: 'Editor' },
  { id: 4, name: 'Moderator' },
  { id: 5, name: 'Reader' },
];

export const roleName = (id) => ROLES.find((role) => role.id === Number(id))?.name || 'Unknown';
export const homeForRole = (id) => Number(id) === 5 ? '/portal' : '/dashboard';
export const canAccess = (user, roles) => !!user && roles.includes(Number(user.accountRole));
