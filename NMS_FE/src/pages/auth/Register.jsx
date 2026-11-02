import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Container, Form } from 'react-bootstrap';
import { registerReader } from '../../api/auth';
import { passwordError } from './authForm';

export default function Register() {
  const [form, setForm] = useState({ accountName: '', accountEmail: '', accountPassword: '', confirmation: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const submit = async (event) => {
    event.preventDefault();
    if (busy) return;
    setError('');
    if (form.accountPassword !== form.confirmation) { setError('Passwords do not match.'); return; }
    const invalidPassword = passwordError(form.accountPassword, 50);
    if (invalidPassword) { setError(invalidPassword); return; }
    setBusy(true);
    try {
      await registerReader({
        accountName: form.accountName.trim(), accountEmail: form.accountEmail.trim(), accountPassword: form.accountPassword,
      });
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setError(Object.values(err.response?.data?.errors || {})[0] || err.response?.data?.message || 'Unable to create account.');
    } finally { setBusy(false); }
  };
  return (
    <div className="login-page"><Container className="d-flex align-items-center justify-content-center min-vh-100 py-4">
      <Card className="login-card"><Card.Body className="p-4">
        <h2>Create a Reader account</h2><p>Read campus news and join the community.</p>
        {error && <Alert variant="danger">{error}</Alert>}
        <Form onSubmit={submit}><fieldset disabled={busy}>
          {[
            ['accountName', 'Name', 'text', 'name', 100], ['accountEmail', 'Email', 'email', 'email', 254],
            ['accountPassword', 'Password', 'password', 'new-password', 50], ['confirmation', 'Confirm password', 'password', 'new-password', 50],
          ].map(([key, label, type, autoComplete, maxLength]) => (
            <Form.Group key={key} controlId={`register-${key}`} className="mb-3">
              <Form.Label>{label}</Form.Label><Form.Control required type={type} autoComplete={autoComplete}
                maxLength={maxLength} minLength={type === 'password' ? 6 : undefined} value={form[key]}
                onChange={(event) => setForm((previous) => ({ ...previous, [key]: event.target.value }))} />
            </Form.Group>
          ))}
          <Button type="submit" className="w-100">{busy ? 'Creating...' : 'Register'}</Button>
        </fieldset></Form>
        <div className="text-center mt-3"><Link to="/login">Back to sign in</Link></div>
      </Card.Body></Card>
    </Container></div>
  );
}
