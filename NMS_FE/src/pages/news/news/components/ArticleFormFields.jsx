import { Col, Form, Row } from "react-bootstrap";
import ContentEditor from "./ContentEditor";
import TagPicker from "./TagPicker";
import { normalizeStatus, STATUS_CONFIG, ARTICLE_STATUS } from "../constants";

// Tab "Soạn thảo nội dung"
const ArticleFormFields = ({
  formData,
  setFormData,
  categories,
  tags,
  onToggleTag,
  onOpenTagModal,
}) => {
  const setField = (field) => (e) =>
    setFormData({ ...formData, [field]: e.target.value });

  const currentStatus = normalizeStatus(formData.newsStatus);
  const statusMeta = STATUS_CONFIG[currentStatus] || STATUS_CONFIG.DRAFT;

  return (
    <>
      <Form.Group className="mb-3">
        <div className="d-flex justify-content-between align-items-center">
          <Form.Label className="fw-bold">
            Tiêu đề bài viết <span className="text-danger">*</span>
          </Form.Label>
          <span className="text-muted small">
            {formData.newsTitle.length}/250 ký tự
          </span>
        </div>
        <Form.Control
          type="text"
          required
          maxLength={250}
          placeholder="Nhập tiêu đề bài báo (ví dụ: Thông báo lịch thi, Workshop React...)"
          value={formData.newsTitle}
          onChange={setField("newsTitle")}
        />
      </Form.Group>

      <Row className="g-3 mb-3">
        <Col md={6}>
          <Form.Group>
            <Form.Label className="fw-bold">
              Chuyên mục bài viết <span className="text-danger">*</span>
            </Form.Label>
            <Form.Select
              required
              value={formData.categoryId}
              onChange={setField("categoryId")}
            >
              <option value="">-- Chọn danh mục --</option>
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>
                  {c.categoryName} {c.parentCategoryId ? "(con)" : ""}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>

        <Col md={6}>
          <Form.Group>
            <Form.Label className="fw-bold">
              Nguồn tin / Đơn vị cung cấp
            </Form.Label>
            <Form.Control
              type="text"
              maxLength={250}
              placeholder="Ví dụ: Phòng CTSV, FPT Education, VnExpress..."
              value={formData.newsSource}
              onChange={setField("newsSource")}
            />
          </Form.Group>
        </Col>
      </Row>

      <Form.Group className="mb-3">
        <div className="d-flex justify-content-between align-items-center">
          <Form.Label className="fw-bold">Tóm tắt dẫn đề (Headline)</Form.Label>
          <span className="text-muted small">
            {formData.headline.length}/500 ký tự
          </span>
        </div>
        <Form.Control
          as="textarea"
          rows={2}
          maxLength={500}
          placeholder="Tóm tắt ngắn gọn 1-2 câu về nội dung cốt lõi của bài viết..."
          value={formData.headline}
          onChange={setField("headline")}
        />
      </Form.Group>

      <TagPicker
        tags={tags}
        selectedIds={formData.tagIds}
        onToggle={onToggleTag}
        onOpenTagModal={onOpenTagModal}
      />

      <ContentEditor
        value={formData.newsContent}
        onChange={(newsContent) =>
          setFormData((prev) => ({ ...prev, newsContent }))
        }
      />

      <Form.Group className="mb-2 p-3 border rounded-3 bg-light">
        <Row className="align-items-center">
          <Col md={7}>
            <div className="fw-bold mb-1 d-flex align-items-center gap-2">
              <i className={`bi ${statusMeta.icon} text-${statusMeta.badgeVariant}`}></i>
              Trạng thái bài viết
            </div>
            <div className="text-muted small">
              {currentStatus === ARTICLE_STATUS.PUBLISHED
                ? "Bài viết sẽ được hiển thị công khai trên Cổng bạn đọc (Portal)."
                : `Bài viết ở chế độ "${statusMeta.label}", chỉ người có thẩm quyền/tác giả nhìn thấy.`}
            </div>
          </Col>
          <Col md={5} className="mt-2 mt-md-0">
            <Form.Select
              value={currentStatus}
              onChange={(e) =>
                setFormData({ ...formData, newsStatus: e.target.value })
              }
            >
              {Object.values(STATUS_CONFIG).map((sc) => (
                <option key={sc.key} value={sc.key}>
                  {sc.label} ({sc.key})
                </option>
              ))}
            </Form.Select>
          </Col>
        </Row>
      </Form.Group>
    </>
  );
};

export default ArticleFormFields;
