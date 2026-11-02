import { Outlet, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { Alert, Button, Spinner } from 'react-bootstrap';
import { getCurrentUser } from '../api/auth';
import { getSession, saveUser } from '../services/session';
import Header from './Header';
import Sidebar from './Sidebar';
import Footer from './Footer';

const Layout = () => {
  const [status, setStatus] = useState('loading');
  const [user, setUser] = useState(null);

  useEffect(() => {
    if (!getSession()) return;
    const requestedAccountId = getSession().user.accountId;
    const controller = new AbortController();
    getCurrentUser({ signal: controller.signal })
      .then((data) => {
        if (controller.signal.aborted || getSession()?.user.accountId !== requestedAccountId) return;
        saveUser(data);
        setUser(data);
        setStatus('ready');
      })
      .catch(() => {
        if (!controller.signal.aborted) setStatus('error');
      });
    return () => controller.abort();
  }, []);

  if (!getSession()) {
    return <Navigate to="/login" replace />;
  }

  if (status === 'loading') {
    return <div className="p-4 text-center"><Spinner animation="border" role="status" aria-label="Loading account" /></div>;
  }

  if (status === 'error') {
    return (
      <Alert variant="danger" className="m-4">
        Cannot verify your account. Check the backend connection.
        <Button variant="outline-danger" className="ms-3" onClick={() => window.location.reload()}>Retry</Button>
      </Alert>
    );
  }

  if (user.accountRole === 5) return <Navigate to="/portal" replace />;

  return (
    <div className="app-shell d-flex flex-column min-vh-100">
      <Header user={user} />
      <div className="app-body d-flex flex-grow-1">
        <Sidebar user={user} />
        <main className="app-main flex-grow-1 p-4">
          <Outlet context={{ user }} />
        </main>
      </div>
      <Footer />
    </div>
  );
};

export default Layout;
