import { useEffect, useState } from 'react';
import { Alert, Button, Form, Modal } from 'react-bootstrap';
import { requestUnlock, resetUnlockPassword, verifyUnlock } from '../../api/auth';

export default function UnlockAccountModal({ initialEmail, onClose, onUnlocked }) {
  const [email, setEmail] = useState(initialEmail);
  const [step, setStep] = useState('request');
  const [otp, setOtp] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((value) => value - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const sendOtp = async () => {
    const data = await requestUnlock(email.trim());
    setNotice(data.message);
    setCooldown(60);
    setOtp('');
    setResetToken('');
    setStep('verify');
  };

  const perform = async (action) => {
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (err) {
      const data = err.response?.data;
      setError(Object.values(data?.errors || {})[0] || data?.message
        || (err.response || err.request ? 'Cannot connect to the server. Please try again.' : err.message));
    } finally {
      setBusy(false);
    }
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    perform(async () => {
      if (step === 'request') {
        await sendOtp();
      } else if (step === 'verify') {
        const data = await verifyUnlock(email.trim(), otp);
        setResetToken(data.resetToken);
        setNotice('Email verified. Set a new password within 5 minutes to unlock your account.');
        setStep('reset');
      } else {
        if (password !== confirmation) throw new Error('Passwords do not match.');
        if (!password.trim() || new TextEncoder().encode(password).length > 72) {
          setError('Password must not be blank and must contain at most 72 UTF-8 bytes.');
          return;
        }
        const data = await resetUnlockPassword(email.trim(), resetToken, password);
        onUnlocked(email.trim(), data.message);
      }
    });
  };

  return (
    <Modal show onHide={busy ? undefined : onClose} backdrop={busy ? 'static' : true} keyboard={!busy} centered>
      <Modal.Header closeButton={!busy}>
        <Modal.Title>Unlock account</Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          {error && <Alert variant="danger">{error}</Alert>}
          {notice && <Alert variant="info">{notice}</Alert>}
          <p className="text-muted">
            {step === 'request' && 'Enter your account email to receive an unlock code.'}
            {step === 'verify' && 'Enter the 6-digit code from your email. The code expires in 5 minutes.'}
            {step === 'reset' && 'Your account stays locked until you save a new password.'}
          </p>
          <Form.Group className="mb-3" controlId="unlock-email">
            <Form.Label>Account email</Form.Label>
            <Form.Control type="email" autoComplete="username" maxLength={254} required
              value={email} disabled={busy || step !== 'request'} onChange={(event) => setEmail(event.target.value)} />
          </Form.Group>
          {step === 'verify' && (
            <Form.Group className="mb-3" controlId="unlock-otp">
              <Form.Label>OTP code</Form.Label>
              <Form.Control type="text" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}"
                maxLength={6} required value={otp} disabled={busy}
                onChange={(event) => setOtp(event.target.value.replace(/\D/g, ''))} />
            </Form.Group>
          )}
          {step === 'reset' && (
            <>
              <Form.Group className="mb-3" controlId="unlock-password">
                <Form.Label>New password</Form.Label>
                <Form.Control type="password" autoComplete="new-password" minLength={6} maxLength={72}
                  required value={password} disabled={busy} onChange={(event) => setPassword(event.target.value)} />
              </Form.Group>
              <Form.Group className="mb-3" controlId="unlock-password-confirmation">
                <Form.Label>Confirm new password</Form.Label>
                <Form.Control type="password" autoComplete="new-password" minLength={6} maxLength={72}
                  required value={confirmation} disabled={busy} onChange={(event) => setConfirmation(event.target.value)} />
              </Form.Group>
            </>
          )}
          {step === 'verify' && (
            <Button type="button" variant="link" className="p-0" disabled={busy || cooldown > 0}
              onClick={() => perform(sendOtp)}>
              {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
            </Button>
          )}
          {step === 'reset' && (
            <Button type="button" variant="link" className="p-0" disabled={busy} onClick={() => {
              setStep('request'); setResetToken(''); setPassword(''); setConfirmation(''); setNotice(''); setError('');
            }}>Request a new code</Button>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" variant="secondary" disabled={busy} onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={busy}>
            {busy ? 'Processing...' : step === 'request' ? 'Send OTP' : step === 'verify' ? 'Verify OTP' : 'Save password and unlock'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
