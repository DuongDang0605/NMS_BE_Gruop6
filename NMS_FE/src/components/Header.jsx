import { useEffect, useState } from 'react';
import { Navbar, Container, Nav, Button, Dropdown, Badge, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import logo from '../assets/fpt-university-logo.png';
import { clearSession } from '../services/session';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from '../api/notification';

const Header = ({ user }) => {
  const navigate = useNavigate();

  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [loadingNoti, setLoadingNoti] = useState(false);

  const recipientId = user?.accountId;

  const fetchUnread = () => {
    if (!recipientId) return;
    getUnreadCount(recipientId)
      .then((count) => setUnreadCount(count))
      .catch(() => { });
  };

  useEffect(() => {
    fetchUnread();

    // Chỉ thăm dò (poll) khi tab trình duyệt đang mở (active), tránh tải rác khi tab bị ẩn
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        fetchUnread();
      }
    };

    const interval = setInterval(() => {
      if (document.visibilityState === 'visible') {
        fetchUnread();
      }
    }, 60000);

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [recipientId]);

  const loadNotifications = async () => {
    if (!recipientId) return;
    try {
      setLoadingNoti(true);
      const res = await getNotifications(recipientId, 'all', 0, 8);
      setNotifications(res?.content || []);
    } catch {
      // bỏ qua lỗi
    } finally {
      setLoadingNoti(false);
    }
  };

  const handleMarkOneRead = async (noti) => {
    if (noti.isRead === 1) return;
    try {
      await markAsRead(noti.notificationId, recipientId);
      setNotifications((prev) =>
        prev.map((item) =>
          item.notificationId === noti.notificationId ? { ...item, isRead: 1 } : item
        )
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead(recipientId);
      setNotifications((prev) => prev.map((item) => ({ ...item, isRead: 1 })));
      setUnreadCount(0);
    } catch {
      // ignore
    }
  };

  const handleLogout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };

  const formatNotiTime = (isoString) => {
    if (!isoString) return '';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
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
            <Button
              variant="outline-primary"
              size="sm"
              onClick={() => navigate('/portal')}
              className="me-lg-2 d-flex align-items-center gap-1"
            >
              <i className="bi bi-newspaper"></i> Xem Cổng Đọc Báo
            </Button>

            {/* Chuông thông báo (Notification Bell) */}
            <Dropdown onToggle={(isOpen) => isOpen && loadNotifications()} align="end">
              <Dropdown.Toggle
                variant="light"
                size="sm"
                className="position-relative border-0 bg-transparent text-secondary p-2 me-lg-2"
                style={{ fontSize: '1.25rem', boxShadow: 'none' }}
              >
                <i className="bi bi-bell-fill"></i>
                {unreadCount > 0 && (
                  <Badge
                    bg="danger"
                    pill
                    className="position-absolute top-0 start-100 translate-middle"
                    style={{ fontSize: '0.68rem', padding: '0.28em 0.55em' }}
                  >
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </Badge>
                )}
              </Dropdown.Toggle>

              <Dropdown.Menu style={{ width: 340, maxHeight: 420, overflowY: 'auto' }} className="shadow-sm p-0">
                <div className="p-3 border-bottom d-flex justify-content-between align-items-center bg-light">
                  <span className="fw-bold text-dark small">
                    <i className="bi bi-bell me-1 text-primary"></i>Thông báo ({unreadCount} chưa đọc)
                  </span>
                  {unreadCount > 0 && (
                    <Button
                      variant="link"
                      size="sm"
                      className="p-0 text-decoration-none small"
                      onClick={handleMarkAllRead}
                    >
                      Đọc tất cả
                    </Button>
                  )}
                </div>

                {loadingNoti ? (
                  <div className="text-center py-4">
                    <Spinner animation="border" size="sm" variant="primary" />
                    <div className="text-muted small mt-1">Đang tải thông báo...</div>
                  </div>
                ) : notifications.length === 0 ? (
                  <div className="text-center py-4 text-muted small">
                    <i className="bi bi-chat-square-dots fs-3 d-block text-secondary mb-1"></i>
                    Không có thông báo nào
                  </div>
                ) : (
                  <div>
                    {notifications.map((n) => (
                      <div
                        key={n.notificationId}
                        onClick={() => handleMarkOneRead(n)}
                        className={`p-2 px-3 border-bottom d-flex align-items-start gap-2 ${n.isRead === 0 ? 'bg-light' : 'bg-white'
                          }`}
                        style={{ cursor: 'pointer', transition: 'background-color 0.15s' }}
                      >
                        <i
                          className={`bi ${n.isRead === 0 ? 'bi-circle-fill text-primary' : 'bi-check-circle text-muted'
                            } mt-1`}
                          style={{ fontSize: '0.65rem' }}
                        ></i>
                        <div className="flex-grow-1">
                          <div
                            className={`small ${n.isRead === 0 ? 'fw-bold text-dark' : 'text-muted'}`}
                            style={{ lineHeight: 1.35 }}
                          >
                            {n.content}
                          </div>
                          <div className="text-muted" style={{ fontSize: '0.72rem' }}>
                            {formatNotiTime(n.createdDate)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Dropdown.Menu>
            </Dropdown>

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
