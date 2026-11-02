import React, { useEffect, useState } from 'react';
import { Modal, Button, Spinner, Alert, Badge } from 'react-bootstrap';
import { editorialApi } from '../../api/editorial';

const ACTION_COLORS = {
  CREATE_DRAFT: 'secondary',
  UPDATE_DRAFT: 'info',
  SUBMIT_REVIEW: 'warning',
  REQUEST_CHANGES: 'danger',
  APPROVE: 'primary',
  SCHEDULE: 'info',
  PUBLISH: 'success',
  AUTO_PUBLISH: 'success',
  ARCHIVE: 'dark',
  RESTORE: 'success',
};

const ACTION_LABELS = {
  CREATE_DRAFT: 'Tạo bản nháp',
  UPDATE_DRAFT: 'Cập nhật nháp',
  SUBMIT_REVIEW: 'Nộp chờ duyệt',
  REQUEST_CHANGES: 'Yêu cầu sửa đổi',
  APPROVE: 'Duyệt xuất bản',
  SCHEDULE: 'Hẹn giờ xuất bản',
  PUBLISH: 'Xuất bản ngay',
  AUTO_PUBLISH: 'Hệ thống tự xuất bản',
  ARCHIVE: 'Gỡ lưu trữ',
  RESTORE: 'Khôi phục xuất bản',
};

const AuditTrailTimeline = ({ show, onHide, articleId, articleTitle }) => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (show && articleId) {
      loadAuditLogs();
    }
  }, [show, articleId]);

  const loadAuditLogs = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await editorialApi.getAuditTrail(articleId);
      setLogs(data || []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải lịch sử kiểm toán.');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleString('vi-VN');
  };

  return (
    <Modal show={show} onHide={onHide} size="lg" centered>
      <Modal.Header closeButton>
        <Modal.Title>
          <i className="bi bi-clock-history me-2 text-primary"></i>
          Nhật ký Kiểm toán: {articleTitle || `#${articleId}`}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: '70vh', overflowY: 'auto' }}>
        {loading && (
          <div className="text-center py-4">
            <Spinner animation="border" variant="primary" />
            <p className="mt-2 text-muted">Đang tải nhật ký kiểm toán...</p>
          </div>
        )}

        {error && <Alert variant="danger">{error}</Alert>}

        {!loading && !error && logs.length === 0 && (
          <div className="text-center py-4 text-muted">
            <i className="bi bi-info-circle fs-3 d-block mb-2"></i>
            Chưa có ghi nhận nhật ký nào cho bài viết này.
          </div>
        )}

        {!loading && logs.length > 0 && (
          <div className="timeline ps-2">
            {logs.map((log) => {
              const badgeColor = ACTION_COLORS[log.action] || 'secondary';
              const actionLabel = ACTION_LABELS[log.action] || log.action;

              return (
                <div key={log.logId} className="card mb-3 shadow-sm border-start border-4 border-${badgeColor}">
                  <div className="card-body py-2 px-3">
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <div>
                        <Badge bg={badgeColor} className="me-2">
                          {actionLabel}
                        </Badge>
                        <strong className="text-dark">{log.actorName}</strong>
                      </div>
                      <small className="text-muted">
                        <i className="bi bi-calendar3 me-1"></i>
                        {formatDate(log.createdAt)}
                      </small>
                    </div>

                    <div className="d-flex align-items-center small text-secondary mb-1">
                      <span>Trạng thái: </span>
                      <span className="badge bg-light text-dark border mx-1">{log.fromStatus || 'MỚI'}</span>
                      <i className="bi bi-arrow-right mx-1 text-primary"></i>
                      <span className="badge bg-light text-dark border">{log.toStatus}</span>
                    </div>

                    {log.notes && (
                      <div className="p-2 bg-light rounded small mt-1 text-dark border">
                        <i className="bi bi-chat-left-text me-1 text-muted"></i>
                        {log.notes}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Đóng
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default AuditTrailTimeline;

