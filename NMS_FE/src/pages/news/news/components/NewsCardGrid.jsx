import { Badge, Button, Col, Row } from 'react-bootstrap';
import { getArticleTags } from '../utils';

const NewsCardGrid = ({ articles, categoryMap, tagMap, onView, onEdit, onToggleStatus, onDelete }) => (
  <Row className="g-3">
    {articles.map((item) => {
      const catName =
        categoryMap.get(item.categoryId) || item.categoryName || 'Chưa phân loại';
      const itemTags = getArticleTags(item, tagMap);

      return (
        <Col md={6} lg={4} key={item.newsArticleId}>
          <div className="news-card-item p-3">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <Badge bg="light" text="dark" className="border">
                <i className="bi bi-folder2 text-primary me-1"></i>
                {catName}
              </Badge>

              <Button
                variant={item.newsStatus === 1 ? 'success' : 'secondary'}
                size="sm"
                className="rounded-pill py-0 px-2 fw-semibold"
                style={{ fontSize: '0.72rem' }}
                onClick={(e) => onToggleStatus(item, e)}
              >
                {item.newsStatus === 1 ? 'Active' : 'Inactive'}
              </Button>
            </div>

            <h6
              className="clickable-row-title fw-bold mb-2 text-truncate-2"
              style={{ minHeight: '2.5rem' }}
              onClick={() => onView(item)}
              title={item.newsTitle}
            >
              {item.newsTitle}
            </h6>

            <p
              className="text-secondary small mb-3 flex-grow-1"
              style={{
                lineHeight: 1.5,
                display: '-webkit-box',
                WebkitLineClamp: 3,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
              }}
            >
              {item.headline || item.newsContent || 'Chưa có tóm tắt...'}
            </p>

            <div className="d-flex flex-wrap gap-1 mb-3">
              {itemTags.slice(0, 3).map((t) => (
                <span key={t.tagId} className="news-tag-badge">
                  #{t.tagName}
                </span>
              ))}
              {itemTags.length > 3 && (
                <span className="text-muted small">+{itemTags.length - 3}</span>
              )}
            </div>

            <div className="d-flex justify-content-between align-items-center pt-2 border-top text-muted small">
              <span className="text-truncate" style={{ maxWidth: 120 }}>
                <i className="bi bi-person me-1"></i>
                {item.createdByName || 'Tác giả'}
              </span>

              <div className="d-flex gap-1">
                <Button variant="light" size="sm" className="py-0 px-2 border" onClick={() => onView(item)} title="Xem chi tiết">
                  <i className="bi bi-eye text-primary"></i>
                </Button>
                <Button variant="light" size="sm" className="py-0 px-2 border" onClick={() => onEdit(item)} title="Sửa">
                  <i className="bi bi-pencil text-warning"></i>
                </Button>
                <Button variant="light" size="sm" className="py-0 px-2 border" onClick={(e) => onDelete(item, e)} title="Xoá">
                  <i className="bi bi-trash text-danger"></i>
                </Button>
              </div>
            </div>
          </div>
        </Col>
      );
    })}
  </Row>
);

export default NewsCardGrid;
