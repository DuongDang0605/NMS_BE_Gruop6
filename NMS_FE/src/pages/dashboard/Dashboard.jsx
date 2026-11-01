import { useEffect, useState } from 'react';
import { Alert, Button, Card, Row, Col, Spinner } from 'react-bootstrap';
import { useOutletContext } from 'react-router-dom';
import api from '../../services/api';

const cardsByRole = {
  1: [['totalArticles', 'Total News'], ['totalCategories', 'Total Categories'], ['totalAccounts', 'Total Users']],
  2: [['totalArticles', 'My Articles'], ['activeArticles', 'Published'], ['inactiveArticles', 'Drafts']],
  3: [['totalArticles', 'Editorial Articles'], ['activeArticles', 'Published'], ['inactiveArticles', 'Drafts']],
  4: [['totalNotifications', 'My Notifications'], ['readNotifications', 'Read'], ['unreadNotifications', 'Unread']],
};

export default function Dashboard() {
  const { user } = useOutletContext();
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    api.get('/dashboard/overview', { signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) { setOverview(data); setError(''); } })
      .catch(() => { if (!controller.signal.aborted) setError('Cannot load dashboard data from the server.'); });
    return () => controller.abort();
  }, [attempt]);
  return (
    <div>
      <h2 className="mb-4">Dashboard</h2>
      {user.accountRole === 4 && <p className="text-muted">Your notification summary is available here. Comment moderation metrics will appear when the moderation module is connected.</p>}
      {error && <Alert variant="danger">{error} <Button variant="outline-danger" size="sm" onClick={() => setAttempt((value) => value + 1)}>Retry</Button></Alert>}
      <Row>{(cardsByRole[user.accountRole] || []).map(([key, label]) => (
        <Col md={4} className="mb-3" key={key}>
          <Card className="stat-card h-100"><Card.Body>
            <Card.Title>{label}</Card.Title>
            <Card.Text className="stat-number">{overview?.[key] ?? (error ? '—' : <Spinner animation="border" size="sm" />)}</Card.Text>
          </Card.Body></Card>
        </Col>
      ))}</Row>
    </div>
  );
}
