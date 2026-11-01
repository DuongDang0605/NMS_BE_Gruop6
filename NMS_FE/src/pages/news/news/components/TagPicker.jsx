import { Button, Form } from 'react-bootstrap';

const TagPicker = ({ tags, selectedIds, onToggle, onOpenTagModal }) => (
  <Form.Group className="mb-3">
    <div className="d-flex justify-content-between align-items-center mb-1">
      <Form.Label className="fw-bold mb-0">Gắn thẻ bài viết (Tags)</Form.Label>
      <Button variant="link" size="sm" className="p-0 text-decoration-none" onClick={onOpenTagModal}>
        <i className="bi bi-plus me-1"></i>Tạo thẻ mới
      </Button>
    </div>

    <div className="p-3 border rounded-3 bg-light" style={{ maxHeight: 115, overflowY: 'auto' }}>
      {tags.length > 0 ? (
        <div className="d-flex flex-wrap gap-2">
          {tags.map((t) => {
            const isSelected = selectedIds.includes(t.tagId);
            return (
              <button
                type="button"
                key={t.tagId}
                onClick={() => onToggle(t.tagId)}
                className={`btn btn-sm rounded-pill d-inline-flex align-items-center gap-1 ${
                  isSelected ? 'btn-primary text-white shadow-sm' : 'btn-outline-secondary bg-white'
                }`}
                style={{ fontSize: '0.8rem' }}
              >
                <i className={`bi ${isSelected ? 'bi-check2' : 'bi-hash'}`}></i>
                {t.tagName}
              </button>
            );
          })}
        </div>
      ) : (
        <span className="text-muted small">
          Chưa có thẻ tag nào trong hệ thống.{' '}
          <button type="button" className="btn btn-link btn-sm p-0" onClick={onOpenTagModal}>
            Bấm vào đây để tạo thẻ tag
          </button>
        </span>
      )}
    </div>
  </Form.Group>
);

export default TagPicker;
