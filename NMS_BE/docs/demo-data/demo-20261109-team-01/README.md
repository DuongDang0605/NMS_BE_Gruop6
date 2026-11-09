# Dữ liệu demo NMS — demo-20261109-team-01

Đích API: http://localhost:8080. Mọi ghi dữ liệu đều thực hiện qua HTTP API. SQL chỉ dùng SELECT để đối chiếu số dòng trước/sau.

Kết quả: 2 request; 0 đạt kỳ vọng; 2 lỗi; 0 bước bỏ qua.

Lỗi chặn: Admin authentication is unavailable; private API inserts require an Admin.

## Tài khoản dùng để demo

Mật khẩu chung của các tài khoản demo mới: `Demo@2026!`. Admin dùng tài khoản quản trị có sẵn, không tạo thêm Admin.

| Vai trò | Email | Xác minh | Trạng thái |
| --- | --- | --- | --- |

## Đối chiếu tất cả bảng

| Bảng | Trước | Sau | Số dòng tăng | Bản ghi demo quan sát qua API |
| --- | ---: | ---: | ---: | ---: |
| SystemAccount | — | 5 | — | 0 |
| Category | — | 0 | — | 0 |
| Tag | — | 0 | — | 0 |
| SystemSetting | — | 0 | — | 0 |
| BannedKeyword | — | 0 | — | 0 |
| NewsArticle | — | 0 | — | 0 |
| NewsTag | — | 0 | — | 0 |
| Comment | — | 0 | — | 0 |
| CommentReport | — | 0 | — | 0 |
| ArticleReaction | — | 0 | — | 0 |
| Notification | — | 0 | — | 0 |
| ArticleReviewNote | — | 0 | — | 0 |
| ArticleAuditLog | — | 0 | — | 0 |

## Kịch bản demo

- Admin: quản lý user, xác minh Reader, Deactivate/Activate tài khoản, danh mục, tag, cấu hình và thông báo.
- Contributor: mở các bài DRAFT hoặc CHANGES_REQUESTED, xem góp ý, sửa nội dung và gửi duyệt.
- Editor: mở hàng đợi PENDING_REVIEW; yêu cầu chỉnh sửa, xuất bản hoặc hẹn giờ; xem lịch sử và bài lưu trữ.
- Moderator: xem bình luận đang chờ, báo cáo PENDING/RESOLVED/DISMISSED, từ khóa FLAG/BLOCK và duyệt/từ chối bình luận.
- Reader: đọc tin công khai, thích bài, bình luận, trả lời và báo cáo. Tài khoản reader3/reader4 chưa xác minh dùng để demo tiền kiểm.
- Tài khoản inactive dùng để demo popup bị vô hiệu hóa. Tất cả nội dung được đánh dấu Demo và là thông tin mô phỏng.

Không sửa/xóa dữ liệu cũ; không gửi email OTP; không đổi cấu hình kiểm duyệt toàn hệ thống. Không thực hiện các API DELETE xóa hẳn bản ghi. Bảng NewsTag, ArticleAuditLog và ArticleReviewNote được tạo qua liên kết/quy trình nghiệp vụ.

## API lỗi cần xử lý

| Method | Endpoint thực tế | HTTP | Tình huống |
| --- | --- | ---: | --- |
| GET | /v3/api-docs | 0 | Read live API specification |
| POST | /api/v1/auth/login | 0 | Login admin |

### Lỗi 1: GET /v3/api-docs

Actor: anonymous. HTTP 0.

Request:
```json
null
```

Response:
```json
"fetch failed"
```

### Lỗi 2: POST /api/v1/auth/login

Actor: anonymous. HTTP 0.

Request:
```json
{
  "accountEmail": "admin@fu.edu.vn",
  "accountPassword": "[REDACTED]"
}
```

Response:
```json
"fetch failed"
```

## Phạm vi kiểm thử

Nhật ký chi tiết: [api-log.jsonl](api-log.jsonl). ID và trạng thái: [summary.json](summary.json). Token và mật khẩu request đã được che trong nhật ký.

API không gọi:


API không gọi không được kết luận là lỗi. Các luồng email OTP, đổi mật khẩu cá nhân, xóa hẳn dữ liệu và sửa thiết lập toàn cục được để lại cho demo chủ động. Khi một endpoint lỗi, các lần gọi cùng endpoint sau đó bị bỏ qua; không tự sửa backend và không thay bằng INSERT SQL.

## Chạy lại với một bộ mới

Tại thư mục backend, đặt DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD, tùy chọn DEMO_PASSWORD và DEMO_RUN_ID rồi chạy:

```powershell
node .scriptsSeed-Demo-Api.mjs
```

Một run ID chỉ được chạy một lần để tránh tạo trùng ngoài ý muốn. Không có thao tác dọn/xóa tự động.
