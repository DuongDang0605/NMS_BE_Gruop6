import { useState } from 'react';
import { Table, Button, Modal, Form, InputGroup } from 'react-bootstrap';
import { mockUsers } from '../../mockData/users';

const UserManager = () => {
  const [users, setUsers] = useState(mockUsers);
  const [showModal, setShowModal] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [currentUser, setCurrentUser] = useState({
    accountId: '', accountName: '', accountEmail: '', accountRole: 2, accountPassword: ''
  });

  const [deleteId, setDeleteId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const handleCloseModal = () => setShowModal(false);
  const handleShowModal = (u = { accountId: '', accountName: '', accountEmail: '', accountRole: 2, accountPassword: '' }) => {
    setCurrentUser(u);
    setShowModal(true);
  };
  const handleSave = () => {
    if (currentUser.accountId) {
      setUsers(users.map(u => u.accountId === currentUser.accountId ? currentUser : u));
    } else {
      setUsers([...users, { ...currentUser, accountId: Date.now() }]);
    }
    handleCloseModal();
  };

  const handleDeleteConfirm = (id) => {
    setDeleteId(id);
    setShowConfirm(true);
  };

  const deleteUser = () => {
    setUsers(users.filter(u => u.accountId !== deleteId));
    setShowConfirm(false);
  };

  const filteredUsers = users.filter(u =>
    u.accountName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    u.accountEmail.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>User Management</h2>
        <Button variant="primary" onClick={() => handleShowModal()}>
          <i className="bi bi-plus-circle me-1"></i> Add User
        </Button>
      </div>

      <InputGroup className="mb-3 w-50">
        <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
        <Form.Control
          placeholder="Search users..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </InputGroup>

      <Table striped bordered hover>
        <thead className="table-brand">
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredUsers.map(u => (
            <tr key={u.accountId}>
              <td>{u.accountId}</td>
              <td>{u.accountName}</td>
              <td>{u.accountEmail}</td>
              <td>{u.accountRole === 1 ? 'Admin' : 'Staff'}</td>
              <td>
                <Button variant="warning" size="sm" className="me-2" onClick={() => handleShowModal(u)}>
                  <i className="bi bi-pencil"></i> Edit
                </Button>
                <Button variant="danger" size="sm" onClick={() => handleDeleteConfirm(u.accountId)}>
                  <i className="bi bi-trash"></i> Delete
                </Button>
              </td>
            </tr>
          ))}
          {filteredUsers.length === 0 && (
            <tr>
              <td colSpan="5" className="text-center">No users found.</td>
            </tr>
          )}
        </tbody>
      </Table>

      <Modal show={showModal} onHide={handleCloseModal}>
        <Modal.Header closeButton>
          <Modal.Title>{currentUser.accountId ? 'Edit User' : 'Add User'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Name</Form.Label>
              <Form.Control
                type="text"
                value={currentUser.accountName}
                onChange={e => setCurrentUser({...currentUser, accountName: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                value={currentUser.accountEmail}
                onChange={e => setCurrentUser({...currentUser, accountEmail: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                value={currentUser.accountPassword}
                onChange={e => setCurrentUser({...currentUser, accountPassword: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Role</Form.Label>
              <Form.Select
                value={currentUser.accountRole}
                onChange={e => setCurrentUser({...currentUser, accountRole: Number(e.target.value)})}
              >
                <option value={1}>Admin</option>
                <option value={2}>Staff</option>
              </Form.Select>
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseModal}>Cancel</Button>
          <Button variant="primary" onClick={handleSave}>Save</Button>
        </Modal.Footer>
      </Modal>

      <Modal show={showConfirm} onHide={() => setShowConfirm(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>Are you sure you want to delete this user?</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)}>Cancel</Button>
          <Button variant="danger" onClick={deleteUser}>Delete</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default UserManager;
