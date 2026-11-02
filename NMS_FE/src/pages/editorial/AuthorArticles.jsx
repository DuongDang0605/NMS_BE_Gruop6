import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Button,
  Badge,
  Nav,
  Modal,
  Form,
  Alert,
  Spinner,
  Pagination,
  Row,
  Col,
} from 'react-bootstrap';
import { authorApi } from '../../api/editorial';
import { getCategories } from '../../api/category';
import { getTags } from '../../api/tag';
import AuditTrailTimeline from '../../components/editorial/AuditTrailTimeline';

const STATUS_BADGES = {
  DRAFT: { label: 'Bản nháp', bg: 'secondary' },
  PENDING_REVIEW: { label: 'Chờ duyệt', bg: 'warning' },
  CHANGES_REQUESTED: { label: 'Yêu cầu sửa', bg: 'danger' },
  APPROVED: { label: 'Đã duyệt', bg: 'info' },
  SCHEDULED: { label: 'Đã lên lịch', bg: 'primary' },
  PUBLISHED: { label: 'Đã xuất bản', bg: 'success' },
  ARCHIVED: { label: 'Lưu trữ', bg: 'dark' },
};

const AuthorArticles = () => {
  const [activeTab, setActiveTab] = useState('ALL');
  const [articles, setArticles] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Dropdown data
  const [categories, setCategories] = useState([]);
  const [allTags, setAllTags] = useState([]);

  // Modal Article Form (Create / Edit)
  const [showFormModal, setShowFormModal] = useState(false);
  const [editingArticleId, setEditingArticleId] = useState(null);
  const [formData, setFormData] = useState({
    newsTitle: '',
    headline: '',
    newsContent: '',
    newsSource: '',
    categoryId: '',
    tagIds: [],
    allowComments: true,
  });
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  // Modal Feedback Notes
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackNotes, setFeedbackNotes] = useState([]);
  const [feedbackLoading, setFeedbackLoading] = useState(false);

  // Modal Audit Trail
  const [showAuditModal, setShowAuditModal] = useState(false);
  const [selectedArticleForAudit, setSelectedArticleForAudit] = useState(null);

  useEffect(() => {
    loadCategoriesAndTags();
  }, []);

  useEffect(() => {
    loadArticles(currentPage, activeTab);
  }, [currentPage, activeTab]);

  const loadCategoriesAndTags = async () => {
    try {
      const [cats, tags] = await Promise.all([getCategories(), getTags()]);
      setCategories(cats || []);
      setAllTags(tags || []);
    } catch (e) {
      console.error('Lỗi tải danh mục/thẻ:', e);
    }
  };

  const loadArticles = async (page = 0, status = 'ALL') => {
    setLoading(true);
    setError('');
    try {
      const params = {
        page,
        size: 10,
        sort: 'modifiedDate,desc',
      };
      if (status !== 'ALL') {
        params.status = status;
      }
      const data = await authorApi.getMyArticles(params);
      setArticles(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách bài viết');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenCreate = () => {
    setEditingArticleId(null);
    setFormData({
      newsTitle: '',
      headline: '',
      newsContent: '',
      newsSource: '',
      categoryId: categories.length > 0 ? categories[0].categoryId : '',
      tagIds: [],
      allowComments: true,
    });
    setFormError('');
    setShowFormModal(true);
  };

  const handleOpenEdit = (article) => {
    setEditingArticleId(article.newsArticleId);
    setFormData({
      newsTitle: article.newsTitle || '',
      headline: article.headline || '',
      newsContent: article.newsContent || '',
      newsSource: article.newsSource || '',
      categoryId: article.categoryId || '',
      tagIds: article.tagIds || [],
      allowComments: article.allowComments !== false,
    });
    setFormError('');
    setShowFormModal(true);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!formData.newsTitle.trim()) {
      setFormError('Tiêu đề bài viết không được để trống');
      return;
    }
    if (!formData.newsContent.trim()) {
      setFormError('Nội dung bài viết không được để trống');
      return;
    }
    if (!formData.categoryId) {
      setFormError('Vui lòng chọn danh mục');
      return;
    }

    setFormSubmitting(true);
    setFormError('');
    try {
      const payload = {
        newsTitle: formData.newsTitle.trim(),
        headline: formData.headline.trim(),
        newsContent: formData.newsContent.trim(),
        newsSource: formData.newsSource.trim(),
        categoryId: Number(formData.categoryId),
        tagIds: formData.tagIds,
        allowComments: formData.allowComments,
      };

      if (editingArticleId) {
        await authorApi.updateDraft(editingArticleId, payload);
        setSuccessMsg('Cập nhật bài viết thành công!');
      } else {
        await authorApi.createDraft(payload);
        setSuccessMsg('Lưu bản nháp mới thành công!');
      }
      setShowFormModal(false);
      loadArticles(currentPage, activeTab);
    } catch (err) {
      setFormError(err?.response?.data?.message || 'Có lỗi xảy ra khi lưu bài viết');
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleSubmitForReview = async (articleId) => {
    if (!window.confirm('Bạn có chắc chắn muốn nộp bài viết này để ban biên tập phê duyệt? Sau khi nộp, bạn sẽ không thể chỉnh sửa bài viết cho đến khi có phản hồi.')) {
      return;
    }

    try {
      await authorApi.submitForReview(articleId);
      setSuccessMsg('Đã nộp bài viết phê duyệt thành công! Bài viết đã chuyển sang hàng đợi của Biên tập viên.');
      loadArticles(currentPage, activeTab);
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể nộp bài viết. Vui lòng thử lại.');
    }
  };

  const handleViewFeedback = async (articleId) => {
    setShowFeedbackModal(true);
    setFeedbackLoading(true);
    try {
      const notes = await authorApi.getFeedbackNotes(articleId);
      setFeedbackNotes(notes || []);
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể tải phản hồi của ban biên tập');
    } finally {
      setFeedbackLoading(false);
    }
  };

  const handleDeleteDraft = async (articleId) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa bản nháp này? Hành động này không thể hoàn tác.')) {
      return;
    }

    try {
      await authorApi.deleteDraft(articleId);
      setSuccessMsg('Đã xóa bản nháp thành công!');
      loadArticles(currentPage, activeTab);
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể xóa bản nháp.');
    }
  };

  const handleToggleTag = (tagId) => {
    setFormData((prev) => {
      const exists = prev.tagIds.includes(tagId);
      return {
        ...prev,
        tagIds: exists ? prev.tagIds.filter((id) => id !== tagId) : [...prev.tagIds, tagId],
      };
    });
  };

  return (
    <div className="container-fluid py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-1">
            <i className="bi bi-journal-text me-2 text-primary"></i>
            Quản lý Bài viết Tác giả
          </h2>
          <p className="text-muted mb-0">Không gian soạn thảo, quản lý bản nháp và nộp duyệt bài viết (Contributor Workspace)</p>
        </div>
        <Button variant="primary" onClick={handleOpenCreate}>
          <i className="bi bi-pencil-square me-2"></i>
          Soạn bài mới
        </Button>
      </div>

      {successMsg && (
        <Alert variant="success" dismissible onClose={() => setSuccessMsg('')}>
          <i className="bi bi-check-circle me-2"></i>
          {successMsg}
        </Alert>
      )}

      {error && (
        <Alert variant="danger" dismissible onClose={() => setError('')}>
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      {/* Tabs Bộ lọc trạng thái */}
      <Card className="shadow-sm mb-4 border-0">
        <Card.Header className="bg-white border-bottom-0 pt-3">
          <Nav variant="pills" activeKey={activeTab} onSelect={(k) => { setActiveTab(k); setCurrentPage(0); }}>
            <Nav.Item>
              <Nav.Link eventKey="ALL">Tất cả</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="DRAFT">Bản nháp (Draft)</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="PENDING_REVIEW">Chờ duyệt (Pending)</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="CHANGES_REQUESTED">Yêu cầu sửa (Changes Requested)</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="PUBLISHED">Đã xuất bản (Published)</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="ARCHIVED">Lưu trữ (Archived)</Nav.Link>
            </Nav.Item>
          </Nav>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
              <p className="mt-2 text-muted">Đang tải danh sách bài viết...</p>
            </div>
          ) : articles.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-inbox fs-1 d-block mb-2"></i>
              Không tìm thấy bài viết nào trong danh mục này.
            </div>
          ) : (
            <Table responsive hover className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th style={{ width: '40%' }}>Tiêu đề</th>
                  <th>Danh mục</th>
                  <th>Trạng thái</th>
                  <th>Ngày cập nhật</th>
                  <th className="text-end">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {articles.map((article) => {
                  const badgeInfo = STATUS_BADGES[article.newsStatus] || { label: article.newsStatus, bg: 'secondary' };
                  const canEdit = article.newsStatus === 'DRAFT' || article.newsStatus === 'CHANGES_REQUESTED';
                  const canSubmit = article.newsStatus === 'DRAFT' || article.newsStatus === 'CHANGES_REQUESTED';
                  const isChangesRequested = article.newsStatus === 'CHANGES_REQUESTED';

                  return (
                    <tr key={article.newsArticleId}>
                      <td>
                        <div className="fw-semibold text-dark">{article.newsTitle}</div>
                        {article.headline && (
                          <small className="text-muted text-truncate d-block" style={{ maxWidth: '400px' }}>
                            {article.headline}
                          </small>
                        )}
                      </td>
                      <td>
                        <Badge bg="light" text="dark" className="border">
                          {article.categoryName || 'Chưa phân loại'}
                        </Badge>
                      </td>
                      <td>
                        <Badge bg={badgeInfo.bg}>{badgeInfo.label}</Badge>
                      </td>
                      <td>
                        <small className="text-muted">
                          {article.modifiedDate
                            ? new Date(article.modifiedDate).toLocaleString('vi-VN')
                            : new Date(article.createdDate).toLocaleString('vi-VN')}
                        </small>
                      </td>
                      <td className="text-end">
                        {isChangesRequested && (
                          <Button
                            variant="outline-danger"
                            size="sm"
                            className="me-2"
                            onClick={() => handleViewFeedback(article.newsArticleId)}
                            title="Xem lý do yêu cầu sửa đổi từ Editor"
                          >
                            <i className="bi bi-chat-dots me-1"></i>
                            Phản hồi
                          </Button>
                        )}

                        {canEdit && (
                          <Button
                            variant="outline-primary"
                            size="sm"
                            className="me-2"
                            onClick={() => handleOpenEdit(article)}
                            title="Chỉnh sửa bài viết"
                          >
                            <i className="bi bi-pencil me-1"></i>
                            Sửa
                          </Button>
                        )}

                        {canSubmit && (
                          <Button
                            variant="success"
                            size="sm"
                            className="me-2 text-white"
                            onClick={() => handleSubmitForReview(article.newsArticleId)}
                            title="Nộp duyệt lên Ban biên tập"
                          >
                            <i className="bi bi-send me-1"></i>
                            Nộp duyệt
                          </Button>
                        )}

                        <Button
                          variant="outline-secondary"
                          size="sm"
                          className="me-2"
                          onClick={() => {
                            setSelectedArticleForAudit(article);
                            setShowAuditModal(true);
                          }}
                          title="Lịch sử kiểm toán"
                        >
                          <i className="bi bi-clock-history"></i>
                        </Button>

                        {article.newsStatus === 'DRAFT' && (
                          <Button
                            variant="outline-danger"
                            size="sm"
                            onClick={() => handleDeleteDraft(article.newsArticleId)}
                            title="Xóa bản nháp"
                          >
                            <i className="bi bi-trash"></i>
                          </Button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </Table>
          )}
        </Card.Body>

        {totalPages > 1 && (
          <Card.Footer className="bg-white d-flex justify-content-center py-3 border-top-0">
            <Pagination className="mb-0">
              <Pagination.Prev
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              />
              {[...Array(totalPages)].map((_, i) => (
                <Pagination.Item
                  key={i}
                  active={i === currentPage}
                  onClick={() => setCurrentPage(i)}
                >
                  {i + 1}
                </Pagination.Item>
              ))}
              <Pagination.Next
                disabled={currentPage === totalPages - 1}
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
              />
            </Pagination>
          </Card.Footer>
        )}
      </Card>

      {/* Modal Soạn thảo bài viết */}
      <Modal show={showFormModal} onHide={() => setShowFormModal(false)} size="lg" backdrop="static">
        <Modal.Header closeButton>
          <Modal.Title>
            <i className="bi bi-pencil-square me-2 text-primary"></i>
            {editingArticleId ? 'Chỉnh sửa bài viết' : 'Tạo mới bản nháp bài viết'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleFormSubmit}>
          <Modal.Body style={{ maxHeight: '75vh', overflowY: 'auto' }}>
            {formError && <Alert variant="danger">{formError}</Alert>}

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">
                Tiêu đề bài viết <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="text"
                placeholder="Nhập tiêu đề tin tức..."
                value={formData.newsTitle}
                onChange={(e) => setFormData({ ...formData, newsTitle: e.target.value })}
                required
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Tóm tắt ngắn (Headline)</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                placeholder="Tóm tắt ngắn gọn nội dung bài viết..."
                value={formData.headline}
                onChange={(e) => setFormData({ ...formData, headline: e.target.value })}
              />
            </Form.Group>

            <Row className="mb-3">
              <Col md={6}>
                <Form.Group>
                  <Form.Label className="fw-semibold">
                    Danh mục <span className="text-danger">*</span>
                  </Form.Label>
                  <Form.Select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                    required
                  >
                    <option value="">-- Chọn danh mục --</option>
                    {categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.categoryName}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className="fw-semibold">Nguồn tin tức</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="VD: Phòng Đào tạo FPT, VnExpress..."
                    value={formData.newsSource}
                    onChange={(e) => setFormData({ ...formData, newsSource: e.target.value })}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold d-block">Gắn thẻ (Tags)</Form.Label>
              <div className="d-flex flex-wrap gap-2">
                {allTags.map((tag) => {
                  const selected = formData.tagIds.includes(tag.tagId);
                  return (
                    <Button
                      key={tag.tagId}
                      size="sm"
                      variant={selected ? 'primary' : 'outline-secondary'}
                      onClick={() => handleToggleTag(tag.tagId)}
                      type="button"
                    >
                      <i className={`bi ${selected ? 'bi-check-lg' : 'bi-plus'} me-1`}></i>
                      {tag.tagName}
                    </Button>
                  );
                })}
              </div>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">
                Nội dung bài viết <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                as="textarea"
                rows={10}
                placeholder="Nhập nội dung chi tiết bài viết..."
                value={formData.newsContent}
                onChange={(e) => setFormData({ ...formData, newsContent: e.target.value })}
                required
              />
            </Form.Group>

            <Form.Check
              type="switch"
              id="allow-comments-switch"
              label="Cho phép độc giả bình luận vào bài viết này"
              checked={formData.allowComments}
              onChange={(e) => setFormData({ ...formData, allowComments: e.target.checked })}
            />
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowFormModal(false)} disabled={formSubmitting}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={formSubmitting}>
              {formSubmitting ? (
                <>
                  <Spinner size="sm" animation="border" className="me-1" />
                  Đang lưu...
                </>
              ) : (
                <>
                  <i className="bi bi-save me-1"></i>
                  {editingArticleId ? 'Lưu thay đổi' : 'Lưu bản nháp'}
                </>
              )}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal Xem Phản hồi của Editor */}
      <Modal show={showFeedbackModal} onHide={() => setShowFeedbackModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>
            <i className="bi bi-exclamation-circle text-danger me-2"></i>
            Phản hồi từ Ban biên tập
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {feedbackLoading ? (
            <div className="text-center py-4">
              <Spinner animation="border" variant="danger" />
              <p className="mt-2 text-muted">Đang tải phản hồi...</p>
            </div>
          ) : feedbackNotes.length === 0 ? (
            <div className="text-center py-4 text-muted">Không có ghi chú phản hồi nào.</div>
          ) : (
            <div>
              <p className="text-muted small mb-3">
                Ban biên tập yêu cầu bạn chỉnh sửa lại các nội dung sau trước khi được phê duyệt:
              </p>
              {feedbackNotes.map((note) => (
                <div key={note.noteId} className="card mb-3 border-danger shadow-sm">
                  <div className="card-header bg-danger bg-opacity-10 py-2 d-flex justify-content-between align-items-center">
                    <strong className="text-danger">
                      <i className="bi bi-person-badge me-1"></i>
                      {note.editorName}
                    </strong>
                    <small className="text-muted">
                      {new Date(note.createdDate).toLocaleString('vi-VN')}
                    </small>
                  </div>
                  <div className="card-body py-2">
                    <p className="mb-0 text-dark" style={{ whiteSpace: 'pre-wrap' }}>
                      {note.feedbackContent}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowFeedbackModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Modal Lịch sử Kiểm toán */}
      <AuditTrailTimeline
        show={showAuditModal}
        onHide={() => setShowAuditModal(false)}
        articleId={selectedArticleForAudit?.newsArticleId}
        articleTitle={selectedArticleForAudit?.newsTitle}
      />
    </div>
  );
};

export default AuthorArticles;

