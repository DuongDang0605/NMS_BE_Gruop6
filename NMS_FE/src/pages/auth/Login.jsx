import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Form, Button, Container, Card, Alert, Modal } from 'react-bootstrap';
import logo from '../../assets/fpt-university-logo.png';
import { login } from '../../api/auth';
import { saveSession } from '../../services/session';
import UnlockAccountModal from './UnlockAccountModal';
import { homeForRole } from '../../services/roles';

const Login = () => {
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [locked, setLocked] = useState(false);
  const [showUnlock, setShowUnlock] = useState(false);
  const [showInactive, setShowInactive] = useState(false);
  const [success, setSuccess] = useState(location.state?.passwordChanged
    ? 'Password changed. Sign in with your new password.'
    : location.state?.registered ? 'Account created. Sign in to continue.' : '');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError('');
    setSuccess('');
    setLocked(false);
    setShowInactive(false);
    try {
      const data = await login({
        accountEmail: email.trim(),
        accountPassword: password,
      });
      saveSession(data);
      navigate(homeForRole(data.accountRole), { replace: true });
    } catch (error) {
      if (error.response?.data?.errors?.account === 'ACCOUNT_INACTIVE') {
        setShowInactive(true);
        return;
      }
      setLocked(error.response?.data?.errors?.account === 'ACCOUNT_LOCKED');
      setError(error.response?.data?.message || 'Cannot connect to the server. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <Container className="d-flex align-items-center justify-content-center min-vh-100 py-4">
        <Card className="login-card">
          <Card.Body className="p-4 p-md-5">
          <div className="text-center mb-4">
            <img src={logo} alt="FPT University" className="login-logo mb-4" />
            <h4 className="login-title mb-1">News Management System</h4>
            <p className="login-subtitle mb-0">Sign in to the administration portal</p>
          </div>

          {error && <Alert variant="danger">{error}</Alert>}
          {success && <Alert variant="success">{success}</Alert>}

          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                autoComplete="username"
                placeholder="Enter email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setLocked(false); setError(''); }}
                required
              />
            </Form.Group>

            <Form.Group className="mb-4">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                autoComplete="current-password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </Form.Group>

            <Button variant="primary" type="submit" className="w-100" disabled={submitting}>
              <i className="bi bi-box-arrow-in-right me-2"></i>{submitting ? 'Signing in...' : 'Login'}
            </Button>
          </Form>
          <Button variant={locked ? 'warning' : 'link'} className="w-100 mt-3" disabled={submitting}
            onClick={() => setShowUnlock(true)}>
            {locked ? 'Request account unlock' : 'Unlock Account/Forgot password'}
          </Button>
          <div className="d-flex flex-column flex-sm-row gap-2 mt-3">
            <Button as={Link} to="/register" variant="outline-primary" className="w-100 py-2">
              <i className="bi bi-person-plus me-2" aria-hidden="true" />Register
            </Button>
            <Button as={Link} to="/portal" variant="outline-secondary" className="w-100 py-2">
              <i className="bi bi-newspaper me-2" aria-hidden="true" />Explore News
            </Button>
          </div>
          </Card.Body>
        </Card>
      </Container>
      <Modal show={showInactive} onHide={() => setShowInactive(false)} centered aria-labelledby="inactive-account-title">
        <Modal.Header closeButton>
          <Modal.Title id="inactive-account-title">Tài khoản đã bị vô hiệu hóa</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ Admin để biết thêm chi tiết.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="primary" onClick={() => setShowInactive(false)}>Đóng</Button>
        </Modal.Footer>
      </Modal>
      {showUnlock && <UnlockAccountModal initialEmail={email} onClose={() => setShowUnlock(false)}
        onUnlocked={(accountEmail, message) => {
          setEmail(accountEmail); setPassword(''); setLocked(false); setError(''); setSuccess(message); setShowUnlock(false);
        }} />}
    </div>
  );
};

export default Login;
