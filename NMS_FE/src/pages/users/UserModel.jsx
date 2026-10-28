import { useState } from 'react';
import { Alert, Button, Form, Modal } from 'react-bootstrap';
import { createUser, deleteUser, updateUser } from '../../api/user';
import { apiErrorMessage, emptyUser, userPayload, validateUser } from './userForm';

const UserModel = ({ mode, user, onHide, onSuccess }) => {
  const [currentUser, setCurrentUser] = useState(() => user ? { ...user, accountPassword: '' } : emptyUser());
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const isDelete = mode === 'delete';
  const isEdit = mode === 'edit';
  const title = isDelete ? 'Confirm Delete' : isEdit ? 'Edit User' : 'Add User';

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
    if (!isDelete) {
      const errors = validateUser(currentUser);
      setFieldErrors(errors);
      if (Object.keys(errors).length) return;
    }
    setSaving(true);
    try {
      if (isDelete) await deleteUser(user.accountId);
      else if (isEdit) await updateUser(user.accountId, userPayload(currentUser));
      else await createUser(userPayload(currentUser));
    } catch (error) {
      setFieldErrors(error.response?.data?.errors || {});
      setFormError(apiErrorMessage(error, isDelete ? 'Unable to delete user.' : 'Unable to save user.'));
      setSaving(false);
      return;
    }
    onSuccess(isDelete ? 'User deleted successfully.' : isEdit ? 'User updated successfully.' : 'User created successfully.');
  };

  return (
    <Modal show onHide={close} backdrop={saving ? 'static' : true} keyboard={!saving} aria-labelledby="user-modal-title">
      <Modal.Header closeButton={!saving}>
        <Modal.Title id="user-modal-title">{title}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={submit} noValidate>
        <Modal.Body>
          {formError && <Alert variant="danger">{formError}</Alert>}
          {isDelete ? (
            <p className="mb-0">Delete user <strong>{user.accountName}</strong> ({user.accountEmail})?</p>
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
                <Form.Select value={currentUser.accountRole} isInvalid={!!fieldErrors.accountRole}
                  onChange={(event) => editField('accountRole', Number(event.target.value))}>
                  <option value={1}>Admin</option><option value={2}>Staff</option>
                </Form.Select>
                <Form.Control.Feedback type="invalid">{fieldErrors.accountRole}</Form.Control.Feedback>
              </Form.Group>
            </fieldset>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={close} disabled={saving}>Cancel</Button>
          <Button type="submit" variant={isDelete ? 'danger' : 'primary'} disabled={saving}>
            {saving ? (isDelete ? 'Deleting...' : 'Saving...') : (isDelete ? 'Delete' : 'Save')}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
};

export default UserModel;
