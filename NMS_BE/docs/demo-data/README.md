# Bộ dữ liệu demo cho cả nhóm

Bộ đang có trong database: **demo-20261109-team-02**, tạo ngày 09/11/2026 trên `http://localhost:8080`.

- [Tài khoản, mật khẩu demo, số dòng từng bảng và kịch bản trình diễn](demo-20261109-team-02/README.md)
- [Nhật ký từng request và response](demo-20261109-team-02/api-log.jsonl)
- [ID các bản ghi, trạng thái và phạm vi API đã gọi](demo-20261109-team-02/summary.json)
- [Script tạo dữ liệu qua API](../../scripts/Seed-Demo-Api.mjs)
- [12 tài khoản dễ nhớ: contributor/editor/moderator/reader 1–3, mật khẩu 123456](easy-accounts.md)

Đã thêm **314 bản ghi vào đủ 13 bảng**: 12 tài khoản, 8 danh mục, 12 tag, 3 cấu hình demo, 4 từ khóa kiểm duyệt, 29 bài viết, 59 liên kết bài/tag, 32 bình luận, 8 báo cáo, 40 lượt thích, 24 thông báo, 2 ghi chú biên tập và 81 dòng lịch sử. SQL chỉ được dùng SELECT để đối chiếu số dòng; mọi ghi dữ liệu đi qua HTTP API.

Có 16 bài Published, 3 Draft, 3 Pending Review, 2 Changes Requested, 1 Approved, 2 Scheduled và 2 Archived. Bình luận gồm 24 Approved, 6 Pending Moderation và 2 Rejected. Báo cáo gồm 6 Pending, 1 Resolved và 1 Dismissed. Thông báo gồm 12 chưa đọc và 12 đã đọc.

473 request đã thực hiện: 472 đạt kỳ vọng, 1 request sai đầu vào bị HTTP 400. Không ghi nhận lỗi backend trong các request hợp lệ đã chạy. Request 400 gửi `isRead=1` khi tạo thông báo; API yêu cầu `isRead=0`. Script đã được sửa để tạo chưa đọc rồi gọi API đánh dấu đã đọc. Request lỗi gốc và các bước từng bị bỏ qua vẫn được giữ trong nhật ký. API chưa gọi được liệt kê trong báo cáo, không được xem là đã kiểm thử hoặc bị lỗi.

Mật khẩu chung của **các tài khoản demo mới**: `Demo@2026!`. Dùng Admin hiện có để quản lý; không tạo thêm Admin. Email từng vai trò có trong báo cáo chính. Tài khoản `demo.inactive.20261109-team-02@example.com` được giữ inactive để demo popup vô hiệu hóa và chức năng Activate.

Lần `demo-20261109-team-01` là lần thử khi backend đã ngừng chạy, chưa thêm bản ghi nào. Giữ thư mục đó làm bằng chứng gián đoạn kết nối; dùng báo cáo **team-02** để demo.

## Tạo thêm một bộ mới

Tại thư mục backend, dùng Node.js có hỗ trợ `fetch` (Node 20+), cấu hình biến môi trường rồi chạy:

```powershell
$env:DEMO_BASE_URL = 'http://localhost:8080'
$env:DEMO_ADMIN_EMAIL = 'admin@fu.edu.vn'
$env:DEMO_ADMIN_PASSWORD = '<mật khẩu Admin đang dùng>'
$env:DEMO_RUN_ID = 'demo-bo-moi'
try {
    node ./scripts/Seed-Demo-Api.mjs
} finally {
    Remove-Item Env:DEMO_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
```

Tùy chọn `DEMO_PASSWORD` để đổi mật khẩu cho bộ mới. Nếu muốn đối chiếu số dòng bằng Windows Authentication, đặt `DEMO_SQL_SERVER=localhost` và `DEMO_SQL_DATABASE=FUNewsManagementSystem`; SQL chỉ đọc số dòng. Không chạy lại run ID đã có. Script không tự xóa dữ liệu, không gửi OTP email và không sửa thiết lập kiểm duyệt toàn cục.

Để tạo lại báo cáo từ nhật ký mà không ghi thêm dữ liệu ứng dụng, đặt `DEMO_MODE=report` cùng run ID cũ. Chế độ `DEMO_MODE=complete-notifications` chỉ bổ sung các thông báo còn thiếu của run đã có; dùng khi đã xác định lỗi đầu vào của script, không dùng để thử lại endpoint lỗi backend.
