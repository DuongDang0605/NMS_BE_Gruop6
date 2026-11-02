import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Button,
  Badge,
  Modal,
  Form,
  Alert,
  Spinner,
  Pagination,
} from 'react-bootstrap';
import { editorialApi } from '../../api/editorial';
import ScheduleModal from './ScheduleModal';
import AuditTrailTimeline from '../../components/editorial/AuditTrailTimeline';

const EditorialQueue = () => {
  const [queueArticles, setQueueArticles] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Click 1: Review Modal (Preview article full content)
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [selectedArticle, setSelectedArticle] = useState(null);

  // Click 2 -> 3: Request Changes Modal (Mandatory feedback)
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackContent, setFeedbackContent] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const [feedbackError, setFeedbackError] = useState('');

  // Schedule Modal
  const [showScheduleModal, setShowScheduleModal] = useState(false);

  // Audit Trail Modal
  const [showAuditModal, setShowAuditModal] = useState(false);

  // Instant action loading
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadReviewQueue(currentPage);
  }, [currentPage]);

  const loadReviewQueue = async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const data = await editorialApi.getReviewQueue({
        page,
        size: 10,
        sort: 'modifiedDate,asc', // Bài nộp trước được duyệt trước (FIFO)
      });
      setQueueArticles(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải hàng đợi phê duyệt.');
    } finally {
      setLoading(false);
    }
  };

  // Click 1: Mở bài xem xét
  const handleOpenReview = (article) => {
    setSelectedArticle(article);
    setShowReviewModal(true);
  };

  // Click 2 & 3: Duyệt xuất bản ngay
  const handlePublishNow = async (articleId) => {
    if (!window.confirm('Xác nhận duyệt và xuất bản công khai bài viết này ngay lập tức?')) {
      return;
    }

    setActionLoading(true);
    try {
      await editorialApi.publishNow(articleId);
      setSuccessMsg('Đã phê duyệt và xuất bản bài viết thành công!');
      setShowReviewModal(false);
      loadReviewQueue(currentPage);
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể xuất bản bài viết.');
    } finally {
      setActionLoading(false);
    }
  };

  // Mở modal yêu cầu sửa đổi
  const handleOpenRequestChanges = () => {
    setFeedbackContent('');
    setFeedbackError('');
    setShowFeedbackModal(true);
  };

  // Gửi phản hồi yêu cầu sửa đổi (Bắt buộc nội dung)
  const handleSubmitRequestChanges = async (e) => {
    e.preventDefault();
    if (!feedbackContent.trim()) {
      setFeedbackError('Ghi chú phản hồi lý do bắt buộc không được để trống (AC-ED-002)');
      return;
    }

    setSubmittingFeedback(true);
    setFeedbackError('');
    try {
      await editorialApi.requestChanges(selectedArticle.newsArticleId, feedbackContent.trim());
      setSuccessMsg('Đã gửi yêu cầu sửa đổi trả về cho tác giả thành công!');
      setShowFeedbackModal(false);
      setShowReviewModal(false);
      loadReviewQueue(currentPage);
    } catch (err) {
      setFeedbackError(err?.response?.data?.message || 'Không thể gửi yêu cầu sửa đổi.');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  return (
    <div className="container-fluid py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-1">
            <i className="bi bi-clipboard-check me-2 text-warning"></i>
            Hàng đợi Phê duyệt Bài viết (Editorial Review Queue)
          </h2>
          <p className="text-muted mb-0">
            Xem xét, phản hồi yêu cầu chỉnh sửa, duyệt xuất bản ngay hoặc hẹn giờ xuất bản (Editor & Admin)
          </p>
        </div>
        <Button variant="outline-secondary" onClick={() => loadReviewQueue(currentPage)}>
          <i className="bi bi-arrow-clockwise me-1"></i>
          Làm mới hàng đợi
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

      <Card className="shadow-sm border-0">
        <Card.Header className="bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
          <h5 className="mb-0 fw-semibold">
            Danh sách bài viết chờ duyệt
            <Badge bg="warning" text="dark" className="ms-2">
              {queueArticles.length} bài
            </Badge>
          </h5>
          <small className="text-muted">
            <i className="bi bi-info-circle me-1"></i>
            Ưu tiên thứ tự bài nộp trước (FIFO)
          </small>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="warning" />
              <p className="mt-2 text-muted">Đang tải hàng đợi phê duyệt...</p>
            </div>
          ) : queueArticles.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-check2-circle fs-1 text-success d-block mb-2"></i>
              Tuyệt vời! Hiện không còn bài viết nào đang chờ duyệt.
            </div>
          ) : (
            <Table responsive hover className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th style={{ width: '35%' }}>Tiêu đề</th>
                  <th>Tác giả</th>
                  <th>Danh mục</th>
                  <th>Thời gian nộp</th>
                  <th className="text-end">Hành động duyệt</th>
                </tr>
              </thead>
              <tbody>
                {queueArticles.map((article) => (
                  <tr key={article.newsArticleId}>
                    <td>
                      <div className="fw-semibold text-dark">{article.newsTitle}</div>
                      {article.headline && (
                        <small className="text-muted text-truncate d-block" style={{ maxWidth: '380px' }}>
                          {article.headline}
                        </small>
                      )}
                    </td>
                    <td>
                      <i className="bi bi-person-circle me-1 text-secondary"></i>
                      {article.createdByName || 'Tác giả'}
                    </td>
                    <td>
                      <Badge bg="light" text="dark" className="border">
                        {article.categoryName || 'Chưa phân loại'}
                      </Badge>
                    </td>
                    <td>
                      <small className="text-muted">
                        {article.modifiedDate
                          ? new Date(article.modifiedDate).toLocaleString('vi-VN')
                          : new Date(article.createdDate).toLocaleString('vi-VN')}
                      </small>
                    </td>
                    <td className="text-end">
                      {/* Click 1: Nút mở Modal xem xét & phê duyệt trong <= 3 click */}
                      <Button
                        variant="primary"
                        size="sm"
                        className="me-2"
                        onClick={() => handleOpenReview(article)}
                        title="Xem nội dung chi tiết và xét duyệt"
                      >
                        <i className="bi bi-eye me-1"></i>
                        Xem & Duyệt
                      </Button>

                      <Button
                        variant="outline-secondary"
                        size="sm"
                        onClick={() => {
                          setSelectedArticle(article);
                          setShowAuditModal(true);
                        }}
                        title="Xem lịch sử kiểm toán"
                      >
                        <i className="bi bi-clock-history"></i>
                      </Button>
                    </td>
                  </tr>
                ))}
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

      {/* MODAL XEM CHI TIẾT & THAO TÁC DUYỆT (Click 1, 2, 3) */}
      <Modal show={showReviewModal} onHide={() => setShowReviewModal(false)} size="lg" backdrop="static">
        <Modal.Header closeButton>
          <Modal.Title>
            <Badge bg="warning" text="dark" className="me-2">
              PENDING REVIEW
            </Badge>
            Xem xét Phê duyệt Bài viết
          </Modal.Title>
        </Modal.Header>
        <Modal.Body style={{ maxHeight: '70vh', overflowY: 'auto' }}>
          {selectedArticle && (
            <div>
              <h3 className="fw-bold mb-2">{selectedArticle.newsTitle}</h3>

              <div className="d-flex flex-wrap gap-3 text-muted small mb-3 pb-2 border-bottom">
                <span>
                  <strong>Tác giả:</strong> {selectedArticle.createdByName || 'N/A'}
                </span>
                <span>
                  <strong>Danh mục:</strong> {selectedArticle.categoryName || 'N/A'}
                </span>
                <span>
                  <strong>Nguồn:</strong> {selectedArticle.newsSource || 'Nội bộ'}
                </span>
                <span>
                  <strong>Bình luận:</strong> {selectedArticle.allowComments ? 'Cho phép' : 'Khóa'}
                </span>
              </div>

              {selectedArticle.headline && (
                <div className="p-3 bg-light rounded mb-3 border-start border-3 border-primary">
                  <h6 className="fw-semibold mb-1 text-primary">Tóm tắt:</h6>
                  <p className="mb-0 text-dark fst-italic">{selectedArticle.headline}</p>
                </div>
              )}

              {selectedArticle.tags && selectedArticle.tags.length > 0 && (
                <div className="mb-3">
                  <strong className="small text-muted me-2">Thẻ:</strong>
                  {selectedArticle.tags.map((tag) => (
                    <Badge key={tag.tagId} bg="info" className="me-1 text-white">
                      #{tag.tagName}
                    </Badge>
                  ))}
                </div>
              )}

              <div className="p-3 border rounded bg-white mt-3">
                <h6 className="fw-semibold mb-2 text-secondary">Nội dung bài viết:</h6>
                <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.7' }}>
                  {selectedArticle.newsContent}
                </div>
              </div>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer className="d-flex justify-content-between">
          <Button
            variant="outline-secondary"
            onClick={() => {
              setShowAuditModal(true);
            }}
          >
            <i className="bi bi-clock-history me-1"></i>
            Xem Audit Trail
          </Button>

          <div className="d-flex gap-2">
            {/* Click 2 -> Yêu cầu sửa đổi */}
            <Button
              variant="outline-danger"
              onClick={handleOpenRequestChanges}
              disabled={actionLoading}
            >
              <i className="bi bi-arrow-counterclockwise me-1"></i>
              Yêu cầu sửa đổi
            </Button>

            {/* Click 2 -> Hẹn giờ xuất bản */}
            <Button
              variant="info"
              className="text-white"
              onClick={() => setShowScheduleModal(true)}
              disabled={actionLoading}
            >
              <i className="bi bi-calendar-event me-1"></i>
              Hẹn giờ xuất bản
            </Button>

            {/* Click 2 & 3 -> Duyệt xuất bản ngay */}
            <Button
              variant="success"
              onClick={() => handlePublishNow(selectedArticle?.newsArticleId)}
              disabled={actionLoading}
            >
              {actionLoading ? (
                <Spinner size="sm" animation="border" className="me-1" />
              ) : (
                <i className="bi bi-check2-circle me-1"></i>
              )}
              Duyệt xuất bản ngay
            </Button>
          </div>
        </Modal.Footer>
      </Modal>

      {/* MODAL NHẬP GHI CHÚ BẮT BUỘC YÊU CẦU SỬA ĐỔI */}
      <Modal show={showFeedbackModal} onHide={() => setShowFeedbackModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>
            <i className="bi bi-exclamation-triangle text-danger me-2"></i>
            Yêu cầu Tác giả sửa đổi bài viết
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmitRequestChanges}>
          <Modal.Body>
            {feedbackError && <Alert variant="danger">{feedbackError}</Alert>}
            <p className="text-muted small">
              Theo quy định tòa soạn (FR-ED-002 & AC-ED-002), bạn <strong>bắt buộc phải ghi rõ lý do</strong> và hướng dẫn sửa đổi để tác giả có cơ sở hoàn thiện lại bài viết.
            </p>
            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">
                Nội dung hướng dẫn chỉnh sửa <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                placeholder="VD: Cần bổ sung nguồn số liệu trích dẫn ở đoạn 2; Tiêu đề cần đổi ngắn gọn hơn..."
                value={feedbackContent}
                onChange={(e) => setFeedbackContent(e.target.value)}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowFeedbackModal(false)} disabled={submittingFeedback}>
              Hủy
            </Button>
            <Button variant="danger" type="submit" disabled={submittingFeedback || !feedbackContent.trim()}>
              {submittingFeedback ? (
                <>
                  <Spinner size="sm" animation="border" className="me-1" />
                  Đang gửi...
                </>
              ) : (
                <>
                  <i className="bi bi-send me-1"></i>
                  Gửi yêu cầu sửa
                </>
              )}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* MODAL HẸN GIỜ XUẤT BẢN */}
      <ScheduleModal
        show={showScheduleModal}
        onHide={() => setShowScheduleModal(false)}
        article={selectedArticle}
        onSuccess={() => {
          setSuccessMsg('Đã lên lịch xuất bản tự động thành công!');
          setShowReviewModal(false);
          loadReviewQueue(currentPage);
        }}
      />

      {/* MODAL AUDIT TRAIL */}
      <AuditTrailTimeline
        show={showAuditModal}
        onHide={() => setShowAuditModal(false)}
        articleId={selectedArticle?.newsArticleId}
        articleTitle={selectedArticle?.newsTitle}
      />
    </div>
  );
};

export default EditorialQueue;

