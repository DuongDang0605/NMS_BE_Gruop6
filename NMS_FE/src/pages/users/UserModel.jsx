import { useState } from 'react';
import { Alert, Button, Form, Modal } from 'react-bootstrap';
import { activateUser, createUser, deactivateUser, updateUser } from '../../api/user';
import { apiErrorMessage, emptyUser, userPayload, validateUser } from './userForm';
import { ROLES } from '../../services/roles';

const UserModel = ({ mode, user, onHide, onSuccess }) => {
  const [currentUser, setCurrentUser] = useState(() => user ? { ...user, accountPassword: '' } : emptyUser());
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const isDeactivate = mode === 'deactivate';
  const isActivate = mode === 'activate';
  const isStatusChange = isDeactivate || isActivate;
  const isEdit = mode === 'edit';
  const isAdminUser = Number(user?.accountRole) === 1;
  const availableRoles = ROLES.filter((role) => isEdit && isAdminUser ? role.id === 1 : role.id !== 1);
  const title = isActivate ? 'Confirm Activation' : isDeactivate ? 'Confirm Deactivation' : isEdit ? 'Edit User' : 'Add User';

  const close = () => {
    if (!saving) onHide();
  };

  const editField = (field, value) => {
    setCurrentUser((previous) => ({ ...previous, [field]: value }));
    setFieldErrors((previous) => ({ ...previous, [field]: undefined }));
  };

  const submit = async (event) => {
    event.preventDefault();
    if (saving) return;
    setFormError('');
    if (isDeactivate && isAdminUser) {
      setFormError('Admin accounts cannot be deactivated.');
      return;
    }
    if (!isStatusChange) {
      const errors = validateUser(currentUser, isEdit ? user : null);
      setFieldErrors(errors);
      if (Object.keys(errors).length) return;
    }
    setSaving(true);
    try {
      if (isActivate) await activateUser(user.accountId);
      else if (isDeactivate) await deactivateUser(user.accountId);
      else if (isEdit) await updateUser(user.accountId, userPayload(currentUser));
      else await createUser(userPayload(currentUser));
    } catch (error) {
      setFieldErrors(error.response?.data?.errors || {});
      setFormError(apiErrorMessage(error, isActivate ? 'Unable to activate user.' : isDeactivate ? 'Unable to deactivate user.' : 'Unable to save user.'));
      setSaving(false);
      return;
    }
    onSuccess(isActivate ? 'User activated successfully.' : isDeactivate ? 'User deactivated successfully.' : isEdit ? 'User updated successfully.' : 'User created successfully.');
  };

  return (
    <Modal show onHide={close} backdrop={saving ? 'static' : true} keyboard={!saving} aria-labelledby="user-modal-title">
      <Modal.Header closeButton={!saving}>
        <Modal.Title id="user-modal-title">{title}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={submit} noValidate>
        <Modal.Body>
          {formError && <Alert variant="danger">{formError}</Alert>}
          {isActivate ? (
            <p className="mb-0">Activate user <strong>{user.accountName}</strong> ({user.accountEmail})?
              They will be able to sign in again unless their account is locked.</p>
          ) : isDeactivate ? (
            <p className="mb-0">Deactivate user <strong>{user.accountName}</strong> ({user.accountEmail})?
              Their account and related data will be kept, and they will no longer be able to sign in.</p>
          ) : (
            <fieldset disabled={saving}>
              {[
                { key: 'accountName', label: 'Name', maxLength: 100 },
                { key: 'accountEmail', label: 'Email', type: 'email', maxLength: 254 },
                { key: 'accountPassword', label: isEdit ? 'New password (optional)' : 'Password', type: 'password', maxLength: 72, autoComplete: 'new-password' },
              ].map(({ key, label, ...props }) => (
                <Form.Group className="mb-3" controlId={`user-${key}`} key={key}>
                  <Form.Label>{label}</Form.Label>
                  <Form.Control {...props} value={currentUser[key]} isInvalid={!!fieldErrors[key]}
                    onChange={(event) => editField(key, event.target.value)} />
                  <Form.Control.Feedback type="invalid">{fieldErrors[key]}</Form.Control.Feedback>
                  {key === 'accountPassword' && isEdit && <Form.Text>Leave blank to keep the current password.</Form.Text>}
                </Form.Group>
              ))}
              <Form.Group controlId="user-accountRole">
                <Form.Label>Role</Form.Label>
                <Form.Select value={currentUser.accountRole} isInvalid={!!fieldErrors.accountRole} disabled={isEdit && isAdminUser}
                  onChange={(event) => editField('accountRole', Number(event.target.value))}>
                  {availableRoles.map((role) => <option key={role.id} value={role.id}>{role.name}</option>)}
                </Form.Select>
                <Form.Control.Feedback type="invalid">{fieldErrors.accountRole}</Form.Control.Feedback>
                {isEdit && isAdminUser && <Form.Text>Admin role is fixed. You can update account details or reset the password.</Form.Text>}
              </Form.Group>
            </fieldset>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={close} disabled={saving}>Cancel</Button>
          <Button type="submit" variant={isActivate ? 'success' : isDeactivate ? 'danger' : 'primary'} disabled={saving || (isDeactivate && isAdminUser)}>
            {saving ? (isActivate ? 'Activating...' : isDeactivate ? 'Deactivating...' : 'Saving...') : (isActivate ? 'Activate' : isDeactivate ? 'Deactivate' : 'Save')}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
};

export default UserModel;
