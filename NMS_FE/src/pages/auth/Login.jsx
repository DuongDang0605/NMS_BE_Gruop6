import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Button, Container, Card, Alert } from 'react-bootstrap';
import logo from '../assets/fpt-university-logo.png';
import api from '../services/api';
import { saveSession } from '../services/session';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', {
        accountEmail: email.trim(),
        accountPassword: password,
      });
      saveSession(data);
      navigate('/dashboard', { replace: true });
    } catch (error) {
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

          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                autoComplete="username"
                placeholder="Enter email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
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
          </Card.Body>
        </Card>
      </Container>
    </div>
  );
};

export default Login;
