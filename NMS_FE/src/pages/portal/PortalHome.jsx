import { useEffect, useState } from 'react';
import { Row, Col, Card, Form, InputGroup, Button, Badge, Spinner, Alert, Pagination, Modal } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { getPublishedArticles } from '../../api/portal';
import { getCategories } from '../../api/category';

const VISIBLE_CATEGORY_LIMIT = 5; // Số danh mục hiển thị nhanh trên thanh

const PortalHome = () => {
  const navigate = useNavigate();

  const [articles, setArticles] = useState([]);
  const [categories, setCategories] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [keyword, setKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Quản lý Modal Popup "Tất cả chuyên mục"
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [categoryModalSearch, setCategoryModalSearch] = useState('');

  // Tải danh mục
  useEffect(() => {
    getCategories()
      .then((data) => setCategories(Array.isArray(data) ? data : []))
      .catch(() => setCategories([]));
  }, []);

  // Tải bài viết khi filter / page thay đổi
  const fetchArticles = async (targetPage = page) => {
    try {
      setLoading(true);
      setError('');
      const params = {
        page: targetPage,
        size: 9,
      };
      if (keyword.trim()) params.keyword = keyword.trim();
      if (selectedCategory) params.categoryId = Number(selectedCategory);

      const res = await getPublishedArticles(params);
      setArticles(res?.content || []);
      setTotalPages(res?.totalPages || 0);
      setTotalElements(res?.totalElements || 0);
      setPage(targetPage);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách bài báo.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Debounce tìm kiếm theo từ khóa và đổi category: hoãn gọi API 350ms khi gõ
    const timer = setTimeout(() => {
      fetchArticles(0);
    }, 350);

    return () => clearTimeout(timer);
  }, [keyword, selectedCategory]);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchArticles(0);
  };

  const formatDate = (isoString) => {
    if (!isoString) return 'Mới cập nhật';
    try {
      return new Date(isoString).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
    } catch {
      return isoString;
    }
  };

  // Danh sách hiển thị nhanh (tối đa 5 mục đầu tiên)
  const topCategories = categories.slice(0, VISIBLE_CATEGORY_LIMIT);
  const currentCategoryObj = categories.find((c) => String(c.categoryId) === String(selectedCategory));
  const isSelectedInTop = topCategories.some((c) => String(c.categoryId) === String(selectedCategory));

  // Lọc trong modal popup
  const modalFilteredCategories = categories.filter((c) =>
    c.categoryName.toLowerCase().includes(categoryModalSearch.toLowerCase())
  );

  const selectCategoryAndClose = (catId) => {
    setSelectedCategory(catId);
    setShowCategoryModal(false);
    setCategoryModalSearch('');
  };

  // Tách bài đầu tiên làm Spotlight / Feature khi ở trang 1 và không tìm kiếm
  const featuredArticle = !keyword && !selectedCategory && page === 0 && articles.length > 0 ? articles[0] : null;
  const remainingArticles = featuredArticle ? articles.slice(1) : articles;

  return (
    <div>
      {/* 1. Thanh tìm kiếm hiện đại */}
      <div className="bg-white p-3 p-md-4 rounded-4 shadow-sm border mb-4">
        <Row className="align-items-center g-3">
          <Col md={7}>
            <Form onSubmit={handleSearch}>
              <InputGroup size="lg">
                <InputGroup.Text className="bg-white border-end-0 text-muted ps-3">
                  <i className="bi bi-search"></i>
                </InputGroup.Text>
                <Form.Control
                  type="text"
                  placeholder="Tìm kiếm bài viết, chủ đề, sự kiện..."
                  className="border-start-0 border-end-0 shadow-none ps-2"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                />
                <Button variant="primary" type="submit" className="px-4">
                  Tìm kiếm
                </Button>
              </InputGroup>
            </Form>
          </Col>
          <Col md={5} className="d-flex align-items-center justify-content-md-end text-muted small">
            <span>
              <i className="bi bi-newspaper me-1 text-primary"></i>
              Tổng số <strong>{totalElements}</strong> bài viết đã xuất bản
            </span>
          </Col>
        </Row>

        {/* 2. Thanh lọc Category gọn gàng + Nút Mở Popup */}
        <div className="d-flex flex-wrap align-items-center gap-2 pt-3 mt-3 border-top">
          {/* Nút Xem tất cả */}
          <button
            type="button"
            className={`portal-category-pill ${!selectedCategory ? 'active' : ''}`}
            onClick={() => setSelectedCategory('')}
          >
            Tất cả
          </button>

          {/* Các mục phổ biến nhất (Top 5) */}
          {topCategories.map((cat) => (
            <button
              key={cat.categoryId}
              type="button"
              className={`portal-category-pill ${String(selectedCategory) === String(cat.categoryId) ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat.categoryId)}
            >
              {cat.categoryName}
            </button>
          ))}

          {/* Nếu chuyên mục đang chọn nằm ngoài top 5, hiển thị riêng pill đó */}
          {selectedCategory && !isSelectedInTop && currentCategoryObj && (
            <button
              type="button"
              className="portal-category-pill active d-inline-flex align-items-center gap-1"
              onClick={() => setSelectedCategory('')}
              title="Bấm để bỏ chọn"
            >
              <span>{currentCategoryObj.categoryName}</span>
              <i className="bi bi-x-circle-fill" style={{ fontSize: '0.8rem' }}></i>
            </button>
          )}

          {/* Nút bấm mở Popup chứa tất cả 100+ chuyên mục */}
          <Button
            variant="outline-secondary"
            size="sm"
            className="rounded-pill d-inline-flex align-items-center gap-1 px-3 py-1 fw-semibold ms-auto"
            style={{ fontSize: '0.85rem', borderColor: '#cbd5e1' }}
            onClick={() => setShowCategoryModal(true)}
          >
            <i className="bi bi-grid-3x3-gap-fill text-primary"></i>
            <span>Tất cả chuyên mục ({categories.length})</span>
            <i className="bi bi-chevron-right" style={{ fontSize: '0.75rem' }}></i>
          </Button>
        </div>
      </div>

      {/* Thông báo lỗi nếu có */}
      {error && (
        <Alert variant="danger" className="d-flex justify-content-between align-items-center rounded-3">
          <span>{error}</span>
          <Button variant="outline-danger" size="sm" onClick={() => fetchArticles(page)}>
            Thử lại
          </Button>
        </Alert>
      )}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
          <div className="mt-3 text-muted fw-semibold">Đang cập nhật tin tức mới nhất...</div>
        </div>
      ) : articles.length === 0 ? (
        <Card className="border-0 shadow-sm text-center py-5 rounded-4">
          <Card.Body>
            <div className="text-muted mb-3" style={{ fontSize: '3.5rem' }}>
              <i className="bi bi-journal-text"></i>
            </div>
            <h4 className="fw-bold text-dark">Không tìm thấy bài viết nào</h4>
            <p className="text-secondary">
              Không có bài báo nào phù hợp với bộ lọc hiện tại. Thử xóa từ khóa hoặc chọn chuyên mục khác.
            </p>
            <Button
              variant="outline-primary"
              size="sm"
              onClick={() => {
                setKeyword('');
                setSelectedCategory('');
              }}
            >
              Xem tất cả bài viết
            </Button>
          </Card.Body>
        </Card>
      ) : (
        <>
          {/* 3. Tiêu điểm (Featured Article Spotlight) */}
          {featuredArticle && (
            <Card
              className="portal-card portal-lead-card mb-4 border-0 shadow-sm overflow-hidden"
              onClick={() => navigate(`/portal/articles/${featuredArticle.articleId}`)}
            >
              <Card.Body className="p-4 p-md-5">
                <div className="d-flex align-items-center gap-2 mb-3">
                  <Badge bg="danger" className="px-3 py-1 text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>
                    <i className="bi bi-lightning-fill me-1"></i>Tiêu điểm
                  </Badge>
                  <span className="text-primary fw-semibold small">{featuredArticle.categoryName}</span>
                  <span className="text-muted small">• {formatDate(featuredArticle.publishedDate)}</span>
                </div>

                <h2 className="portal-lead-title mb-3">
                  {featuredArticle.title}
                </h2>

                <p className="text-secondary lead mb-4" style={{ fontSize: '1.1rem', lineHeight: 1.65 }}>
                  {featuredArticle.headline ||
                    (featuredArticle.content ? featuredArticle.content.substring(0, 180) + '...' : '')}
                </p>

                <div className="d-flex justify-content-between align-items-center pt-3 border-top text-muted small flex-wrap gap-2">
                  <div className="d-flex align-items-center gap-2">
                    <i className="bi bi-person-circle fs-5 text-secondary"></i>
                    <span className="fw-semibold text-dark">{featuredArticle.authorName || 'FPT Editorial'}</span>
                  </div>
                  <div className="d-flex gap-4">
                    <span>
                      <i className="bi bi-eye me-1"></i>
                      {featuredArticle.viewCount?.toLocaleString('vi-VN') || 0}
                    </span>
                    <span className="text-danger">
                      <i className="bi bi-heart-fill me-1"></i>
                      {featuredArticle.likeCount?.toLocaleString('vi-VN') || 0}
                    </span>
                  </div>
                </div>
              </Card.Body>
            </Card>
          )}

          {/* 4. Lưới các bài viết còn lại */}
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="fw-bold text-dark m-0 d-flex align-items-center gap-2">
              <span style={{ width: 4, height: 18, background: 'var(--fpt-orange)', borderRadius: 2 }}></span>
              {currentCategoryObj ? `Chuyên mục: ${currentCategoryObj.categoryName}` : 'Tin tức mới cập nhật'}
            </h5>
          </div>

          <Row className="g-4 mb-4">
            {remainingArticles.map((item) => (
              <Col md={6} lg={4} key={item.articleId}>
                <Card
                  className="portal-card h-100 shadow-sm border-0 d-flex flex-column"
                  onClick={() => navigate(`/portal/articles/${item.articleId}`)}
                >
                  <Card.Body className="d-flex flex-column p-4">
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <span className="text-primary fw-bold text-uppercase" style={{ fontSize: '0.75rem' }}>
                        {item.categoryName || 'Tin tức'}
                      </span>
                      <small className="text-muted">{formatDate(item.publishedDate)}</small>
                    </div>

                    <h3 className="portal-card-title mb-2">
                      {item.title}
                    </h3>

                    <p className="text-secondary small mb-4 flex-grow-1" style={{ lineHeight: 1.6 }}>
                      {item.headline || (item.content ? item.content.substring(0, 105) + '...' : '')}
                    </p>

                    <div className="d-flex justify-content-between align-items-center pt-3 border-top text-muted small">
                      <span className="text-truncate" style={{ maxWidth: 130 }}>
                        <i className="bi bi-person me-1"></i>
                        {item.authorName || 'Ban biên tập'}
                      </span>
                      <div className="d-flex gap-3">
                        <span>
                          <i className="bi bi-eye me-1"></i>
                          {item.viewCount?.toLocaleString('vi-VN') || 0}
                        </span>
                        <span className="text-danger">
                          <i className="bi bi-heart-fill me-1"></i>
                          {item.likeCount?.toLocaleString('vi-VN') || 0}
                        </span>
                      </div>
                    </div>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>

          {/* 5. Phân trang */}
          {totalPages > 1 && (
            <div className="d-flex justify-content-center mt-4">
              <Pagination>
                <Pagination.Prev disabled={page === 0} onClick={() => fetchArticles(page - 1)} />
                {Array.from({ length: totalPages }, (_, i) => (
                  <Pagination.Item key={i} active={i === page} onClick={() => fetchArticles(i)}>
                    {i + 1}
                  </Pagination.Item>
                ))}
                <Pagination.Next disabled={page === totalPages - 1} onClick={() => fetchArticles(page + 1)} />
              </Pagination>
            </div>
          )}
        </>
      )}

      {/* 6. MODAL POPUP DUYỆT TẤT CẢ CHUYÊN MỤC (Tối ưu cho cả hàng trăm mục) */}
      <Modal
        show={showCategoryModal}
        onHide={() => setShowCategoryModal(false)}
        size="lg"
        centered
        backdrop="static"
      >
        <Modal.Header closeButton className="border-bottom pb-3">
          <Modal.Title className="fw-bold d-flex align-items-center gap-2" style={{ fontSize: '1.2rem' }}>
            <i className="bi bi-grid-3x3-gap-fill text-primary"></i>
            Tất cả chuyên mục tin tức ({categories.length})
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          {/* Ô tìm kiếm nhanh chuyên mục */}
          <InputGroup className="mb-4">
            <InputGroup.Text className="bg-light border-end-0">
              <i className="bi bi-search text-muted"></i>
            </InputGroup.Text>
            <Form.Control
              type="text"
              placeholder="Gõ để tìm kiếm nhanh chuyên mục..."
              className="border-start-0 bg-light"
              value={categoryModalSearch}
              onChange={(e) => setCategoryModalSearch(e.target.value)}
              autoFocus
            />
            {categoryModalSearch && (
              <Button variant="light" className="border border-start-0" onClick={() => setCategoryModalSearch('')}>
                <i className="bi bi-x-lg text-muted"></i>
              </Button>
            )}
          </InputGroup>

          {/* Lưới hiển thị các chuyên mục dạng Grid */}
          <div style={{ maxHeight: '55vh', overflowY: 'auto', paddingRight: '4px' }}>
            {/* Lựa chọn 'Tất cả' */}
            <div className="mb-3">
              <Button
                variant={!selectedCategory ? 'primary' : 'outline-secondary'}
                size="sm"
                className="w-100 text-start py-2 px-3 fw-bold rounded-3 d-flex justify-content-between align-items-center"
                onClick={() => selectCategoryAndClose('')}
              >
                <span>
                  <i className="bi bi-collection me-2"></i>Tất cả chuyên mục
                </span>
                {!selectedCategory && <i className="bi bi-check-lg"></i>}
              </Button>
            </div>

            <Row className="g-2">
              {modalFilteredCategories.map((cat) => {
                const isSelected = String(selectedCategory) === String(cat.categoryId);
                return (
                  <Col md={6} lg={4} key={cat.categoryId}>
                    <div
                      onClick={() => selectCategoryAndClose(cat.categoryId)}
                      className={`p-3 rounded-3 border d-flex justify-content-between align-items-center ${
                        isSelected
                          ? 'border-primary bg-primary text-white fw-bold shadow-sm'
                          : 'bg-white hover-bg-light text-dark'
                      }`}
                      style={{
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                      }}
                      onMouseEnter={(e) => {
                        if (!isSelected) {
                          e.currentTarget.style.backgroundColor = '#f8fafc';
                          e.currentTarget.style.borderColor = 'var(--fpt-orange)';
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!isSelected) {
                          e.currentTarget.style.backgroundColor = '#ffffff';
                          e.currentTarget.style.borderColor = '#dee2e6';
                        }
                      }}
                    >
                      <div className="d-flex align-items-center gap-2 text-truncate me-2">
                        <i
                          className={`bi ${cat.parentCategoryId ? 'bi-tag' : 'bi-folder2'} ${
                            isSelected ? 'text-white' : 'text-primary'
                          }`}
                        ></i>
                        <span className="text-truncate" title={cat.categoryName}>
                          {cat.categoryName}
                        </span>
                      </div>
                      {isSelected ? (
                        <i className="bi bi-check-circle-fill"></i>
                      ) : (
                        <i className="bi bi-arrow-right-short text-muted"></i>
                      )}
                    </div>
                  </Col>
                );
              })}
            </Row>

            {modalFilteredCategories.length === 0 && (
              <div className="text-center py-5 text-muted">
                <i className="bi bi-search fs-2 d-block mb-2 text-secondary"></i>
                Không tìm thấy chuyên mục nào với từ khóa "{categoryModalSearch}".
              </div>
            )}
          </div>
        </Modal.Body>
        <Modal.Footer className="border-top bg-light d-flex justify-content-between">
          <span className="text-muted small">
            Bấm vào bất kỳ chuyên mục nào để lọc tin tức ngay lập tức.
          </span>
          <Button variant="secondary" size="sm" onClick={() => setShowCategoryModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default PortalHome;
