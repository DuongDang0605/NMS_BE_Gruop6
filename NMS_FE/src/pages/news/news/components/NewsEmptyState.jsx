import { Button, Card } from 'react-bootstrap';

const NewsEmptyState = ({ hasActiveFilters, onClear, onCreate }) => (
  <Card className="text-center py-5 border-0 shadow-sm rounded-4">
    <Card.Body>
      <div className="text-muted mb-3" style={{ fontSize: '3rem' }}>
        <i className="bi bi-newspaper"></i>
      </div>
      <h5 className="fw-bold text-dark">Không tìm thấy bài viết nào</h5>
      <p className="text-secondary small mb-3">
        Không có bài viết nào khớp với các điều kiện lọc hiện tại của bạn.
      </p>
      {hasActiveFilters ? (
        <Button variant="outline-primary" size="sm" onClick={onClear}>
          Xóa bộ lọc để xem tất cả
        </Button>
      ) : (
        <Button variant="primary" size="sm" onClick={onCreate}>
          <i className="bi bi-plus-circle me-1"></i> Tạo bài viết đầu tiên
        </Button>
      )}
    </Card.Body>
  </Card>
);

export default NewsEmptyState;
