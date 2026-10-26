import { useState } from 'react';
import { Table, Button, Modal, Form, InputGroup } from 'react-bootstrap';
import { mockNews } from '../../mockData/news';
import { mockCategories } from '../../mockData/categories';

const NewsManager = () => {
  const [news, setNews] = useState(mockNews);
  const [showModal, setShowModal] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [currentNews, setCurrentNews] = useState({
    newsArticleId: '', newsTitle: '', headline: '', newsContent: '',
    newsSource: '', categoryId: '', newsStatus: 1
  });
  const [deleteId, setDeleteId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const handleCloseModal = () => setShowModal(false);
  const handleShowModal = (n = { newsArticleId: '', newsTitle: '', headline: '', newsContent: '', newsSource: '', categoryId: '', newsStatus: 1 }) => {
    setCurrentNews(n);
    setShowModal(true);
  };

  const handleSave = () => {
    if (currentNews.newsArticleId) {
      setNews(news.map(n => n.newsArticleId === currentNews.newsArticleId ? currentNews : n));
    } else {
      setNews([...news, { ...currentNews, newsArticleId: Date.now(), createdDate: new Date().toISOString() }]);
    }
    handleCloseModal();
  };

  const handleDeleteConfirm = (id) => {
    setDeleteId(id);
    setShowConfirm(true);
  };

  const deleteNews = () => {
    setNews(news.filter(n => n.newsArticleId !== deleteId));
    setShowConfirm(false);
  };

  const filteredNews = news.filter(n =>
    n.newsTitle.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>News Article Management</h2>
        <Button variant="primary" onClick={() => handleShowModal()}>
          <i className="bi bi-plus-circle me-1"></i> Add News
        </Button>
      </div>

      <InputGroup className="mb-3 w-50">
        <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
        <Form.Control
          placeholder="Search news by title..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </InputGroup>

      <Table striped bordered hover>
        <thead className="table-brand">
          <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Headline</th>
            <th>Category</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredNews.map(n => {
            const catName = mockCategories.find(c => c.categoryId === Number(n.categoryId))?.categoryName || 'Unknown';
            return (
              <tr key={n.newsArticleId}>
                <td>{n.newsArticleId}</td>
                <td>{n.newsTitle}</td>
                <td>{n.headline}</td>
                <td>{catName}</td>
                <td>{n.newsStatus ? <span className="badge bg-success">Active</span> : <span className="badge bg-secondary">Inactive</span>}</td>
                <td>
                  <Button variant="warning" size="sm" className="me-2" onClick={() => handleShowModal(n)}>
                    <i className="bi bi-pencil"></i> Edit
                  </Button>
                  <Button variant="danger" size="sm" onClick={() => handleDeleteConfirm(n.newsArticleId)}>
                    <i className="bi bi-trash"></i> Delete
                  </Button>
                </td>
              </tr>
            );
          })}
          {filteredNews.length === 0 && (
            <tr>
              <td colSpan="6" className="text-center">No news articles found.</td>
            </tr>
          )}
        </tbody>
      </Table>

      <Modal show={showModal} onHide={handleCloseModal} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>{currentNews.newsArticleId ? 'Edit News' : 'Add News'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>News Title</Form.Label>
              <Form.Control
                type="text"
                value={currentNews.newsTitle}
                onChange={e => setCurrentNews({...currentNews, newsTitle: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Headline</Form.Label>
              <Form.Control
                type="text"
                value={currentNews.headline}
                onChange={e => setCurrentNews({...currentNews, headline: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Category</Form.Label>
              <Form.Select
                value={currentNews.categoryId}
                onChange={e => setCurrentNews({...currentNews, categoryId: e.target.value})}
              >
                <option value="">Select Category...</option>
                {mockCategories.map(c => (
                  <option key={c.categoryId} value={c.categoryId}>{c.categoryName}</option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Source</Form.Label>
              <Form.Control
                type="text"
                value={currentNews.newsSource}
                onChange={e => setCurrentNews({...currentNews, newsSource: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Content</Form.Label>
              <Form.Control
                as="textarea"
                rows={5}
                value={currentNews.newsContent}
                onChange={e => setCurrentNews({...currentNews, newsContent: e.target.value})}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Check
                type="switch"
                id="news-active-switch"
                label="Active"
                checked={currentNews.newsStatus === 1}
                onChange={e => setCurrentNews({...currentNews, newsStatus: e.target.checked ? 1 : 0})}
              />
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
        <Modal.Body>Are you sure you want to delete this news article?</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)}>Cancel</Button>
          <Button variant="danger" onClick={deleteNews}>Delete</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default NewsManager;
