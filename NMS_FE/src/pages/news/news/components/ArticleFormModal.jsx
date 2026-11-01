import { Alert, Button, Form, Modal, Nav, Spinner } from 'react-bootstrap';
import ArticleFormFields from './ArticleFormFields';
import ArticlePreview from './ArticlePreview';

const ArticleFormModal = ({ form, categories, tags, categoryMap, tagMap, actionError, onOpenTagModal }) => {
  const { show, close, formData, setFormData, activeTab, setActiveTab, submitting, toggleTag, save } = form;

  return (
    <Modal show={show} onHide={close} size="lg" backdrop="static" centered>
      <Form onSubmit={save}>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold d-flex align-items-center gap-2">
            <i
              className={`bi ${formData.newsArticleId ? 'bi-pencil-square' : 'bi-plus-circle'} text-primary`}
            ></i>
            {formData.newsArticleId
              ? `Chỉnh sửa bài viết #${formData.newsArticleId}`
              : 'Thêm bài viết tin tức mới'}
          </Modal.Title>
        </Modal.Header>

        <Modal.Body className="p-4">
          {actionError && (
            <Alert variant="danger" className="py-2 mb-3">
              <i className="bi bi-exclamation-circle me-1"></i> {actionError}
            </Alert>
          )}

          <Nav variant="tabs" className="mb-3">
            <Nav.Item>
              <Nav.Link
                active={activeTab === 'write'}
                onClick={() => setActiveTab('write')}
                className="fw-semibold"
              >
                <i className="bi bi-pencil-fill me-1"></i> Soạn thảo nội dung
              </Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link
                active={activeTab === 'preview'}
                onClick={() => setActiveTab('preview')}
                className="fw-semibold"
              >
                <i className="bi bi-eye-fill me-1"></i> Xem trước bài báo
              </Nav.Link>
            </Nav.Item>
          </Nav>

          {activeTab === 'write' ? (
            <ArticleFormFields
              formData={formData}
              setFormData={setFormData}
              categories={categories}
              tags={tags}
              onToggleTag={toggleTag}
              onOpenTagModal={onOpenTagModal}
            />
          ) : (
            <ArticlePreview formData={formData} categoryMap={categoryMap} tagMap={tagMap} />
          )}
        </Modal.Body>

        <Modal.Footer>
          <Button variant="secondary" onClick={close} disabled={submitting}>
            Hủy bỏ
          </Button>
          <Button variant="primary" type="submit" disabled={submitting}>
            {submitting ? (
              <>
                <Spinner animation="border" size="sm" className="me-1" />
                Đang lưu...
              </>
            ) : (
              <>
                <i className="bi bi-save me-1"></i> Lưu bài viết
              </>
            )}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
};

export default ArticleFormModal;
