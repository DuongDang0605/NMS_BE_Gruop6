import { Nav } from "react-bootstrap";
import { NavLink } from "react-router-dom";
import { canAccess } from "../services/roles";

const Sidebar = ({ user }) => {
  return (
    <aside className="app-sidebar">
      <div className="p-3">
        <h5 className="sidebar-title mb-3">Menu</h5>
        <Nav className="sidebar-nav flex-column">
          <Nav.Link as={NavLink} to="/dashboard">
            <i className="bi bi-speedometer2 me-2"></i>Dashboard
          </Nav.Link>

          {canAccess(user, [1, 2]) && (
            <Nav.Link as={NavLink} to="/author/articles">
              <i className="bi bi-journal-text me-2 text-primary"></i>Bài viết
              của tôi
            </Nav.Link>
          )}

          {canAccess(user, [1, 3]) && (
            <Nav.Link as={NavLink} to="/editorial/queue">
              <i className="bi bi-clipboard-check me-2 text-warning"></i>Duyệt
              xuất bản
            </Nav.Link>
          )}

          {canAccess(user, [1, 2, 3]) && (
            <>
              <Nav.Link as={NavLink} to="/category">
                <i className="bi bi-tags me-2"></i>Category
              </Nav.Link>
              <Nav.Link as={NavLink} to="/news">
                <i className="bi bi-newspaper me-2"></i>News
              </Nav.Link>
            </>
          )}

          {canAccess(user, [1]) && (
            <Nav.Link as={NavLink} to="/users">
              <i className="bi bi-people me-2"></i>Users
            </Nav.Link>
          )}

          {canAccess(user, [1, 4]) && (
            <Nav.Link as={NavLink} to="/moderation">
              <i className="bi bi-shield-check me-2"></i>Moderation
            </Nav.Link>
          )}

          <hr />
          <Nav.Link as={NavLink} to="/portal">
            <i className="bi bi-globe me-2 text-primary"></i>Public Portal
          </Nav.Link>
          {canAccess(user, [1]) && (
            <Nav.Link as={NavLink} to="/settings">
              <i className="bi bi-gear me-2"></i>Settings
            </Nav.Link>
          )}
        </Nav>
      </div>
    </aside>
  );
};

export default Sidebar;
