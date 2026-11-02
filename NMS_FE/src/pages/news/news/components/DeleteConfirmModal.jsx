import { Button, Modal, Spinner } from 'react-bootstrap';

const DeleteConfirmModal = ({ show, target, submitting, onHide, onConfirm }) => (
  <Modal show={show} onHide={onHide} centered>
    <Modal.Header closeButton className="bg-danger text-white">
      <Modal.Title className="fw-bold text-white d-flex align-items-center gap-2">
        <i className="bi bi-exclamation-triangle-fill"></i> Xác nhận xóa bài viết
      </Modal.Title>
    </Modal.Header>

    <Modal.Body className="p-4">
      <p className="mb-2">Bạn có chắc chắn muốn xóa bài viết sau không?</p>
      <div className="p-3 bg-light rounded-3 border mb-3">
        <div className="fw-bold text-dark">
          #{target?.newsArticleId}: {target?.newsTitle}
        </div>
        {target?.headline && <div className="text-muted small mt-1">{target.headline}</div>}
      </div>
      <p className="text-danger small mb-0">
        <i className="bi bi-info-circle me-1"></i> Thao tác này sẽ xoá hoàn toàn bài báo khỏi hệ
        thống và không thể hoàn tác!
      </p>
    </Modal.Body>

    <Modal.Footer>
      <Button variant="secondary" onClick={onHide} disabled={submitting}>
        Hủy
      </Button>
      <Button variant="danger" onClick={onConfirm} disabled={submitting}>
        {submitting ? <Spinner animation="border" size="sm" className="me-1" /> : null}
        Xác nhận xóa
      </Button>
    </Modal.Footer>
  </Modal>
);

export default DeleteConfirmModal;
