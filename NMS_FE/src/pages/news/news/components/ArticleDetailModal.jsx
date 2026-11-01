import { Badge, Button, Modal } from 'react-bootstrap';
import { formatDate } from '../utils';
import { normalizeStatus, STATUS_CONFIG } from '../constants';

const ArticleDetailModal = ({ show, article, categoryMap, onHide, onEdit, onOpenPortal }) => {
  const normStatus = article ? normalizeStatus(article.newsStatus) : 'DRAFT';
  const meta = STATUS_CONFIG[normStatus] || STATUS_CONFIG.DRAFT;

  return (
    <Modal show={show} onHide={onHide} size="lg" centered>
      <Modal.Header closeButton>
        <Modal.Title className="fw-bold d-flex align-items-center gap-2">
          <i className="bi bi-file-earmark-text text-primary"></i>
          Chi tiết bài viết #{article?.newsArticleId}
        </Modal.Title>
      </Modal.Header>

      <Modal.Body className="p-4">
        {article && (
          <div>
            <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
              <div className="d-flex align-items-center gap-2">
                <Badge bg="primary" className="px-3 py-1 text-uppercase">
                  {categoryMap.get(article.categoryId) || article.categoryName || 'Tin tức'}
                </Badge>
                <Badge bg={meta.badgeVariant} className="px-2 py-1 d-inline-flex align-items-center gap-1">
                  <i className={`bi ${meta.icon}`}></i>
                  {meta.label}
                </Badge>
              </div>

              <div className="text-muted small">
                <i className="bi bi-clock me-1"></i>
                {formatDate(article.createdDate)}
              </div>
            </div>

            <h2 className="fw-bold text-dark mb-3">{article.newsTitle}</h2>

            {article.headline && (
              <div
                className="p-3 mb-4 rounded-3 border-start border-4 border-warning bg-light"
                style={{ fontStyle: 'italic', color: '#475569' }}
              >
                <strong>Tóm tắt:</strong> {article.headline}
              </div>
            )}

            <div className="d-flex flex-wrap gap-4 py-2 px-3 mb-4 bg-light rounded-3 text-muted small border">
              <div>
                <i className="bi bi-person me-1"></i>
                Tác giả: <strong className="text-dark">{article.createdByName || 'Tác giả'}</strong>
              </div>
              {article.newsSource && (
                <div>
                  <i className="bi bi-link-45deg me-1"></i>
                  Nguồn: <strong className="text-dark">{article.newsSource}</strong>
                </div>
              )}
              {article.modifiedDate && (
                <div>
                  <i className="bi bi-pencil-square me-1"></i>
                  Sửa lần cuối: {formatDate(article.modifiedDate)}
                </div>
              )}
            </div>

            <div className="news-preview-content mb-4 p-2">{article.newsContent}</div>

            {Array.isArray(article.tags) && article.tags.length > 0 && (
              <div className="pt-3 border-top">
                <div className="text-muted small fw-bold mb-2">Thẻ bài viết:</div>
                <div className="d-flex flex-wrap gap-1">
                  {article.tags.map((t) => (
                    <span key={t.tagId} className="news-tag-badge">
                      #{t.tagName}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </Modal.Body>

      <Modal.Footer className="d-flex justify-content-between">
        <Button variant="outline-primary" onClick={onOpenPortal}>
          <i className="bi bi-globe me-1"></i> Xem trên Cổng Đọc Báo
        </Button>

        <div className="d-flex gap-2">
          <Button variant="warning" onClick={onEdit}>
            <i className="bi bi-pencil me-1"></i> Chỉnh sửa bài này
          </Button>
          <Button variant="secondary" onClick={onHide}>
            Đóng
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
};

export default ArticleDetailModal;
