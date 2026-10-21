import { Nav } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';

const Sidebar = ({ user }) => {
  return (
    <aside className="app-sidebar">
      <div className="p-3">
        <h5 className="sidebar-title mb-3">Menu</h5>
        <Nav className="sidebar-nav flex-column">
          <Nav.Link as={NavLink} to="/dashboard">
            <i className="bi bi-speedometer2 me-2"></i>Dashboard
          </Nav.Link>
          <Nav.Link as={NavLink} to="/category">
            <i className="bi bi-tags me-2"></i>Category
          </Nav.Link>
          <Nav.Link as={NavLink} to="/news">
            <i className="bi bi-newspaper me-2"></i>News
          </Nav.Link>
          {user.accountRole === 1 && <Nav.Link as={NavLink} to="/users">
            <i className="bi bi-people me-2"></i>Users
          </Nav.Link>}
          <hr />
          <Nav.Link as={NavLink} to="/settings">
            <i className="bi bi-gear me-2"></i>Settings
          </Nav.Link>
        </Nav>
      </div>
    </aside>
  );
};

export default Sidebar;
