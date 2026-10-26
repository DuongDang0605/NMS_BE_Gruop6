import { useState } from 'react';
import { Table, Button, Modal, Form, InputGroup } from 'react-bootstrap';
import { mockCategories } from '../../mockData/categories';

const CategoryManager = () => {
  const [categories, setCategories] = useState(mockCategories);
  const [showModal, setShowModal] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [currentCategory, setCurrentCategory] = useState({ categoryId: '', categoryName: '', categoryDescription: '', isActive: 1 });
  const [deleteId, setDeleteId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const handleCloseModal = () => setShowModal(false);
  const handleShowModal = (cat = { categoryId: '', categoryName: '', categoryDescription: '', isActive: 1 }) => {
    setCurrentCategory(cat);
    setShowModal(true);
  };

  const handleSave = () => {
    if (currentCategory.categoryId) {
      // Update
      setCategories(categories.map(c => c.categoryId === currentCategory.categoryId ? currentCategory : c));
    } else {
      // Create
      setCategories([...categories, { ...currentCategory, categoryId: Date.now() }]);
    }
    handleCloseModal();
  };

  const handleDeleteConfirm = (id) => {
    setDeleteId(id);
    setShowConfirm(true);
  };

  const deleteCategory = () => {
    setCategories(categories.filter(c => c.categoryId !== deleteId));
    setShowConfirm(false);
  };

  const filteredCategories = categories.filter(c =>
    c.categoryName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Category Management</h2>
        <Button variant="primary" onClick={() => handleShowModal()}>
          <i className="bi bi-plus-circle me-1"></i> Add Category
        </Button>
      </div>

      <InputGroup className="mb-3 w-50">
        <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
        <Form.Control
          placeholder="Search categories..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </InputGroup>

      <Table striped bordered hover>
        <thead className="table-brand">
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Description</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredCategories.map(cat => (
            <tr key={cat.categoryId}>
              <td>{cat.categoryId}</td>
              <td>{cat.categoryName}</td>
              <td>{cat.categoryDescription}</td>
              <td>{cat.isActive ? <span className="badge bg-success">Active</span> : <span className="badge bg-secondary">Inactive</span>}</td>
              <td>
                <Button variant="warning" size="sm" className="me-2" onClick={() => handleShowModal(cat)}>
                  <i className="bi bi-pencil"></i> Edit
                </Button>
                <Button variant="danger" size="sm" onClick={() => handleDeleteConfirm(cat.categoryId)}>
                  <i className="bi bi-trash"></i> Delete
                </Button>
              </td>
            </tr>
          ))}
          {filteredCategories.length === 0 && (
            <tr>
              <td colSpan="5" className="text-center">No categories found.</td>
            </tr>
          )}
        </tbody>
      </Table>

      {/* Create/Update Modal */}
      <Modal show={showModal} onHide={handleCloseModal}>
        <Modal.Header closeButton>
          <Modal.Title>{currentCategory.categoryId ? 'Edit Category' : 'Add Category'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Category Name</Form.Label>
              <Form.Control
                type="text"
                value={currentCategory.categoryName}
                onChange={e => setCurrentCategory({...currentCategory, categoryName: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                value={currentCategory.categoryDescription}
                onChange={e => setCurrentCategory({...currentCategory, categoryDescription: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Check
                type="switch"
                id="active-switch"
                label="Active"
                checked={currentCategory.isActive === 1}
                onChange={e => setCurrentCategory({...currentCategory, isActive: e.target.checked ? 1 : 0})}
              />
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseModal}>Cancel</Button>
          <Button variant="primary" onClick={handleSave}>Save</Button>
        </Modal.Footer>
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal show={showConfirm} onHide={() => setShowConfirm(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>Are you sure you want to delete this category?</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)}>Cancel</Button>
          <Button variant="danger" onClick={deleteCategory}>Delete</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default CategoryManager;
