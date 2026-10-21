import { Navbar, Container, Nav, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import logo from '../assets/fpt-university-logo.png';
import { clearSession } from '../services/session';

const Header = ({ user }) => {
  const navigate = useNavigate();

  const handleLogout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };

  return (
    <Navbar expand="lg" className="app-navbar">
      <Container fluid>
        <Navbar.Brand onClick={() => navigate('/dashboard')} className="app-brand d-flex align-items-center">
          <img
            alt="FPT University"
            src={logo}
            className="brand-logo"
          />
          <span className="brand-title">News Management System</span>
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav" className="justify-content-end">
          <Nav className="align-items-lg-center gap-2">
            <Navbar.Text className="user-copy me-lg-2">
              Signed in as: <span className="user-name">{user.accountName}</span>
            </Navbar.Text>
            <Button className="logout-button" size="sm" onClick={handleLogout}>
              <i className="bi bi-box-arrow-right me-1"></i>Logout
            </Button>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default Header;
