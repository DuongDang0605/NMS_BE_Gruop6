import { Badge, Button } from 'react-bootstrap';

const NewsHeader = ({ total, tagCount, onReload, onOpenTags, onExport, onCreate }) => (
  <div className="news-header-section">
    <div>
      <h2 className="m-0 fw-bold d-flex align-items-center gap-2">
        <i className="bi bi-newspaper text-primary"></i>
        Quản lý Bài viết Tin tức
        <Badge bg="secondary" pill className="fs-6 fw-normal">
          {total} bài
        </Badge>
      </h2>
      <p className="text-muted small mt-1 mb-0">
        Biên tập, phê duyệt xuất bản, quản lý chuyên mục và thẻ bài báo.
      </p>
    </div>

    <div className="d-flex align-items-center gap-2 flex-wrap">
      <Button variant="outline-secondary" size="sm" onClick={onReload} title="Tải lại dữ liệu">
        <i className="bi bi-arrow-clockwise me-1"></i> Làm mới
      </Button>

      <Button
        variant="outline-info"
        size="sm"
        onClick={onOpenTags}
        className="text-dark fw-semibold"
      >
        <i className="bi bi-tags me-1"></i> Quản lý Thẻ ({tagCount})
      </Button>

      <Button variant="outline-success" size="sm" onClick={onExport} title="Xuất file CSV báo cáo">
        <i className="bi bi-file-earmark-arrow-down me-1"></i> Xuất CSV
      </Button>

      <Button
        variant="primary"
        className="d-flex align-items-center gap-1 shadow-sm"
        onClick={onCreate}
      >
        <i className="bi bi-plus-circle-fill"></i>
        <span>Thêm bài viết mới</span>
      </Button>
    </div>
  </div>
);

export default NewsHeader;
