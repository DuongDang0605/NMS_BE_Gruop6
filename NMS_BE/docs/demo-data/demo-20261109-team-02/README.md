# Dữ liệu demo NMS — demo-20261109-team-02

Đích API: http://localhost:8080. Mọi ghi dữ liệu đều thực hiện qua HTTP API. SQL chỉ dùng SELECT để đối chiếu số dòng trước/sau.

Kết quả: 473 request; 472 đạt kỳ vọng; 1 lỗi; 20 bước bỏ qua.

## Tài khoản dùng để demo

Mật khẩu chung của các tài khoản demo mới: `Demo@2026!`. Admin dùng tài khoản quản trị có sẵn, không tạo thêm Admin.

| Vai trò | Email | Xác minh | Trạng thái |
| --- | --- | --- | --- |
| Contributor | demo.contributor1.20261109-team-02@example.com | Verified | Active |
| Contributor | demo.contributor2.20261109-team-02@example.com | Verified | Active |
| Contributor | demo.contributor3.20261109-team-02@example.com | Verified | Active |
| Editor | demo.editor1.20261109-team-02@example.com | Verified | Active |
| Editor | demo.editor2.20261109-team-02@example.com | Verified | Active |
| Moderator | demo.moderator1.20261109-team-02@example.com | Verified | Active |
| Moderator | demo.moderator2.20261109-team-02@example.com | Verified | Active |
| Reader | demo.reader1.20261109-team-02@example.com | Verified | Active |
| Reader | demo.reader2.20261109-team-02@example.com | Verified | Active |
| Reader | demo.reader3.20261109-team-02@example.com | Unverified | Active |
| Reader | demo.reader4.20261109-team-02@example.com | Unverified | Active |
| Reader | demo.inactive.20261109-team-02@example.com | Verified | Inactive |

## Đối chiếu tất cả bảng

| Bảng | Trước | Sau | Số dòng tăng | Bản ghi demo quan sát qua API |
| --- | ---: | ---: | ---: | ---: |
| SystemAccount | 5 | 17 | 12 | 12 |
| Category | 0 | 8 | 8 | 8 |
| Tag | 0 | 12 | 12 | 12 |
| SystemSetting | 0 | 3 | 3 | 3 |
| BannedKeyword | 0 | 4 | 4 | 4 |
| NewsArticle | 0 | 29 | 29 | 29 |
| NewsTag | 0 | 59 | 59 | 59 |
| Comment | 0 | 32 | 32 | 32 |
| CommentReport | 0 | 8 | 8 | 8 |
| ArticleReaction | 0 | 40 | 40 | 40 |
| Notification | 0 | 24 | 24 | 24 |
| ArticleReviewNote | 0 | 2 | 2 | 2 |
| ArticleAuditLog | 0 | 81 | 81 | 81 |

## Kịch bản demo

- Admin: quản lý user, xác minh Reader, Deactivate/Activate tài khoản, danh mục, tag, cấu hình và thông báo.
- Contributor: mở các bài DRAFT hoặc CHANGES_REQUESTED, xem góp ý, sửa nội dung và gửi duyệt.
- Editor: mở hàng đợi PENDING_REVIEW; yêu cầu chỉnh sửa, xuất bản hoặc hẹn giờ; xem lịch sử và bài lưu trữ.
- Moderator: xem bình luận đang chờ, báo cáo PENDING/RESOLVED/DISMISSED, từ khóa FLAG/BLOCK và duyệt/từ chối bình luận.
- Reader: đọc tin công khai, thích bài, bình luận, trả lời và báo cáo. Tài khoản reader3/reader4 chưa xác minh dùng để demo tiền kiểm.
- Tài khoản inactive dùng để demo popup bị vô hiệu hóa. Tất cả nội dung được đánh dấu Demo và là thông tin mô phỏng.

Không sửa/xóa dữ liệu cũ; không gửi email OTP; không đổi cấu hình kiểm duyệt toàn hệ thống. Không thực hiện các API DELETE xóa hẳn bản ghi. Bảng NewsTag, ArticleAuditLog và ArticleReviewNote được tạo qua liên kết/quy trình nghiệp vụ.

## API lỗi cần xử lý

Lỗi backend ghi nhận: 0. Lỗi đầu vào của script: 1.

| Method | Endpoint thực tế | HTTP | Phân loại | Tình huống |
| --- | --- | ---: | --- | --- |
| POST | /api/v1/notifications | 400 | Đầu vào script đã sửa | Create notification for contributor1 |

### Lỗi 1: POST /api/v1/notifications

Actor: admin. HTTP 400.

The seed script submitted isRead=1. The API correctly rejected it; create unread, then call the read endpoint.

Request:
```json
{
  "recipientId": 3225,
  "newsArticleId": 967,
  "title": "Nhắc lịch trình diễn dự án",
  "content": "Nhóm chuẩn bị trình diễn các tính năng theo vai trò. Mã dữ liệu: demo-20261109-team-02.",
  "isRead": 1
}
```

Response:
```json
{
  "timestamp": "2026-11-09T07:15:33.296450800Z",
  "status": 400,
  "error": "Bad Request",
  "message": "isRead must be 0 when creating a notification",
  "path": "/api/v1/notifications",
  "errors": {}
}
```

## Phạm vi kiểm thử

Nhật ký chi tiết: [api-log.jsonl](api-log.jsonl). ID và trạng thái: [summary.json](summary.json). Token và mật khẩu request đã được che trong nhật ký.

API không gọi:

- `PUT /api/v1/users/me/password`
- `PUT /api/v1/tags/{id}`
- `DELETE /api/v1/tags/{id}`
- `DELETE /api/v1/settings/{id}`
- `PUT /api/v1/news/{id}`
- `DELETE /api/v1/news/{id}`
- `PUT /api/v1/moderation/settings`
- `PUT /api/v1/moderation/keywords/{keywordId}`
- `DELETE /api/v1/moderation/keywords/{keywordId}`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`
- `GET /api/v1/author/articles/{id}`
- `DELETE /api/v1/author/articles/{id}`
- `POST /api/v1/auth/unlock/verify`
- `POST /api/v1/auth/unlock/reset-password`
- `POST /api/v1/auth/unlock/request`
- `POST /api/v1/auth/refresh`
- `PATCH /api/v1/users/{id}/verify`
- `PATCH /api/v1/notifications/read-all`
- `PATCH /api/v1/news/{id}/status`
- `GET /api/v1/notifications/{id}`
- `DELETE /api/v1/notifications/{id}`
- `GET /api/v1/editorial/articles/{id}`
- `GET /api/v1/editorial/articles/{id}/audit-trail`
- `DELETE /api/v1/moderation/comments/{commentId}`

API không gọi không được kết luận là lỗi. Các luồng email OTP, đổi mật khẩu cá nhân, xóa hẳn dữ liệu và sửa thiết lập toàn cục được để lại cho demo chủ động. Khi một endpoint lỗi, các lần gọi cùng endpoint sau đó bị bỏ qua; không tự sửa backend và không thay bằng INSERT SQL.

## Chạy lại với một bộ mới

Tại thư mục backend, đặt DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD, tùy chọn DEMO_PASSWORD và DEMO_RUN_ID rồi chạy:

```powershell
node ./scripts/Seed-Demo-Api.mjs
```

Một run ID chỉ được chạy một lần để tránh tạo trùng ngoài ý muốn. Không có thao tác dọn/xóa tự động.
