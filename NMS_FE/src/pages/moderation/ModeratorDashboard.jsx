import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Alert, Badge, Button, Form, Tab, Table, Tabs } from 'react-bootstrap';
import {
    approveComment,
    createKeyword,
    deleteComment,
    deleteKeyword,
    getKeywords,
    getModerationSettings,
    getPendingComments,
    getReports,
    keepComment,
    rejectComment,
    updateKeyword,
    updateModerationSettings,
} from '../../api/moderation';

const formatDate = (iso) => (iso ? new Date(iso).toLocaleString('vi-VN') : '');

// Gộp các phiếu báo cáo của cùng một bình luận thành một dòng
function groupReports(reports) {
    const map = new Map();
    reports.forEach((r) => {
        if (!map.has(r.commentId)) {
            map.set(r.commentId, {
                commentId: r.commentId,
                content: r.commentContent,
                reportCount: r.commentReportCount,
                reasons: [],
            });
        }
        map.get(r.commentId).reasons.push(r.reason);
    });
    return [...map.values()];
}

export default function ModeratorDashboard() {
    const { user } = useOutletContext();
    const isAdmin = Number(user.accountRole) === 1;

    const [pending, setPending] = useState([]);
    const [reports, setReports] = useState([]);
    const [keywords, setKeywords] = useState([]);
    const [preModeration, setPreModeration] = useState(false);
    const [error, setError] = useState('');
    const [reloadKey, setReloadKey] = useState(0);

    const [newWord, setNewWord] = useState('');
    const [newSeverity, setNewSeverity] = useState('BLOCK');

    // Tải toàn bộ dữ liệu 3 tab (tải lại khi reloadKey đổi)
    useEffect(() => {
        const controller = new AbortController();
        const options = { signal: controller.signal };
        Promise.all([
            getPendingComments({ size: 50 }, options),
            getReports({ status: 'PENDING', size: 50 }, options),
            getKeywords(options),
            getModerationSettings(options),
        ])
            .then(([pendingPage, reportPage, keywordList, settings]) => {
                // Hàng đợi gồm cả bình luận bị báo cáo, tab 1 chỉ lấy bình luận chờ duyệt
                setPending(pendingPage.content.filter((c) => c.status === 'PENDING_MODERATION'));
                setReports(reportPage.content);
                setKeywords(keywordList);
                setPreModeration(settings.preModeration);
                setError('');
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setError(err?.response?.data?.message || 'Không thể tải dữ liệu kiểm duyệt.');
                }
            });
        return () => controller.abort();
    }, [reloadKey]);

    // Chạy một thao tác rồi tải lại dữ liệu
    const run = async (action, confirmText) => {
        if (confirmText && !window.confirm(confirmText)) return;
        try {
            await action();
            setReloadKey((k) => k + 1);
        } catch (err) {
            setError(err?.response?.data?.message || 'Thao tác thất bại.');
        }
    };

    const handleAddKeyword = (e) => {
        e.preventDefault();
        if (!newWord.trim()) return;
        run(async () => {
            await createKeyword({ word: newWord.trim(), severity: newSeverity, isActive: true });
            setNewWord('');
        });
    };

    const handleTogglePreModeration = async () => {
        try {
            const result = await updateModerationSettings(!preModeration);
            setPreModeration(result.preModeration);
        } catch (err) {
            setError(err?.response?.data?.message || 'Không thể đổi chế độ kiểm duyệt.');
        }
    };

    const reportedComments = groupReports(reports);

    return (
        <div>
            <h4 className="fw-bold mb-3">Trung tâm kiểm duyệt</h4>
            {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

            <Tabs defaultActiveKey="pending" className="mb-3">
                {/* Tab 1: bình luận chờ duyệt */}
                <Tab eventKey="pending" title={`Chờ duyệt (${pending.length})`}>
                    <Table bordered hover responsive>
                        <thead>
                        <tr>
                            <th>Người viết</th>
                            <th>Nội dung</th>
                            <th>Thời gian</th>
                            <th style={{ width: 170 }}>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        {pending.length === 0 && (
                            <tr><td colSpan={4} className="text-center text-muted">Không có bình luận chờ duyệt.</td></tr>
                        )}
                        {pending.map((c) => (
                            <tr key={c.commentId}>
                                <td>{c.authorName}</td>
                                <td>
                                    {c.content}
                                    {c.isFlaggedProfanity && <Badge bg="warning" text="dark" className="ms-2">Từ nhạy cảm</Badge>}
                                </td>
                                <td>{formatDate(c.createdDate)}</td>
                                <td>
                                    <Button size="sm" variant="success" className="me-2"
                                            onClick={() => run(() => approveComment(c.commentId))}>
                                        Duyệt
                                    </Button>
                                    <Button size="sm" variant="outline-danger"
                                            onClick={() => run(() => rejectComment(c.commentId))}>
                                        Từ chối
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                </Tab>

                {/* Tab 2: bình luận bị báo cáo */}
                <Tab eventKey="reports" title={`Bị báo cáo (${reportedComments.length})`}>
                    <Table bordered hover responsive>
                        <thead>
                        <tr>
                            <th>Nội dung</th>
                            <th>Lý do báo cáo</th>
                            <th>Số lần</th>
                            <th style={{ width: 170 }}>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        {reportedComments.length === 0 && (
                            <tr><td colSpan={4} className="text-center text-muted">Không có báo cáo nào cần xử lý.</td></tr>
                        )}
                        {reportedComments.map((c) => (
                            <tr key={c.commentId}>
                                <td>{c.content}</td>
                                <td>{c.reasons.join(', ')}</td>
                                <td><Badge bg="danger">{c.reportCount}</Badge></td>
                                <td>
                                    <Button size="sm" variant="outline-secondary" className="me-2"
                                            onClick={() => run(() => keepComment(c.commentId))}>
                                        Giữ lại
                                    </Button>
                                    <Button size="sm" variant="danger"
                                            onClick={() => run(() => deleteComment(c.commentId), 'Xóa bình luận vi phạm này?')}>
                                        Xóa
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                </Tab>

                {/* Tab 3: từ điển từ cấm */}
                <Tab eventKey="keywords" title={`Từ cấm (${keywords.length})`}>
                    <Form.Check
                        type="switch"
                        id="pre-moderation-switch"
                        className="mb-3"
                        label="Kiểm duyệt trước toàn trang (mọi bình luận mới đều phải chờ duyệt)"
                        checked={preModeration}
                        disabled={!isAdmin}
                        onChange={handleTogglePreModeration}
                    />

                    <Form onSubmit={handleAddKeyword} className="d-flex gap-2 mb-3">
                        <Form.Control
                            placeholder="Từ khóa cấm mới"
                            maxLength={100}
                            value={newWord}
                            onChange={(e) => setNewWord(e.target.value)}
                        />
                        <Form.Select style={{ width: 160 }} value={newSeverity} onChange={(e) => setNewSeverity(e.target.value)}>
                            <option value="BLOCK">BLOCK (chặn)</option>
                            <option value="FLAG">FLAG (chờ duyệt)</option>
                        </Form.Select>
                        <Button type="submit" disabled={!newWord.trim()}>Thêm</Button>
                    </Form>

                    <Table bordered hover responsive>
                        <thead>
                        <tr>
                            <th>Từ khóa</th>
                            <th>Mức độ</th>
                            <th>Trạng thái</th>
                            <th style={{ width: 170 }}>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        {keywords.length === 0 && (
                            <tr><td colSpan={4} className="text-center text-muted">Chưa có từ khóa nào.</td></tr>
                        )}
                        {keywords.map((k) => (
                            <tr key={k.keywordId}>
                                <td>{k.word}</td>
                                <td><Badge bg={k.severity === 'BLOCK' ? 'danger' : 'warning'}>{k.severity}</Badge></td>
                                <td>{k.isActive ? 'Đang bật' : 'Đang tắt'}</td>
                                <td>
                                    <Button size="sm" variant="outline-secondary" className="me-2"
                                            onClick={() => run(() => updateKeyword(k.keywordId, {
                                                word: k.word, severity: k.severity, isActive: !k.isActive,
                                            }))}>
                                        {k.isActive ? 'Tắt' : 'Bật'}
                                    </Button>
                                    <Button size="sm" variant="outline-danger"
                                            onClick={() => run(() => deleteKeyword(k.keywordId), `Xóa từ khóa "${k.word}"?`)}>
                                        Xóa
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                </Tab>
            </Tabs>
        </div>
    );
}