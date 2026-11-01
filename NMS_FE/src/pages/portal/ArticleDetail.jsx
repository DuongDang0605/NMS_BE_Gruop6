import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Badge, Button, Spinner, Alert, Breadcrumb } from 'react-bootstrap';
import { getArticleDetail } from '../../api/portal';
import { recordView, toggleLike } from '../../api/engagement';
import { getSession } from '../../services/session';

const ArticleDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [liking, setLiking] = useState(false);

  // Đảm bảo chỉ tăng view đúng 1 lần cho mỗi bài trong phiên
  const viewRecordedRef = useRef(false);

  useEffect(() => {
    let isMounted = true;
    const controller = new AbortController();

    // 1. Tải chi tiết bài viết (hỗ trợ hủy request an toàn khi unmount / đổi trang)
    getArticleDetail(id, { signal: controller.signal })
      .then((data) => {
        if (isMounted) {
          setArticle(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted && isMounted) {
          setError(err?.response?.data?.message || 'Không thể tải chi tiết bài viết.');
          setLoading(false);
        }
      });

    // 2. Tự động tăng 1 lượt xem (chỉ 1 lần trong phiên đọc này)
    const sessionKey = `viewed_article_${id}`;
    if (!sessionStorage.getItem(sessionKey) && !viewRecordedRef.current) {
      viewRecordedRef.current = true;
      sessionStorage.setItem(sessionKey, 'true');
      recordView(id)
        .then((res) => {
          if (isMounted && res?.viewCount !== undefined) {
            setArticle((prev) => (prev ? { ...prev, viewCount: res.viewCount } : prev));
          }
        })
        .catch(() => {});
    }

    return () => {
      isMounted = false;
      controller.abort();
    };
  }, [id]);

  const handleLike = async () => {
    const session = getSession();
    if (!session?.token) {
      if (window.confirm('Bạn cần đăng nhập để thích bài viết này. Đi đến trang đăng nhập?')) {
        navigate('/login');
      }
      return;
    }

    try {
      setLiking(true);
      const res = await toggleLike(id);
      setArticle((prev) =>
        prev
          ? {
              ...prev,
              isLikedByCurrentUser: res.isLiked,
              likeCount: res.likeCount,
            }
          : prev
      );
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể thực hiện thao tác Thích.');
    } finally {
      setLiking(false);
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '';
    try {
      return new Date(isoString).toLocaleDateString('vi-VN', {
        weekday: 'long',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <div className="mt-3 text-muted fw-semibold">Đang tải nội dung bài viết...</div>
      </div>
    );
  }

  if (error || !article) {
    return (
      <div className="py-4" style={{ maxWidth: 860, margin: '0 auto' }}>
        <Alert variant="danger" className="rounded-3 p-4">
          <h5 className="fw-bold">Không tìm thấy bài viết!</h5>
          <p className="mb-3">{error || 'Bài viết có thể chưa được xuất bản hoặc đã bị gỡ.'}</p>
          <Button variant="outline-danger" size="sm" onClick={() => navigate('/portal')}>
            <i className="bi bi-arrow-left me-1"></i>Quay lại trang chủ
          </Button>
        </Alert>
      </div>
    );
  }

  return (
    <article className="py-3" style={{ maxWidth: 880, margin: '0 auto' }}>
      {/* Breadcrumb điều hướng */}
      <Breadcrumb className="mb-4">
        <Breadcrumb.Item onClick={() => navigate('/portal')}>
          <i className="bi bi-house-door me-1"></i>Trang chủ
        </Breadcrumb.Item>
        <Breadcrumb.Item active>{article.categoryName || 'Tin tức'}</Breadcrumb.Item>
      </Breadcrumb>

      <div className="bg-white border rounded-4 shadow-sm p-4 p-md-5">
        {/* Category & Date */}
        <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <span className="badge bg-primary px-3 py-2 text-uppercase fw-semibold" style={{ letterSpacing: '0.05em' }}>
            {article.categoryName || 'Tin tức chung'}
          </span>
          <span className="text-muted small">
            <i className="bi bi-clock me-1"></i>
            {formatDate(article.publishedDate)}
          </span>
        </div>

        {/* Tiêu đề bài báo lớn, phong cách Editorial báo chí */}
        <h1
          className="fw-bold mb-4 text-dark"
          style={{
            lineHeight: 1.3,
            fontSize: 'clamp(1.8rem, 3.5vw, 2.4rem)',
            letterSpacing: '-0.02em',
          }}
        >
          {article.title}
        </h1>

        {/* Tác giả & Thống kê lượt xem / like */}
        <div className="d-flex justify-content-between align-items-center pb-3 mb-4 border-bottom text-muted flex-wrap gap-3">
          <div className="d-flex align-items-center gap-2">
            <i className="bi bi-person-circle fs-4 text-primary"></i>
            <div>
              <div className="fw-bold text-dark">{article.authorName || 'Ban biên tập FPT'}</div>
              {article.source && <small className="text-muted">Nguồn: {article.source}</small>}
            </div>
          </div>
          <div className="d-flex align-items-center gap-4 small">
            <span>
              <i className="bi bi-eye me-1"></i>
              <strong>{article.viewCount?.toLocaleString('vi-VN') || 0}</strong> lượt xem
            </span>
            <span className="text-danger">
              <i className="bi bi-heart-fill me-1"></i>
              <strong>{article.likeCount?.toLocaleString('vi-VN') || 0}</strong> thích
            </span>
          </div>
        </div>

        {/* Headline tóm tắt nổi bật (Lead quote) */}
        {article.headline && (
          <div
            className="p-4 mb-4 rounded-3 border-start border-4 border-warning"
            style={{
              background: '#fffaf5',
              fontSize: '1.15rem',
              lineHeight: 1.6,
              color: '#334155',
              fontWeight: 500,
            }}
          >
            {article.headline}
          </div>
        )}

        {/* Nội dung bài viết */}
        <div
          className="article-content text-dark mb-5"
          style={{
            lineHeight: 1.85,
            fontSize: '1.12rem',
            whiteSpace: 'pre-line',
            color: '#1e293b',
          }}
        >
          {article.content}
        </div>

        {/* Tags */}
        {article.tags && article.tags.length > 0 && (
          <div className="mb-4 pt-3 border-top">
            <span className="fw-bold me-2 small text-muted text-uppercase">
              <i className="bi bi-tags me-1"></i>Từ khóa:
            </span>
            {article.tags.map((tag, idx) => (
              <Badge key={idx} bg="light" text="dark" className="me-2 border px-2 py-1 fw-normal">
                #{tag}
              </Badge>
            ))}
          </div>
        )}

        {/* Thanh tương tác dưới bài (Like & Back) */}
        <div className="p-3 p-md-4 rounded-3 bg-light d-flex justify-content-between align-items-center flex-wrap gap-3">
          <Button
            variant={article.isLikedByCurrentUser ? 'danger' : 'outline-danger'}
            className="d-flex align-items-center gap-2 px-4 py-2 rounded-pill shadow-sm"
            onClick={handleLike}
            disabled={liking}
          >
            {liking ? (
              <Spinner animation="border" size="sm" />
            ) : (
              <i className={`bi ${article.isLikedByCurrentUser ? 'bi-heart-fill' : 'bi-heart'} fs-5`}></i>
            )}
            <span className="fw-bold">
              {article.isLikedByCurrentUser ? 'Đã thích bài viết' : 'Thích bài viết'} ({article.likeCount || 0})
            </span>
          </Button>

          <Button
            variant="outline-secondary"
            size="sm"
            onClick={() => navigate('/portal')}
            className="d-flex align-items-center gap-1 rounded-pill px-3"
          >
            <i className="bi bi-arrow-left"></i> Quay lại danh sách tin
          </Button>
        </div>
      </div>
    </article>
  );
};

export default ArticleDetail;
