import { Card, Row, Col } from 'react-bootstrap';
import { mockNews } from '../../mockData/news';
import { mockCategories } from '../../mockData/categories';
import { mockUsers } from '../../mockData/users';

const Dashboard = () => {
  return (
    <div>
      <h2 className="mb-4">Dashboard</h2>
      <Row>
        <Col md={4} className="mb-3">
          <Card className="stat-card stat-card-orange h-100">
            <Card.Body className="d-flex align-items-center justify-content-between">
              <div>
                <Card.Title>Total News</Card.Title>
                <Card.Text className="stat-number">{mockNews.length}</Card.Text>
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
                <Card.Text className="stat-number">{mockCategories.length}</Card.Text>
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
                <Card.Text className="stat-number">{mockUsers.length}</Card.Text>
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
