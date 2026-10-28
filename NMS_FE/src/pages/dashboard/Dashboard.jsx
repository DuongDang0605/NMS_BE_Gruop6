import { useEffect, useState } from 'react';
import { Alert, Button, Card, Row, Col, Spinner, Table, Badge } from 'react-bootstrap';
import { getDashboardOverview, getDashboardEngagement } from '../../api/dashboard';

const StatCard = ({ title, value, icon, colorClass, loading, hasError }) => (
  <Card className={`stat-card ${colorClass} h-100`}>
    <Card.Body className="d-flex align-items-center justify-content-between">
      <div>
        <Card.Title>{title}</Card.Title>
        <Card.Text className="stat-number">
          {loading ? <Spinner animation="border" size="sm" /> : hasError ? '—' : value?.toLocaleString('vi-VN')}
        </Card.Text>
      </div>
      <i className={`bi ${icon} stat-icon`}></i>
    </Card.Body>
  </Card>
);

const Dashboard = () => {
  const [overview, setOverview] = useState(null);
  const [engagement, setEngagement] = useState(null);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let isMounted = true;

    Promise.all([
      getDashboardOverview({ signal: controller.signal }),
      getDashboardEngagement({ signal: controller.signal }),
    ])
      .then(([overviewData, engagementData]) => {
        if (isMounted) {
          setOverview(overviewData);
          setEngagement(engagementData);
          setError('');
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted && isMounted) {
          setError(err?.response?.data?.message || 'Cannot load dashboard data from the server.');
        }
      });

    return () => {
      isMounted = false;
      controller.abort();
    };
  }, [attempt]);

  const loading = (!overview || !engagement) && !error;

  return (
    <div>
      <h2 className="mb-4">Dashboard</h2>
      {error && (
        <Alert variant="danger">
          {error}{' '}
          <Button variant="outline-danger" size="sm" onClick={() => setAttempt((v) => v + 1)}>Retry</Button>
        </Alert>
      )}

      {/* Hàng 1: 3 Card tổng quan hệ thống */}
      <Row className="mb-3">
        <Col md={4} className="mb-3">
          <StatCard
            title="Total News"
            value={overview?.totalArticles}
            icon="bi-newspaper"
            colorClass="stat-card-orange"
            loading={loading}
            hasError={!!error}
          />
        </Col>
        <Col md={4} className="mb-3">
          <StatCard
            title="Total Categories"
            value={overview?.totalCategories}
            icon="bi-tags"
            colorClass="stat-card-blue"
            loading={loading}
            hasError={!!error}
          />
        </Col>
        <Col md={4} className="mb-3">
          <StatCard
            title="Total Users"
            value={overview?.totalAccounts}
            icon="bi-people"
            colorClass="stat-card-green"
            loading={loading}
            hasError={!!error}
          />
        </Col>
      </Row>

      {/* Hàng 2: 2 Card tương tác (Views & Likes từ API thật) */}
      <Row className="mb-4">
        <Col md={6} className="mb-3">
          <StatCard
            title="Total Views"
            value={engagement?.totalViews || 0}
            icon="bi-eye"
            colorClass="stat-card-purple"
            loading={loading}
            hasError={!!error}
          />
        </Col>
        <Col md={6} className="mb-3">
          <StatCard
            title="Total Likes"
            value={engagement?.totalLikes || 0}
            icon="bi-heart-fill"
            colorClass="stat-card-red"
            loading={loading}
            hasError={!!error}
          />
        </Col>
      </Row>

      {/* Bảng Top 5 Trending từ API thật */}
      <Card className="border-0 shadow-sm">
        <Card.Header className="bg-white fw-bold" style={{ borderLeft: '4px solid var(--fpt-orange)', fontSize: '1rem' }}>
          <i className="bi bi-fire me-2 text-danger"></i>Top 5 Bài Báo Nổi Bật (Trending)
        </Card.Header>
        <Card.Body className="p-0">
          <Table hover responsive className="mb-0">
            <thead className="table-light">
              <tr>
                <th style={{ width: 50 }}>#</th>
                <th>Tiêu đề bài viết</th>
                <th style={{ width: 130 }} className="text-center">
                  <i className="bi bi-eye me-1"></i>Views
                </th>
                <th style={{ width: 130 }} className="text-center">
                  <i className="bi bi-heart-fill me-1 text-danger"></i>Likes
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="4" className="text-center py-4">
                    <Spinner animation="border" size="sm" variant="primary" />
                    <span className="ms-2 text-muted">Đang tải bảng xếp hạng...</span>
                  </td>
                </tr>
              ) : engagement?.top5Trending && engagement.top5Trending.length > 0 ? (
                engagement.top5Trending.map((article, index) => (
                  <tr key={article.articleId}>
                    <td>
                      <Badge
                        bg={index === 0 ? 'warning' : index === 1 ? 'secondary' : 'light'}
                        text={index < 2 ? 'dark' : 'dark'}
                        className="border"
                      >
                        {index + 1}
                      </Badge>
                    </td>
                    <td className="fw-semibold">{article.title}</td>
                    <td className="text-center">{article.viewCount?.toLocaleString('vi-VN') || 0}</td>
                    <td className="text-center text-danger fw-semibold">
                      {article.likeCount?.toLocaleString('vi-VN') || 0}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="text-center py-4 text-muted">
                    Chưa có dữ liệu bài báo nổi bật.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
    </div>
  );
};

export default Dashboard;
