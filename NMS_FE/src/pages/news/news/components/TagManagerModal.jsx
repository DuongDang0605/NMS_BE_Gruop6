import { Alert, Button, Form, Modal, Spinner } from 'react-bootstrap';

const TagManagerModal = ({ tags, manager }) => {
  const {
    show, close,
    name, setName,
    note, setNote,
    error, setError,
    loading,
    handleCreate,
    handleDelete,
  } = manager;

  return (
    <Modal show={show} onHide={close} size="md" centered>
      <Modal.Header closeButton>
        <Modal.Title className="fw-bold d-flex align-items-center gap-2">
          <i className="bi bi-tags text-primary"></i> Quản lý Thẻ (Tags)
        </Modal.Title>
      </Modal.Header>

      <Modal.Body className="p-4">
        {error && (
          <Alert variant="danger" dismissible onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        <Form onSubmit={handleCreate} className="mb-4 p-3 bg-light rounded-3 border">
          <div className="fw-bold mb-2 text-dark">Thêm thẻ tag mới</div>
          <Form.Group className="mb-2">
            <Form.Control
              type="text"
              placeholder="Tên thẻ tag (ví dụ: CNTT, Sự kiện, Tuyển dụng...)"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </Form.Group>
          <Form.Group className="mb-2">
            <Form.Control
              type="text"
              placeholder="Ghi chú về thẻ (tuỳ chọn)..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Form.Group>
          <Button
            variant="primary"
            size="sm"
            type="submit"
            disabled={loading || !name.trim()}
            className="w-100"
          >
            {loading ? (
              <Spinner animation="border" size="sm" />
            ) : (
              <>
                <i className="bi bi-plus-lg me-1"></i> Tạo thẻ tag
              </>
            )}
          </Button>
        </Form>

        <div className="fw-bold mb-2 text-dark d-flex justify-content-between align-items-center">
          <span>Danh sách thẻ hiện có ({tags.length})</span>
        </div>

        <div style={{ maxHeight: 250, overflowY: 'auto' }}>
          {tags.length > 0 ? (
            <ul className="list-group list-group-flush">
              {tags.map((t) => (
                <li
                  key={t.tagId}
                  className="list-group-item d-flex justify-content-between align-items-center px-2 py-2"
                >
                  <div>
                    <span className="fw-bold text-dark">#{t.tagName}</span>
                    {t.note && (
                      <div className="text-muted small" style={{ fontSize: '0.75rem' }}>
                        {t.note}
                      </div>
                    )}
                  </div>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    className="py-0 px-2"
                    onClick={() => handleDelete(t.tagId)}
                    title="Xóa tag này"
                  >
                    <i className="bi bi-trash"></i>
                  </Button>
                </li>
              ))}
            </ul>
          ) : (
            <div className="text-muted text-center py-3 small">
              Chưa có thẻ tag nào trong hệ thống.
            </div>
          )}
        </div>
      </Modal.Body>

      <Modal.Footer>
        <Button variant="secondary" onClick={close}>
          Đóng
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default TagManagerModal;
