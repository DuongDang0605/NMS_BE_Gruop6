import { useEffect, useState } from 'react';
import { Alert, Button, Card, Row, Col, Spinner } from 'react-bootstrap';
import api from '../../services/api';

const Dashboard = () => {
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
      {error && <Alert variant="danger">{error} <Button variant="outline-danger" size="sm" onClick={() => setAttempt((value) => value + 1)}>Retry</Button></Alert>}
      <Row>
        <Col md={4} className="mb-3">
          <Card className="stat-card stat-card-orange h-100">
            <Card.Body className="d-flex align-items-center justify-content-between">
              <div>
                <Card.Title>Total News</Card.Title>
                <Card.Text className="stat-number">{overview?.totalArticles ?? (error ? '—' : <Spinner animation="border" size="sm" />)}</Card.Text>
              </div>
              <i className="bi bi-newspaper stat-icon"></i>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4} className="mb-3">
          <Card className="stat-card stat-card-blue h-100">
            <Card.Body className="d-flex align-items-center justify-content-between">
              <div>
                <Card.Title>Total Categories</Card.Title>
                <Card.Text className="stat-number">{overview?.totalCategories ?? (error ? '—' : <Spinner animation="border" size="sm" />)}</Card.Text>
              </div>
              <i className="bi bi-tags stat-icon"></i>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4} className="mb-3">
          <Card className="stat-card stat-card-green h-100">
            <Card.Body className="d-flex align-items-center justify-content-between">
              <div>
                <Card.Title>Total Users</Card.Title>
                <Card.Text className="stat-number">{overview?.totalAccounts ?? (error ? '—' : <Spinner animation="border" size="sm" />)}</Card.Text>
              </div>
              <i className="bi bi-people stat-icon"></i>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
