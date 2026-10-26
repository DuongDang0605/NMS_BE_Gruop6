import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Col, Collapse, Form, Pagination, Row, Spinner, Table } from 'react-bootstrap';
import { getUsers } from '../../api/user';
import { apiErrorMessage } from './userForm';
import UserModel from './UserModel';

const emptyFilters = () => ({ keyword: '', accountId: '', name: '', email: '', role: '' });
const initialQuery = () => ({ ...emptyFilters(), page: 0, size: 10, sort: 'accountId,asc' });
const filterFields = [
  { key: 'keyword', label: 'Search', placeholder: 'Name or email', maxLength: 254, width: 4 },
  { key: 'accountId', label: 'User ID', placeholder: 'Any ID', type: 'number', min: 1, max: 2147483647, width: 2 },
  { key: 'name', label: 'Name contains', placeholder: 'Any name', maxLength: 100, width: 3 },
  { key: 'email', label: 'Email contains', placeholder: 'e.g. @example.com', maxLength: 254, width: 3 },
];

const UserManager = () => {
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState(emptyFilters);
  const [query, setQuery] = useState(initialQuery);
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState(null);
  const [modal, setModal] = useState(null);
  const [notice, setNotice] = useState('');
  const requestKey = JSON.stringify([query, revision]);
  const loading = result?.key !== requestKey;
  const data = loading ? null : result?.data;
  const listError = loading ? '' : result?.error;
  const busy = !!modal;
  const [sortBy, sortDirection] = query.sort.split(',');

  useEffect(() => {
    const controller = new AbortController();
    const params = Object.fromEntries(Object.entries(query).filter(([, value]) => value !== ''));
    getUsers(params, { signal: controller.signal })
      .then((page) => {
        if (controller.signal.aborted) return;
        const lastPage = Math.max(0, page.totalPages - 1);
        if (query.page > lastPage) {
          setQuery((previous) => ({ ...previous, page: lastPage }));
          return;
        }
        setResult({ key: requestKey, data: page, error: '' });
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setResult({ key: requestKey, data: null, error: apiErrorMessage(error, 'Unable to load users.') });
        }
      });
    return () => controller.abort();
  }, [query, requestKey]);

  const refresh = () => setRevision((value) => value + 1);
  const changeQuery = (field, value) => setQuery((previous) => ({ ...previous, [field]: value, page: 0 }));
  const goToPage = (page) => setQuery((previous) => ({ ...previous, page }));
  const applyFilters = (event) => {
    event.preventDefault();
    setQuery((previous) => ({
      ...previous, ...Object.fromEntries(Object.entries(filters).map(([key, value]) => [key, value.trim()])), page: 0,
    }));
  };
  const resetFilters = () => { setFilters(emptyFilters()); setQuery(initialQuery()); };
  const handleSuccess = (message) => {
    setModal(null);
    setNotice(message);
    refresh();
  };
  const totalPages = data?.totalPages || 0;
  const pageNumbers = Array.from({ length: Math.min(5, totalPages) }, (_, index) =>
    Math.max(0, Math.min(query.page - 2, totalPages - 5)) + index);

  return (
    <div>
      <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-4">
        <h2 className="mb-0">User Management</h2>
        <div className="d-flex gap-2">
          <Button variant="outline-secondary" disabled={loading || busy} onClick={refresh}>Refresh</Button>
          <Button disabled={busy} onClick={() => setModal({ mode: 'create' })}><i className="bi bi-plus-circle me-1" /> Add User</Button>
        </div>
      </div>
      {notice && <Alert variant="success" dismissible onClose={() => setNotice('')}>{notice}</Alert>}
      <Button variant="outline-secondary" className="mb-3" onClick={() => setShowFilters((visible) => !visible)}
        aria-expanded={showFilters} aria-controls="user-search-sort">
        <i className="bi bi-funnel me-2" aria-hidden="true" />
        {showFilters ? 'Hide search & sort' : 'Search & sort'}
        <i className={`bi bi-chevron-${showFilters ? 'up' : 'down'} ms-2`} aria-hidden="true" />
      </Button>
      <Collapse in={showFilters}>
        <div id="user-search-sort">
          <Form onSubmit={applyFilters} className="border rounded p-3 mb-3" aria-label="User filters">
            <Row className="g-3">
              {filterFields.map(({ key, label, width, ...props }) => (
                <Col md={6} lg={width} key={key}>
                  <Form.Group controlId={`user-filter-${key}`}>
                    <Form.Label>{label}</Form.Label>
                    <Form.Control {...props} value={filters[key]}
                      onChange={(event) => setFilters((previous) => ({ ...previous, [key]: event.target.value }))} />
                  </Form.Group>
                </Col>
              ))}
              <Col md={4}>
                <Form.Group controlId="user-filter-role">
                  <Form.Label>Role</Form.Label>
                  <Form.Select value={filters.role} onChange={(event) => setFilters((previous) => ({ ...previous, role: event.target.value }))}>
                    <option value="">All roles</option><option value="1">Admin</option><option value="2">Staff</option>
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col className="d-flex gap-2 align-items-end">
                <Button type="submit" disabled={busy}>Apply filters</Button>
                <Button variant="outline-secondary" onClick={resetFilters} disabled={busy}>Reset</Button>
              </Col>
            </Row>
          </Form>
          <Row className="g-3 mb-3">
            <Col sm={5} md={4}>
              <Form.Group controlId="user-sort-by">
                <Form.Label>Sort by</Form.Label>
                <Form.Select value={sortBy} onChange={(event) => changeQuery('sort', `${event.target.value},${sortDirection}`)} disabled={busy}>
                  <option value="accountId">ID</option><option value="accountName">Name</option>
                  <option value="accountEmail">Email</option><option value="accountRole">Role</option>
                </Form.Select>
              </Form.Group>
            </Col>
            <Col sm={4} md={3}>
              <Form.Group controlId="user-sort-direction">
                <Form.Label>Order</Form.Label>
                <Form.Select value={sortDirection} onChange={(event) => changeQuery('sort', `${sortBy},${event.target.value}`)} disabled={busy}>
                  <option value="asc">Ascending</option><option value="desc">Descending</option>
                </Form.Select>
              </Form.Group>
            </Col>
            <Col sm={3} md={2}>
              <Form.Group controlId="user-page-size">
                <Form.Label>Rows per page</Form.Label>
                <Form.Select value={query.size} onChange={(event) => changeQuery('size', Number(event.target.value))} disabled={busy}>
                  {[5, 10, 20, 50, 100].map((size) => <option key={size} value={size}>{size}</option>)}
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>
        </div>
      </Collapse>
      {listError && <Alert variant="danger">{listError} <Button variant="outline-danger" size="sm" onClick={refresh}>Retry</Button></Alert>}
      <Table striped bordered hover responsive aria-busy={loading}>
        <thead className="table-brand"><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Actions</th></tr></thead>
        <tbody>
          {loading && <tr><td colSpan={5} className="text-center p-4"><Spinner size="sm" animation="border" role="status" /> Loading users...</td></tr>}
          {data?.content.map((user) => (
            <tr key={user.accountId}>
              <td>{user.accountId}</td><td>{user.accountName}</td><td>{user.accountEmail}</td>
              <td><Badge bg={user.accountRole === 1 ? 'primary' : 'secondary'}>{user.accountRole === 1 ? 'Admin' : 'Staff'}</Badge></td>
              <td className="text-nowrap">
                <Button variant="warning" size="sm" className="me-2" disabled={busy} onClick={() => setModal({ mode: 'edit', user })}><i className="bi bi-pencil me-1" />Edit</Button>
                <Button variant="danger" size="sm" disabled={busy} onClick={() => setModal({ mode: 'delete', user })}><i className="bi bi-trash me-1" />Delete</Button>
              </td>
            </tr>
          ))}
          {data?.content.length === 0 && <tr><td colSpan={5} className="text-center p-4">No users match these filters.</td></tr>}
        </tbody>
      </Table>
      {data && (
        <div className="d-flex flex-wrap gap-3 justify-content-between align-items-center">
          <span aria-live="polite">
            Showing {data.totalElements === 0 ? 0 : query.page * query.size + 1}–{Math.min((query.page + 1) * query.size, data.totalElements)} of {data.totalElements} users
            {totalPages > 0 && ` · Page ${query.page + 1} of ${totalPages}`}
          </span>
          <Pagination className="mb-0" aria-label="User pagination">
            <Pagination.First disabled={query.page === 0 || busy} onClick={() => goToPage(0)} />
            <Pagination.Prev disabled={query.page === 0 || busy} onClick={() => goToPage(query.page - 1)} />
            {pageNumbers.map((page) => <Pagination.Item key={page} active={page === query.page} disabled={busy} onClick={() => goToPage(page)}>{page + 1}</Pagination.Item>)}
            <Pagination.Next disabled={query.page >= totalPages - 1 || busy} onClick={() => goToPage(query.page + 1)} />
            <Pagination.Last disabled={query.page >= totalPages - 1 || busy} onClick={() => goToPage(totalPages - 1)} />
          </Pagination>
        </div>
      )}
      {modal && <UserModel key={`${modal.mode}-${modal.user?.accountId ?? 'new'}`} mode={modal.mode}
        user={modal.user} onHide={() => setModal(null)} onSuccess={handleSuccess} />}
    </div>
  );
};

export default UserManager;
