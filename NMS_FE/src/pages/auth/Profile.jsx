import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { changeMyPassword, getMyProfile } from '../../api/user';
import { clearSession, getSession, saveUser } from '../../services/session';
import { homeForRole, roleName } from '../../services/roles';
import { validatePasswordChange } from './authForm';

const emptyPasswords = () => ({ currentPassword: '', newPassword: '', confirmation: '' });

export default function Profile() {
  const [account, setAccount] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [passwords, setPasswords] = useState(emptyPasswords);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();
  const session = getSession();

  useEffect(() => {
    if (!getSession()) return;
    const requestedAccountId = getSession().user.accountId;
    const controller = new AbortController();
    getMyProfile({ signal: controller.signal })
      .then((profile) => {
        if (controller.signal.aborted || getSession()?.user.accountId !== requestedAccountId) return;
        saveUser(profile);
        setAccount(profile);
        setLoadError('');
      })
      .catch((error) => {
        if (!controller.signal.aborted) setLoadError(error.response?.data?.message || 'Cannot load your account. Please try again.');
      });
    return () => controller.abort();
  }, [attempt]);

  if (!session) return <Navigate to="/login" replace />;

  const submit = async (event) => {
    event.preventDefault();
    if (saving) return;
    setFormError('');
    const errors = validatePasswordChange(passwords);
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;
    setSaving(true);
    try {
      await changeMyPassword({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      setPasswords(emptyPasswords());
      clearSession();
      navigate('/login', { replace: true, state: { passwordChanged: true } });
    } catch (error) {
      setFieldErrors(error.response?.data?.errors || {});
      setFormError(error.response?.data?.message || 'Cannot change your password. Please try again.');
    } finally { setSaving(false); }
  };

  return (
    <Container className="py-4">
      <div className="d-flex flex-wrap gap-3 justify-content-between align-items-center mb-4">
        <h1>My account</h1>
        <div className="d-flex gap-3">
          <Link to={homeForRole(account?.accountRole ?? session.user.accountRole)}>Back to home</Link>
          <Button variant="outline-secondary" onClick={() => { clearSession(); navigate('/login', { replace: true }); }}>Sign out</Button>
        </div>
      </div>
      {loadError && <Alert variant="danger">{loadError} <Button variant="outline-danger" size="sm"
        onClick={() => setAttempt((value) => value + 1)}>Retry</Button></Alert>}
      {!account && !loadError && <Spinner animation="border" role="status" aria-label="Loading your profile" />}
      {account && <Row className="g-4">
        <Col lg={5}><Card><Card.Body>
          <Card.Title>Profile</Card.Title>
          <dl className="mb-0">
            <dt>Name</dt><dd>{account.accountName}</dd>
            <dt>Email</dt><dd>{account.accountEmail}</dd>
            <dt>Role</dt><dd>{roleName(account.accountRole)}</dd>
            <dt>Verification</dt><dd><Badge bg={account.isVerified ? 'success' : 'warning'}>
              {account.isVerified ? 'Verified' : 'Unverified'}
            </Badge></dd>
          </dl>
          {!account.isVerified && <p className="text-muted mt-3 mb-0">An administrator can verify your account.</p>}
        </Card.Body></Card></Col>
        <Col lg={7}><Card><Card.Body>
          <Card.Title>Change password</Card.Title>
          <p className="text-muted">After changing your password, sign in again on your devices.</p>
          {formError && <Alert variant="danger">{formError}</Alert>}
          <Form onSubmit={submit} noValidate><fieldset disabled={saving}>
            {[
              ['currentPassword', 'Current password', 'current-password'],
              ['newPassword', 'New password', 'new-password'],
              ['confirmation', 'Confirm new password', 'new-password'],
            ].map(([key, label, autoComplete]) => <Form.Group key={key} controlId={`profile-${key}`} className="mb-3">
              <Form.Label>{label}</Form.Label>
              <Form.Control type="password" autoComplete={autoComplete} required maxLength={72}
                value={passwords[key]} isInvalid={!!fieldErrors[key]} onChange={(event) => {
                  setPasswords((previous) => ({ ...previous, [key]: event.target.value }));
                  setFieldErrors((previous) => ({ ...previous, [key]: undefined }));
                }} />
              <Form.Control.Feedback type="invalid">{fieldErrors[key]}</Form.Control.Feedback>
            </Form.Group>)}
            <Form.Text className="d-block mb-3">Use 6–72 characters and at most 72 UTF-8 bytes.</Form.Text>
            <Button type="submit">{saving ? 'Saving...' : 'Change password'}</Button>
          </fieldset></Form>
        </Card.Body></Card></Col>
      </Row>}
    </Container>
  );
}
