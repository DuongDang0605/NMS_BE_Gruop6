import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Alert, Badge, Button, Form, Modal, Tab, Table, Tabs } from 'react-bootstrap';
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
    const [pendingPageInfo, setPendingPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
    const [reportsPageInfo, setReportsPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
    const [keywords, setKeywords] = useState([]);
    const [preModeration, setPreModeration] = useState(false);
    const [error, setError] = useState('');
    const [reloadKey, setReloadKey] = useState(0);
    const [pendingPage, setPendingPage] = useState(0);
    const [reportsPage, setReportsPage] = useState(0);

    const [newWord, setNewWord] = useState('');
    const [newSeverity, setNewSeverity] = useState('BLOCK');
    const [confirmation, setConfirmation] = useState(null);
    const [confirming, setConfirming] = useState(false);

    // Mỗi endpoint trả một trang riêng; lọc sau khi nhận trang sẽ làm mất bản ghi ở các trang sau.
    useEffect(() => {
        const controller = new AbortController();
        const options = { signal: controller.signal };
        Promise.all([
            getPendingComments({ page: pendingPage, size: 20 }, options),
            getReports({ status: 'PENDING', page: reportsPage, size: 20 }, options),
            getKeywords(options),
            getModerationSettings(options),
        ])
            .then(([pendingResult, reportResult, keywordList, settings]) => {
                setPending(pendingResult.content);
                setPendingPageInfo({
                    page: pendingResult.page,
                    totalPages: pendingResult.totalPages,
                    totalElements: pendingResult.totalElements,
                });
                setReports(reportResult.content);
                setReportsPageInfo({
                    page: reportResult.page,
                    totalPages: reportResult.totalPages,
                    totalElements: reportResult.totalElements,
                });
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
    }, [pendingPage, reloadKey, reportsPage]);

    // Chạy một thao tác rồi tải lại dữ liệu
    const run = async (action) => {
        try {
            await action();
            setPendingPage(0);
            setReportsPage(0);
            setReloadKey((k) => k + 1);
        } catch (err) {
            setError(err?.response?.data?.message || 'Thao tác thất bại.');
        }
    };

    const requestConfirmation = (action, message, confirmLabel = 'Xác nhận') => {
        setConfirmation({ action, message, confirmLabel });
    };

    const handleConfirm = async () => {
        if (!confirmation || confirming) return;
        setConfirming(true);
        await run(confirmation.action);
        setConfirming(false);
        setConfirmation(null);
    };

    const handleAddKeyword = (e) => {
        e.preventDefault();
        if (!newWord.trim()) return;
        run(async () => {
            await createKeyword({ word: newWord.trim(), severity: newSeverity, isActive: true });
            setNewWord('');
        });
    };

    const handleTogglePreModeration = () => {
        const updateSetting = async () => {
            const result = await updateModerationSettings(!preModeration);
            setPreModeration(result.preModeration);
        };

        if (preModeration) {
            requestConfirmation(
                updateSetting,
                'Bạn có chắc chắn muốn tắt chế độ kiểm duyệt trước toàn trang không?',
                'Tắt kiểm duyệt',
            );
            return;
        }
        run(updateSetting);
    };

    const reportedComments = groupReports(reports);

    return (
        <div>
            <h4 className="fw-bold mb-3">Trung tâm kiểm duyệt</h4>
            {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

            <Tabs defaultActiveKey="pending" className="mb-3">
                {/* Tab 1: bình luận chờ duyệt */}
                <Tab eventKey="pending" title={`Chờ duyệt (${pendingPageInfo.totalElements})`}>
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
                                            onClick={() => requestConfirmation(
                                                () => rejectComment(c.commentId),
                                                'Bạn có chắc chắn muốn từ chối bình luận này không?',
                                                'Từ chối',
                                            )}>
                                        Từ chối
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                    {pendingPageInfo.totalPages > 1 && (
                        <div className="d-flex align-items-center justify-content-end gap-2 mb-3">
                            <Button
                                size="sm"
                                variant="outline-secondary"
                                disabled={pendingPageInfo.page === 0}
                                onClick={() => setPendingPage((page) => page - 1)}
                            >
                                Trước
                            </Button>
                            <span className="small text-muted">
                                Trang {pendingPageInfo.page + 1}/{pendingPageInfo.totalPages}
                            </span>
                            <Button
                                size="sm"
                                variant="outline-secondary"
                                disabled={pendingPageInfo.page + 1 >= pendingPageInfo.totalPages}
                                onClick={() => setPendingPage((page) => page + 1)}
                            >
                                Sau
                            </Button>
                        </div>
                    )}
                </Tab>

                {/* Tab 2: bình luận bị báo cáo */}
                <Tab eventKey="reports" title={`Phiếu báo cáo (${reportsPageInfo.totalElements})`}>
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
                                            onClick={() => requestConfirmation(
                                                () => deleteComment(c.commentId),
                                                'Bạn có chắc chắn muốn xóa bình luận vi phạm này không?',
                                                'Xóa bình luận',
                                            )}>
                                        Xóa
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                    {reportsPageInfo.totalPages > 1 && (
                        <div className="d-flex align-items-center justify-content-end gap-2 mb-3">
                            <Button
                                size="sm"
                                variant="outline-secondary"
                                disabled={reportsPageInfo.page === 0}
                                onClick={() => setReportsPage((page) => page - 1)}
                            >
                                Trước
                            </Button>
                            <span className="small text-muted">
                                Trang {reportsPageInfo.page + 1}/{reportsPageInfo.totalPages}
                            </span>
                            <Button
                                size="sm"
                                variant="outline-secondary"
                                disabled={reportsPageInfo.page + 1 >= reportsPageInfo.totalPages}
                                onClick={() => setReportsPage((page) => page + 1)}
                            >
                                Sau
                            </Button>
                        </div>
                    )}
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
                                            onClick={() => {
                                                const update = () => updateKeyword(k.keywordId, {
                                                    word: k.word, severity: k.severity, isActive: !k.isActive,
                                                });
                                                if (k.isActive) {
                                                    requestConfirmation(
                                                        update,
                                                        `Bạn có chắc chắn muốn tắt từ khóa "${k.word}" không?`,
                                                        'Tắt từ khóa',
                                                    );
                                                    return;
                                                }
                                                run(update);
                                            }}>
                                        {k.isActive ? 'Tắt' : 'Bật'}
                                    </Button>
                                    <Button size="sm" variant="outline-danger"
                                            onClick={() => requestConfirmation(
                                                () => deleteKeyword(k.keywordId),
                                                `Bạn có chắc chắn muốn xóa từ khóa "${k.word}" không?`,
                                                'Xóa từ khóa',
                                            )}>
                                        Xóa
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </Table>
                </Tab>
            </Tabs>
            <Modal
                show={confirmation !== null}
                onHide={() => {
                    if (!confirming) setConfirmation(null);
                }}
                centered
                backdrop={confirming ? 'static' : true}
                keyboard={!confirming}
            >
                <Modal.Header closeButton={!confirming}>
                    <Modal.Title>Xác nhận thao tác</Modal.Title>
                </Modal.Header>
                <Modal.Body>{confirmation?.message}</Modal.Body>
                <Modal.Footer>
                    <Button
                        variant="secondary"
                        onClick={() => setConfirmation(null)}
                        disabled={confirming}
                    >
                        Hủy
                    </Button>
                    <Button variant="danger" onClick={handleConfirm} disabled={confirming}>
                        {confirming ? 'Đang xử lý...' : confirmation?.confirmLabel}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}