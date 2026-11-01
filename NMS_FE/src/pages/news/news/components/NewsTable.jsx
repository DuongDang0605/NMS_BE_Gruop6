import { Badge, Button, Table } from 'react-bootstrap';
import { formatDate, getArticleTags } from '../utils';

const NewsTable = ({ articles, categoryMap, tagMap, onView, onEdit, onToggleStatus, onDelete }) => (
  <div className="bg-white rounded-4 shadow-sm border overflow-hidden">
    <Table hover responsive className="align-middle mb-0">
      <thead className="table-brand">
        <tr>
          <th style={{ width: 65 }}>ID</th>
          <th style={{ minWidth: 260 }}>Bài viết & Tóm tắt</th>
          <th style={{ width: 140 }}>Chuyên mục</th>
          <th style={{ minWidth: 150 }}>Thẻ (Tags)</th>
          <th style={{ width: 150 }}>Tác giả & Ngày tạo</th>
          <th style={{ width: 120 }} className="text-center">Trạng thái</th>
          <th style={{ width: 150 }} className="text-center">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        {articles.map((item) => {
          const catName =
            categoryMap.get(item.categoryId) || item.categoryName || 'Chưa phân loại';
          const itemTags = getArticleTags(item, tagMap);

          return (
            <tr key={item.newsArticleId}>
              <td className="fw-bold text-muted">#{item.newsArticleId}</td>

              <td>
                <div
                  className="fw-bold clickable-row-title mb-1"
                  onClick={() => onView(item)}
                  title="Bấm để xem chi tiết bài viết"
                >
                  {item.newsTitle}
                </div>

                {item.headline ? (
                  <div
                    className="text-muted small text-truncate"
                    style={{ maxWidth: 360 }}
                    title={item.headline}
                  >
                    {item.headline}
                  </div>
                ) : null}

                {item.newsSource ? (
                  <div className="mt-1">
                    <span
                      className="badge bg-light text-secondary border fw-normal"
                      style={{ fontSize: '0.72rem' }}
                    >
                      <i className="bi bi-link-45deg me-1"></i>
                      {item.newsSource}
                    </span>
                  </div>
                ) : null}
              </td>

              <td>
                <Badge bg="light" text="dark" className="border px-2 py-1">
                  <i className="bi bi-folder2 text-primary me-1"></i>
                  {catName}
                </Badge>
              </td>

              <td>
                <div className="d-flex flex-wrap gap-1">
                  {itemTags.length > 0 ? (
                    itemTags.map((t) => (
                      <span key={t.tagId} className="news-tag-badge">
                        #{t.tagName}
                      </span>
                    ))
                  ) : (
                    <span className="text-muted small"><em>—</em></span>
                  )}
                </div>
              </td>

              <td className="small">
                <div className="fw-semibold text-dark">
                  <i className="bi bi-person me-1 text-muted"></i>
                  {item.createdByName || 'Tác giả'}
                </div>
                <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                  {formatDate(item.createdDate)}
                </div>
              </td>

              <td className="text-center">
                <Button
                  variant={item.newsStatus === 1 ? 'outline-success' : 'outline-secondary'}
                  size="sm"
                  className="rounded-pill px-2 py-0 d-inline-flex align-items-center gap-1"
                  style={{ fontSize: '0.78rem' }}
                  onClick={(e) => onToggleStatus(item, e)}
                  title="Bấm để bật/tắt nhanh trạng thái bài viết"
                >
                  <i
                    className={`bi ${
                      item.newsStatus === 1 ? 'bi-check-circle-fill' : 'bi-dash-circle'
                    }`}
                  ></i>
                  <span>{item.newsStatus === 1 ? 'Active' : 'Inactive'}</span>
                </Button>
              </td>

              <td className="text-center">
                <div className="d-flex justify-content-center gap-1">
                  <Button variant="outline-info" size="sm" className="px-2" onClick={() => onView(item)} title="Xem chi tiết">
                    <i className="bi bi-eye"></i>
                  </Button>
                  <Button variant="warning" size="sm" className="px-2" onClick={() => onEdit(item)} title="Chỉnh sửa bài viết">
                    <i className="bi bi-pencil"></i>
                  </Button>
                  <Button variant="danger" size="sm" className="px-2" onClick={(e) => onDelete(item, e)} title="Xóa bài viết">
                    <i className="bi bi-trash"></i>
                  </Button>
                </div>
              </td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  </div>
);

export default NewsTable;
