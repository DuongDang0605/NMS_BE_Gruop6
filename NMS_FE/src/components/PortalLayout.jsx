import { Navbar, Container, Nav, Button } from 'react-bootstrap';
import { useNavigate, Outlet, Link } from 'react-router-dom';
import logo from '../assets/fpt-university-logo.png';
import Footer from './Footer';
import { getSession } from '../services/session';

const PortalLayout = () => {
  const navigate = useNavigate();
  const session = getSession();

  const todayStr = new Date().toLocaleDateString('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

  return (
    <div className="d-flex flex-column min-vh-100 portal-page">
      {/* 1. Thanh Topbar thời gian & hotline */}
      <div className="portal-topbar">
        <Container className="d-flex justify-content-between align-items-center">
          <div>
            <i className="bi bi-clock me-1 text-warning"></i>
            <span className="text-capitalize">{todayStr}</span>
            <span className="mx-2 text-secondary">|</span>
            <span>Bản tin điện tử FPT University</span>
          </div>
          <div className="d-none d-md-flex gap-3 align-items-center">
            <span>
              <i className="bi bi-geo-alt me-1 text-danger"></i>Hà Nội • TP.HCM • Đà Nẵng • Cần Thơ • Quy Nhơn
            </span>
          </div>
        </Container>
      </div>

      {/* 2. Main Navigation Bar */}
      <Navbar expand="lg" className="portal-header py-2 sticky-top shadow-sm">
        <Container>
          <Navbar.Brand
            onClick={() => navigate('/portal')}
            className="d-flex align-items-center gap-3 py-1"
            style={{ cursor: 'pointer' }}
          >
            <img alt="FPT University" src={logo} style={{ height: 46, objectFit: 'contain' }} />
            <div className="d-flex flex-column ps-2 border-start border-2 border-warning">
              <span className="fw-bold tracking-tight text-dark" style={{ fontSize: '1.2rem', lineHeight: 1.1 }}>
                FPT NEWS
              </span>
              <span className="text-muted text-uppercase" style={{ fontSize: '0.68rem', letterSpacing: '0.12em' }}>
                Cổng thông tin tin tức
              </span>
            </div>
          </Navbar.Brand>

          <Navbar.Toggle aria-controls="portal-navbar" className="border-0 shadow-none" />

          <Navbar.Collapse id="portal-navbar" className="justify-content-end">
            <Nav className="align-items-center gap-2 mt-2 mt-lg-0">
              <Nav.Link as={Link} to="/portal" className="fw-semibold px-3 text-dark">
                <i className="bi bi-newspaper me-1 text-primary"></i>Tin mới
              </Nav.Link>

              {session?.user ? (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => navigate('/dashboard')}
                  className="d-flex align-items-center gap-2 px-3 py-2 ms-lg-2 shadow-sm rounded-pill"
                >
                  <i className="bi bi-speedometer2"></i>
                  <span>Trang quản trị ({session.user.accountName})</span>
                </Button>
              ) : (
                <Button
                  variant="outline-primary"
                  size="sm"
                  onClick={() => navigate('/login')}
                  className="d-flex align-items-center gap-2 px-3 py-2 ms-lg-2 rounded-pill"
                >
                  <i className="bi bi-box-arrow-in-right"></i>
                  <span>Đăng nhập</span>
                </Button>
              )}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      {/* 3. Nội dung trang */}
      <main className="flex-grow-1 py-4">
        <Container>
          <Outlet />
        </Container>
      </main>

      {/* 4. Footer */}
      <Footer />
    </div>
  );
};

export default PortalLayout;
