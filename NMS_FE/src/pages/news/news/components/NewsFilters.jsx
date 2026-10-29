import { Button, Col, Form, InputGroup, Row } from 'react-bootstrap';

const NewsFilters = ({ filters, categories, tags }) => {
  const {
    searchTerm, setSearchTerm,
    filterCategory, setFilterCategory,
    filterStatus, setFilterStatus,
    filterTag, setFilterTag,
    sortBy, setSortBy,
    viewMode, setViewMode,
    setCurrentPage,
    totalItems,
    hasActiveFilters,
    handleClearFilters,
  } = filters;

  // Đổi bộ lọc thì quay về trang 1
  const onFilterChange = (setter) => (e) => {
    setter(e.target.value);
    setCurrentPage(1);
  };

  return (
    <div className="news-filter-box">
      <Row className="g-3 align-items-center">
        <Col lg={4} md={6}>
          <InputGroup>
            <InputGroup.Text className="bg-light border-end-0">
              <i className="bi bi-search text-muted"></i>
            </InputGroup.Text>
            <Form.Control
              placeholder="Tìm bài viết theo tiêu đề, tóm tắt, tác giả..."
              className="border-start-0 bg-light"
              value={searchTerm}
              onChange={onFilterChange(setSearchTerm)}
            />
            {searchTerm && (
              <Button
                variant="light"
                className="border border-start-0"
                onClick={() => setSearchTerm('')}
              >
                <i className="bi bi-x-lg text-muted"></i>
              </Button>
            )}
          </InputGroup>
        </Col>

        <Col lg={3} md={6}>
          <Form.Select value={filterCategory} onChange={onFilterChange(setFilterCategory)}>
            <option value="">-- Tất cả danh mục ({categories.length}) --</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.categoryName} {c.parentCategoryId ? '(con)' : ''}
              </option>
            ))}
          </Form.Select>
        </Col>

        <Col lg={2} sm={6}>
          <Form.Select value={filterStatus} onChange={onFilterChange(setFilterStatus)}>
            <option value="">-- Tất cả trạng thái --</option>
            <option value="1">Đang hoạt động (Active)</option>
            <option value="0">Đang ẩn / Nháp (Inactive)</option>
          </Form.Select>
        </Col>

        <Col lg={3} sm={6}>
          <Form.Select value={filterTag} onChange={onFilterChange(setFilterTag)}>
            <option value="">-- Tất cả thẻ tag ({tags.length}) --</option>
            {tags.map((t) => (
              <option key={t.tagId} value={t.tagId}>
                #{t.tagName}
              </option>
            ))}
          </Form.Select>
        </Col>
      </Row>

      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 pt-3 mt-3 border-top">
        <div className="d-flex align-items-center gap-2 flex-wrap">
          <span className="text-muted small fw-semibold">
            Tìm thấy <strong className="text-dark">{totalItems}</strong> bài viết:
          </span>

          {hasActiveFilters && (
            <Button
              variant="outline-danger"
              size="sm"
              className="py-0 px-2 rounded-pill d-inline-flex align-items-center gap-1"
              style={{ fontSize: '0.78rem' }}
              onClick={handleClearFilters}
            >
              <i className="bi bi-x-circle"></i> Xoá bộ lọc
            </Button>
          )}
        </div>

        <div className="d-flex align-items-center gap-3">
          <div className="d-flex align-items-center gap-1 text-muted small">
            <span>Sắp xếp:</span>
            <Form.Select
              size="sm"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              style={{ width: 'auto', minWidth: 145 }}
            >
              <option value="newest">Mới nhất trước</option>
              <option value="oldest">Cũ nhất trước</option>
              <option value="title_asc">Tiêu đề (A-Z)</option>
              <option value="title_desc">Tiêu đề (Z-A)</option>
              <option value="id_desc">ID giảm dần</option>
              <option value="id_asc">ID tăng dần</option>
            </Form.Select>
          </div>

          <div className="btn-group btn-group-sm" role="group">
            <Button
              variant={viewMode === 'table' ? 'primary' : 'outline-secondary'}
              onClick={() => setViewMode('table')}
              title="Xem dạng bảng"
            >
              <i className="bi bi-table"></i>
            </Button>
            <Button
              variant={viewMode === 'card' ? 'primary' : 'outline-secondary'}
              onClick={() => setViewMode('card')}
              title="Xem dạng thẻ card"
            >
              <i className="bi bi-grid-fill"></i>
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NewsFilters;
