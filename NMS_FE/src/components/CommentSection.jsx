import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Button, Form, Modal } from 'react-bootstrap';
import { getArticleComments, postComment, reportComment } from '../api/comment';
import { getSession } from '../services/session';

const REASONS = [
    { value: 'SPAM', label: 'Spam' },
    { value: 'HARASSMENT', label: 'Quấy rối' },
    { value: 'OFFENSIVE', label: 'Xúc phạm' },
    { value: 'MISINFORMATION', label: 'Sai sự thật' },
    { value: 'OTHER', label: 'Khác' },
];

const errorMessage = (err, fallback) => err?.response?.data?.message || fallback;

const formatDate = (iso) => (iso ? new Date(iso).toLocaleString('vi-VN') : '');

// Form dùng chung cho bình luận mới và trả lời
function CommentForm({ onSubmit, placeholder, submitLabel, onCancel }) {
    const [text, setText] = useState('');
    const [sending, setSending] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!text.trim()) return;
        setSending(true);
        setError('');
        try {
            await onSubmit(text.trim());
            setText('');
        } catch (err) {
            setError(errorMessage(err, 'Không thể gửi bình luận.'));
        } finally {
            setSending(false);
        }
    };

    return (
        <Form onSubmit={handleSubmit} className="mb-3">
            <Form.Control
                as="textarea"
                rows={3}
                maxLength={2000}
                placeholder={placeholder}
                value={text}
                onChange={(e) => setText(e.target.value)}
            />
            {error && <div className="text-danger small mt-1">{error}</div>}
            <div className="mt-2 d-flex gap-2">
                <Button type="submit" size="sm" disabled={sending || !text.trim()}>
                    {sending ? 'Đang gửi...' : submitLabel}
                </Button>
                {onCancel && (
                    <Button variant="outline-secondary" size="sm" onClick={onCancel}>Hủy</Button>
                )}
            </div>
        </Form>
    );
}

// Một bình luận, gọi đệ quy để hiển thị các câu trả lời bên dưới (thụt đầu dòng)
function CommentItem({ comment, session, replyingTo, setReplyingTo, onReply, onReport }) {
    const deleted = comment.status === 'DELETED';
    const isOwn = session?.user.accountId === comment.accountId;

    return (
        <div className="mb-3">
            <div className="fw-semibold">
                {comment.authorName}
                <span className="text-muted small fw-normal ms-2">{formatDate(comment.createdDate)}</span>
            </div>
            <div className={deleted ? 'text-muted fst-italic' : ''} style={{ whiteSpace: 'pre-line' }}>
                {comment.content}
            </div>

            {session && !deleted && (
                <div className="small mt-1">
                    <Button variant="link" size="sm" className="p-0 me-3" onClick={() => setReplyingTo(comment.commentId)}>
                        Trả lời
                    </Button>
                    {!isOwn && (
                        <Button variant="link" size="sm" className="p-0 text-danger" onClick={() => onReport(comment.commentId)}>
                            Báo cáo
                        </Button>
                    )}
                </div>
            )}

            {replyingTo === comment.commentId && (
                <div className="mt-2">
                    <CommentForm
                        placeholder={`Trả lời ${comment.authorName}...`}
                        submitLabel="Gửi trả lời"
                        onSubmit={(content) => onReply(comment.commentId, content)}
                        onCancel={() => setReplyingTo(null)}
                    />
                </div>
            )}

            {comment.replies.length > 0 && (
                <div className="ms-4 ps-3 border-start mt-3">
                    {comment.replies.map((reply) => (
                        <CommentItem
                            key={reply.commentId}
                            comment={reply}
                            session={session}
                            replyingTo={replyingTo}
                            setReplyingTo={setReplyingTo}
                            onReply={onReply}
                            onReport={onReport}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

export default function CommentSection({ articleId }) {
    const session = getSession();

    const [comments, setComments] = useState([]);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [reloadKey, setReloadKey] = useState(0);
    const [replyingTo, setReplyingTo] = useState(null);

    const [reportTarget, setReportTarget] = useState(null);
    const [reason, setReason] = useState('SPAM');
    const [note, setNote] = useState('');
    const [reportError, setReportError] = useState('');
    const [reporting, setReporting] = useState(false);

    // Tải cây bình luận (tải lại khi reloadKey đổi)
    useEffect(() => {
        const controller = new AbortController();
        getArticleComments(articleId, { signal: controller.signal })
            .then((data) => {
                setComments(data);
                setError('');
            })
            .catch((err) => {
                if (!controller.signal.aborted) setError(errorMessage(err, 'Không thể tải bình luận.'));
            });
        return () => controller.abort();
    }, [articleId, reloadKey]);

    // Đăng bình luận gốc (parentCommentId = null) hoặc trả lời
    const handlePost = async (parentCommentId, content) => {
        const result = await postComment({ newsArticleId: articleId, parentCommentId, content });
        if (result.status === 'PENDING_MODERATION') {
            setNotice('Bình luận của bạn đang chờ kiểm duyệt viên phê duyệt.');
        } else {
            setNotice('');
            setReloadKey((k) => k + 1);
        }
        setReplyingTo(null);
    };

    const openReport = (commentId) => {
        setReportTarget(commentId);
        setReason('SPAM');
        setNote('');
        setReportError('');
    };

    const handleReport = async () => {
        setReporting(true);
        setReportError('');
        try {
            await reportComment(reportTarget, { reason, note });
            setReportTarget(null);
            setNotice('Cảm ơn bạn, báo cáo đã được gửi tới kiểm duyệt viên.');
        } catch (err) {
            setReportError(errorMessage(err, 'Không thể gửi báo cáo.'));
        } finally {
            setReporting(false);
        }
    };

    return (
        <section className="bg-white border rounded-4 shadow-sm p-4 mt-4">
            <h5 className="fw-bold mb-3">
                <i className="bi bi-chat-dots me-2"></i>Bình luận
            </h5>

            {session ? (
                <CommentForm
                    placeholder="Viết bình luận của bạn..."
                    submitLabel="Gửi bình luận"
                    onSubmit={(content) => handlePost(null, content)}
                />
            ) : (
                <Alert variant="light" className="border">
                    <Link to="/login">Đăng nhập</Link> để bình luận.
                </Alert>
            )}

            {notice && <Alert variant="info" dismissible onClose={() => setNotice('')}>{notice}</Alert>}
            {error && <Alert variant="danger">{error}</Alert>}

            {!error && comments.length === 0 && <div className="text-muted">Chưa có bình luận nào.</div>}

            {comments.map((comment) => (
                <CommentItem
                    key={comment.commentId}
                    comment={comment}
                    session={session}
                    replyingTo={replyingTo}
                    setReplyingTo={setReplyingTo}
                    onReply={handlePost}
                    onReport={openReport}
                />
            ))}

            <Modal show={reportTarget !== null} onHide={() => setReportTarget(null)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>Báo cáo bình luận</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form.Group className="mb-3">
                        <Form.Label>Lý do</Form.Label>
                        <Form.Select value={reason} onChange={(e) => setReason(e.target.value)}>
                            {REASONS.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                        </Form.Select>
                    </Form.Group>
                    <Form.Group>
                        <Form.Label>Ghi chú (không bắt buộc)</Form.Label>
                        <Form.Control
                            as="textarea"
                            rows={2}
                            maxLength={400}
                            value={note}
                            onChange={(e) => setNote(e.target.value)}
                        />
                    </Form.Group>
                    {reportError && <div className="text-danger small mt-2">{reportError}</div>}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="outline-secondary" onClick={() => setReportTarget(null)}>Hủy</Button>
                    <Button variant="danger" onClick={handleReport} disabled={reporting}>
                        {reporting ? 'Đang gửi...' : 'Gửi báo cáo'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </section>
    );
}