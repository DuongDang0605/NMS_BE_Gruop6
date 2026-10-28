import { useEffect, useState, useMemo } from 'react';
import { Table, Button, Modal, Form, InputGroup, Alert, Spinner, Badge } from 'react-bootstrap';
import { getCategories, createCategory, updateCategory, deleteCategory } from '../../api/category';

const DEFAULT_CATEGORY = {
  categoryId: null,
  categoryName: '',
  categoryDescription: '',
  parentCategoryId: null,
  isActive: 1,
};

const CategoryManager = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [currentCategory, setCurrentCategory] = useState(DEFAULT_CATEGORY);
  const [deleteId, setDeleteId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getCategories();
      setCategories(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách danh mục.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCloseModal = () => {
    setShowModal(false);
    setActionError('');
  };

  const handleShowModal = (cat = DEFAULT_CATEGORY) => {
    setCurrentCategory({
      categoryId: cat.categoryId || null,
      categoryName: cat.categoryName || '',
      categoryDescription: cat.categoryDescription || '',
      parentCategoryId: cat.parentCategoryId ?? null,
      isActive: cat.isActive !== undefined ? cat.isActive : 1,
    });
    setActionError('');
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!currentCategory.categoryName.trim()) {
      setActionError('Tên danh mục không được để trống.');
      return;
    }

    try {
      setSubmitting(true);
      setActionError('');
      const payload = {
        categoryName: currentCategory.categoryName.trim(),
        categoryDescription: currentCategory.categoryDescription?.trim() || '',
        parentCategoryId: currentCategory.parentCategoryId ? Number(currentCategory.parentCategoryId) : null,
        isActive: currentCategory.isActive,
      };

      if (currentCategory.categoryId) {
        await updateCategory(currentCategory.categoryId, payload);
      } else {
        await createCategory(payload);
      }
      handleCloseModal();
      await loadData();
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Không thể lưu danh mục. Vui lòng kiểm tra lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteConfirm = (id) => {
    setDeleteId(id);
    setActionError('');
    setShowConfirm(true);
  };

  const handleDelete = async () => {
    try {
      setSubmitting(true);
      await deleteCategory(deleteId);
      setShowConfirm(false);
      setDeleteId(null);
      await loadData();
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Không thể xóa danh mục (có thể do còn danh mục con hoặc bài viết thuộc danh mục này).');
    } finally {
      setSubmitting(false);
    }
  };

  // Tra cứu tên danh mục cha theo ID (ghi nhớ tránh cấp phát lại khi gõ tìm kiếm)
  const categoryMap = useMemo(
    () => new Map(categories.map((c) => [c.categoryId, c.categoryName])),
    [categories]
  );

  const filteredCategories = useMemo(
    () =>
      categories.filter(
        (c) =>
          c.categoryName.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (c.categoryDescription && c.categoryDescription.toLowerCase().includes(searchTerm.toLowerCase()))
      ),
    [categories, searchTerm]
  );

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Category Management</h2>
        <Button variant="primary" onClick={() => handleShowModal()}>
          <i className="bi bi-plus-circle me-1"></i> Add Category
        </Button>
      </div>

      {error && (
        <Alert variant="danger" className="d-flex justify-content-between align-items-center">
          <span>{error}</span>
          <Button variant="outline-danger" size="sm" onClick={loadData}>Thử lại</Button>
        </Alert>
      )}

      {actionError && !showModal && !showConfirm && (
        <Alert variant="danger" dismissible onClose={() => setActionError('')}>
          {actionError}
        </Alert>
      )}

      <InputGroup className="mb-3 w-50">
        <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
        <Form.Control
          placeholder="Search categories..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </InputGroup>

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
          <div className="mt-2 text-muted">Đang tải danh mục...</div>
        </div>
      ) : (
        <Table hover responsive className="align-middle">
          <thead className="table-brand">
            <tr>
              <th style={{ width: 70 }}>ID</th>
              <th>Category Name</th>
              <th>Parent Category</th>
              <th>Description</th>
              <th style={{ width: 110 }} className="text-center">Status</th>
              <th style={{ width: 180 }} className="text-center">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredCategories.map(cat => {
              const parentName = cat.parentCategoryId ? categoryMap.get(cat.parentCategoryId) : null;
              const isSubCategory = !!cat.parentCategoryId;

              return (
                <tr key={cat.categoryId}>
                  <td className="fw-semibold text-muted">{cat.categoryId}</td>
                  <td>
                    {isSubCategory ? (
                      <span className="d-inline-flex align-items-center ps-3">
                        <i className="bi bi-arrow-return-right text-muted me-2"></i>
                        <span className="fw-semibold">{cat.categoryName}</span>
                      </span>
                    ) : (
                      <span className="fw-bold text-dark">
                        <i className="bi bi-folder2-open text-primary me-2"></i>
                        {cat.categoryName}
                      </span>
                    )}
                  </td>
                  <td>
                    {parentName ? (
                      <Badge bg="light" text="dark" className="border">
                        <i className="bi bi-diagram-2 me-1 text-secondary"></i>
                        {parentName}
                      </Badge>
                    ) : (
                      <span className="text-muted small"><em>(Root / Danh mục gốc)</em></span>
                    )}
                  </td>
                  <td className="text-muted small">{cat.categoryDescription || '—'}</td>
                  <td className="text-center">
                    {cat.isActive === 1 ? (
                      <Badge bg="success">Active</Badge>
                    ) : (
                      <Badge bg="secondary">Inactive</Badge>
                    )}
                  </td>
                  <td className="text-center">
                    <Button variant="warning" size="sm" className="me-2" onClick={() => handleShowModal(cat)}>
                      <i className="bi bi-pencil"></i> Edit
                    </Button>
                    <Button variant="danger" size="sm" onClick={() => handleDeleteConfirm(cat.categoryId)}>
                      <i className="bi bi-trash"></i> Delete
                    </Button>
                  </td>
                </tr>
              );
            })}
            {filteredCategories.length === 0 && (
              <tr>
                <td colSpan="6" className="text-center py-4 text-muted">
                  Không tìm thấy danh mục nào.
                </td>
              </tr>
            )}
          </tbody>
        </Table>
      )}

      {/* Create/Update Modal */}
      <Modal show={showModal} onHide={handleCloseModal} backdrop="static">
        <Form onSubmit={handleSave}>
          <Modal.Header closeButton>
            <Modal.Title>{currentCategory.categoryId ? 'Edit Category' : 'Add Category'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {actionError && <Alert variant="danger">{actionError}</Alert>}

            <Form.Group className="mb-3">
              <Form.Label>Category Name <span className="text-danger">*</span></Form.Label>
              <Form.Control
                type="text"
                required
                placeholder="Nhập tên danh mục (vd: Công nghệ, Thể thao...)"
                value={currentCategory.categoryName}
                onChange={e => setCurrentCategory({ ...currentCategory, categoryName: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Parent Category (Danh mục cha)</Form.Label>
              <Form.Select
                value={currentCategory.parentCategoryId || ''}
                onChange={e => setCurrentCategory({
                  ...currentCategory,
                  parentCategoryId: e.target.value ? Number(e.target.value) : null,
                })}
              >
                <option value="">-- Không có (Danh mục gốc / Root) --</option>
                {categories
                  .filter(c => c.categoryId !== currentCategory.categoryId) // Không cho chọn chính nó làm cha
                  .map(c => (
                    <option key={c.categoryId} value={c.categoryId}>
                      {c.categoryName} {c.parentCategoryId ? `(con của ${categoryMap.get(c.parentCategoryId) || ''})` : ''}
                    </option>
                  ))}
              </Form.Select>
              <Form.Text className="text-muted">
                Để trống nếu đây là danh mục cấp cao nhất.
              </Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                placeholder="Mô tả danh mục..."
                value={currentCategory.categoryDescription}
                onChange={e => setCurrentCategory({ ...currentCategory, categoryDescription: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Check
                type="switch"
                id="active-switch"
                label="Active"
                checked={currentCategory.isActive === 1}
                onChange={e => setCurrentCategory({ ...currentCategory, isActive: e.target.checked ? 1 : 0 })}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleCloseModal} disabled={submitting}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? <Spinner animation="border" size="sm" className="me-1" /> : null}
              Save
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal show={showConfirm} onHide={() => setShowConfirm(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {actionError && <Alert variant="danger">{actionError}</Alert>}
          Bạn có chắc chắn muốn xóa danh mục này? Thao tác này không thể hoàn tác nếu danh mục chưa gắn bài viết hay danh mục con.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)} disabled={submitting}>Cancel</Button>
          <Button variant="danger" onClick={handleDelete} disabled={submitting}>
            {submitting ? <Spinner animation="border" size="sm" className="me-1" /> : null}
            Delete
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default CategoryManager;
