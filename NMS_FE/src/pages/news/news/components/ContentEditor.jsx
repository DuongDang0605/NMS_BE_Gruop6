import { useRef } from 'react';
import { Form } from 'react-bootstrap';

const TOOLS = [
  { icon: 'bi-type-bold', label: 'In đậm', title: 'Chữ in đậm', prefix: '**', suffix: '**' },
  { icon: 'bi-type-italic', label: 'In nghiêng', title: 'Chữ in nghiêng', prefix: '*', suffix: '*' },
  { icon: 'bi-type-h2', label: 'Tiêu đề mục', title: 'Tiêu đề mục (Heading)', prefix: '\n### ', suffix: '\n' },
  { icon: 'bi-quote', label: 'Trích dẫn', title: 'Trích dẫn', prefix: '\n> ', suffix: '\n' },
  { icon: 'bi-list-ul', label: 'Danh sách', title: 'Danh sách gạch đầu dòng', prefix: '\n- ', suffix: '\n' },
  { icon: 'bi-link', label: 'Chèn Link', title: 'Chèn liên kết', prefix: '[Tên liên kết](', suffix: ')' },
];

const ContentEditor = ({ value, onChange }) => {
  const textareaRef = useRef(null);

  // Chèn cú pháp định dạng nhanh vào nội dung
  const insertFormatting = (prefix, suffix = '') => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const currentVal = value || '';
    const selected = currentVal.substring(start, end);

    onChange(
      currentVal.substring(0, start) + prefix + selected + suffix + currentVal.substring(end)
    );
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + prefix.length, end + prefix.length);
    }, 0);
  };

  return (
    <Form.Group className="mb-3">
      <div className="d-flex justify-content-between align-items-center mb-1">
        <Form.Label className="fw-bold mb-0">
          Nội dung chi tiết bài viết <span className="text-danger">*</span>
        </Form.Label>
        <span className="text-muted small">
          {value.trim() ? value.trim().split(/\s+/).length : 0} từ • {value.length} ký tự
        </span>
      </div>

      <div className="editor-toolbar">
        {TOOLS.map((t) => (
          <button
            type="button"
            key={t.label}
            className="btn-tool"
            onClick={() => insertFormatting(t.prefix, t.suffix)}
            title={t.title}
          >
            <i className={`bi ${t.icon}`}></i> {t.label}
          </button>
        ))}
      </div>

      <Form.Control
        ref={textareaRef}
        as="textarea"
        rows={8}
        required
        className="editor-textarea-with-toolbar"
        placeholder="Viết nội dung đầy đủ của bài báo vào đây..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </Form.Group>
  );
};

export default ContentEditor;
