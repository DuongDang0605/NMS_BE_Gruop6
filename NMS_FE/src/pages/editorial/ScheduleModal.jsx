import { useState, useMemo } from 'react';
import { Modal, Button, Form, Alert, Spinner } from 'react-bootstrap';
import { editorialApi } from '../../api/editorial';

const formatLocalIso = (date) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const ScheduleModal = ({ show, onHide, article, onSuccess }) => {
  const [scheduledDate, setScheduledDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Lấy thời điểm tối thiểu là hiện tại + 2 phút theo giờ địa phương chuẩn YYYY-MM-DDTHH:mm
  const minDateTime = useMemo(() => {
    const d = new Date(Date.now() + 2 * 60 * 1000);
    return formatLocalIso(d);
  }, [show]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!scheduledDate) {
      setError('Vui lòng chọn ngày và giờ xuất bản');
      return;
    }

    const selectedDate = new Date(scheduledDate);
    const selectedTime = selectedDate.getTime();
    const minTime = Date.now() + 60 * 1000;
    if (selectedTime < minTime) {
      setError('Thời gian hẹn giờ phải ở tương lai ít nhất 1 phút');
      return;
    }

    setLoading(true);
    setError('');
    try {
      // Chuyển đổi thời gian địa phương đã chọn sang UTC (YYYY-MM-DDTHH:mm:ss) để backend xử lý đồng bộ
      const utcFormattedDate = selectedDate.toISOString().slice(0, 19);
      await editorialApi.schedulePublication(article.newsArticleId, utcFormattedDate);
      if (onSuccess) onSuccess();
      onHide();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể lên lịch xuất bản. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>
          <i className="bi bi-calendar-event me-2 text-info"></i>
          Hẹn giờ xuất bản bài viết
        </Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          {error && <Alert variant="danger">{error}</Alert>}

          <p className="mb-2">
            Bài viết: <strong>{article?.newsTitle}</strong>
          </p>
          <p className="text-muted small mb-3">
            Hệ thống sẽ chuyển trạng thái bài viết sang <strong>SCHEDULED</strong> và tự động xuất bản công khai
            (PUBLISHED) đúng vào thời điểm bạn chỉ định.
          </p>

          <Form.Group className="mb-3">
            <Form.Label className="fw-semibold">
              Chọn thời gian xuất bản tự động <span className="text-danger">*</span>
            </Form.Label>
            <Form.Control
              type="datetime-local"
              min={minDateTime}
              value={scheduledDate}
              onChange={(e) => setScheduledDate(e.target.value)}
              required
            />
            <Form.Text className="text-muted">
              Định dạng: Ngày / Tháng / Năm và Giờ : Phút (Giờ địa phương)
            </Form.Text>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide} disabled={loading}>
            Hủy
          </Button>
          <Button variant="info" type="submit" disabled={loading || !scheduledDate} className="text-white">
            {loading ? (
              <>
                <Spinner size="sm" animation="border" className="me-1" />
                Đang xử lý...
              </>
            ) : (
              <>
                <i className="bi bi-clock me-1"></i>
                Xác nhận Lên lịch
              </>
            )}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
};

export default ScheduleModal;

