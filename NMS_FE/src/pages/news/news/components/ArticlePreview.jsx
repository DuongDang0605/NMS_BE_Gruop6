import { Badge } from 'react-bootstrap';

// Tab "Xem trước bài báo"
const ArticlePreview = ({ formData, categoryMap, tagMap }) => (
  <div className="p-4 border rounded-4 bg-white">
    <div className="d-flex justify-content-between align-items-center mb-3">
      <Badge bg="primary" className="text-uppercase px-2 py-1">
        {categoryMap.get(Number(formData.categoryId)) || 'Chuyên mục'}
      </Badge>
      <span className="text-muted small">Nguồn: {formData.newsSource || 'Ban biên tập'}</span>
    </div>

    <h3 className="fw-bold text-dark mb-3">
      {formData.newsTitle || 'Tiêu đề bài viết xem trước'}
    </h3>

    {formData.headline && (
      <p className="lead text-secondary border-start border-3 ps-3 py-1 mb-4">
        {formData.headline}
      </p>
    )}

    <div className="news-preview-content mb-4">
      {formData.newsContent || 'Chưa có nội dung bài viết.'}
    </div>

    {formData.tagIds.length > 0 && (
      <div className="d-flex flex-wrap gap-1 pt-3 border-top">
        {formData.tagIds.map((tId) => (
          <span key={tId} className="news-tag-badge">
            #{tagMap.get(tId)?.tagName || tId}
          </span>
        ))}
      </div>
    )}
  </div>
);

export default ArticlePreview;
