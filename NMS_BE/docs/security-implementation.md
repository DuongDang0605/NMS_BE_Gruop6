# Security implementation — Report 2

Áp dụng ma trận RBAC tại `plan_report2/00_KE_HOACH_TONG_THE_VA_KIEN_TRUC.md` mục 3.3 và hợp đồng User/Auth tại `plan_report2/01_USER_AUTH_SECURITY.md`.

## Vai trò và phân quyền

| ID | Authority | Quyền trên các API hiện có |
| --- | --- | --- |
| 1 | `ROLE_ADMIN` | Quản lý tài khoản, gán role, Verify/Unverify, taxonomy, hệ thống; toàn bộ bài viết và dashboard |
| 2 | `ROLE_CONTRIBUTOR` | Tạo bản nháp; chỉ xem/sửa/xóa bài của mình, chỉ sửa/xóa khi còn nháp; dashboard cá nhân; không tự xuất bản |
| 3 | `ROLE_EDITOR` | Xem bài trong tòa soạn, tạo/sửa bản nháp của mình; xuất bản/gỡ bài qua API trạng thái; dashboard tòa soạn |
| 4 | `ROLE_MODERATOR` | Dashboard nội bộ và thông báo của mình; không quản lý bài viết, tài khoản hoặc cấu hình |
| 5 | `ROLE_READER` | Portal đọc tin đã xuất bản, Like/Unlike, thông báo của mình, profile và đổi mật khẩu cá nhân; không vào dashboard hoặc API quản trị |
| Khách | Không có token | Đăng ký Reader, đăng nhập, refresh, OTP mở khóa, đọc tin công khai, ghi lượt xem và đọc taxonomy |

Vai trò 2 cũ được đổi tên từ Staff sang Contributor, giữ nguyên ID tài khoản và mật khẩu. Hằng `STAFF_ROLE` chỉ giữ làm alias tương thích cho tích hợp cũ; JWT không cấp `ROLE_STAFF`.

`SecurityConfig` chỉ khai báo ranh giới công khai/xác thực. Swagger/OpenAPI, POST đăng nhập/đăng ký/refresh/OTP, POST `/api/v1/news/{id}/views`, GET portal/news-public/taxonomy được mở; các request còn lại yêu cầu token. Từng controller/service dùng `@PreAuthorize` để kiểm tra role. `GET /api/v1/auth/me` vẫn cần đăng nhập. `DispatcherType.ERROR` được phép để giữ nguyên mã lỗi khi servlet xử lý lỗi nội bộ.

`NotificationController` kiểm tra người nhận: người dùng chỉ đọc/đánh dấu/xóa thông báo của mình, Admin được quản lý mọi người nhận; chỉ Admin tạo thông báo. Dashboard thông báo áp dụng cùng kiểm tra người nhận và chỉ dành cho các vai trò nội bộ.

Lỗi filter trả JSON `ApiErrorResponse`: 401 kèm `WWW-Authenticate: Bearer` khi chưa xác thực; 403 khi thiếu quyền. Controller advice dùng chung cùng cấu trúc JSON cho tất cả module.

## User, JWT và profile

- Admin CRUD `/api/v1/users`; trường `accountRole` chấp nhận 1..5. Admin tạo tài khoản thì `isVerified=true`.
- Tìm kiếm Users hỗ trợ `role` 1..5 và `isVerified`, cùng keyword/ID/name/email/phân trang/sắp xếp hiện có.
- Admin dùng `PATCH /api/v1/users/{id}/verify` để Verify/Unverify.
- `POST /api/v1/auth/register-reader` chỉ nhận tên/email/mật khẩu. Server luôn gán Reader và `isVerified=false`, không nhận quyền do client tự chọn.
- Mọi người dùng đã đăng nhập có thể gọi `GET /api/v1/users/me`, `GET /api/v1/auth/me`, `PUT /api/v1/users/me/password` (body `currentPassword`, `newPassword`). Đổi mật khẩu yêu cầu mật khẩu cũ đúng và mật khẩu mới khác mật khẩu cũ. Mật khẩu hiện tại sai trả 400 để frontend hiển thị lỗi form và giữ phiên; đổi thành công vô hiệu hóa cả Access/Refresh Token cũ và challenge OTP/reset đang tồn tại.
- JWT mới dùng email làm subject (`sub`) đúng hợp đồng Report 2; vẫn nhận token cũ có subject là Account ID. Bổ sung `role`, `isVerified`, giữ `accountId`, `accountRole`, `roles`, `email`, `pv` và claim loại token.
- Access Token 30 phút; Refresh Token 7 ngày. Refresh giữ nguyên thời điểm hết hạn ban đầu. Mỗi request tải lại tài khoản từ DB: đổi quyền/Verify có hiệu lực ngay; khóa/xóa tài khoản hoặc đổi mật khẩu làm token không dùng được.
- `SecurityUtils.getCurrentUser()` trả `CustomUserDetails`; có `getCurrentAccountId()`, `hasRole()`, `isAdmin()`. Entity nằm tại `getCurrentUser().getAccount()`.
- Đăng nhập đúng bằng mật khẩu plaintext cũ tự chuyển sang BCrypt trước khi cấp JWT, không cần bật migration hàng loạt lúc khởi động. Sai mật khẩu/đang khóa không chuyển hash và không bỏ qua bộ đếm. BCrypt đã lưu không bị băm lại.
- Đăng ký Reader tuân thủ giới hạn 6–50 ký tự trong kế hoạch và tối đa 72 byte UTF-8; đổi mật khẩu/Admin CRUD giữ giới hạn BCrypt 6–72 ký tự, tối đa 72 byte.
- Response login/profile có `roleName`, `isVerified`; không trả mật khẩu hoặc thông tin OTP.

Tài khoản bị khóa ở lần sai mật khẩu thứ 4. Luồng email OTP tại `/api/v1/auth/unlock/*` phục vụ cả quên mật khẩu khi chưa bị khóa và mở khóa tài khoản. Cả hai đều phải xác thực OTP rồi đặt mật khẩu mới; chỉ xác thực OTP chưa thay đổi mật khẩu hoặc trạng thái khóa. Reset thành công vô hiệu Access/Refresh Token cũ, xóa challenge và đặt lại bộ đếm đăng nhập sai. Xem README để cấu hình SMTP.

## Bài viết và giới hạn triển khai

Các endpoint quản trị `/api/v1/news` áp dụng role Admin/Contributor/Editor. Contributor chỉ nhận bài do chính mình tạo ở cả danh sách, tìm kiếm và lọc. Khi tạo/sửa bài, tác giả/người sửa lấy từ phiên đăng nhập; các ID giả mạo trong body không được sử dụng. Contributor không được thay trạng thái xuất bản bằng POST, PUT hoặc PATCH. Editor/Admin dùng PATCH `/{id}/status`.

Portal mới đọc tin qua GET `/api/v1/portal/articles` (phân trang/lọc) và `/{id}`. Các URL cũ GET `/api/v1/portal/news`, `/{id}` và alias `/api/v1/news/public`, `/{id}` tiếp tục hoạt động. Các endpoint này luôn lọc bài đã xuất bản, không lộ bản nháp.

POST `/api/v1/news/{id}/views` cho khách và tài khoản đăng nhập; POST `/api/v1/news/{id}/reactions` yêu cầu đăng nhập, hỗ trợ cả 5 role. Cả hai chỉ thao tác bài đã xuất bản, khóa hàng bài viết trong transaction để tránh mất bộ đếm khi nhiều request đồng thời. Phản hồi Like dùng `isLikedByCurrentUser`, `articleId`, `likeCount`. Frontend chỉ ghi lượt xem sau khi tải chi tiết thành công và lưu dấu trong session sau khi API ghi nhận thành công.

Category đã có CRUD và cây danh mục: các GET công khai, tạo/sửa/xóa chỉ Admin ở cả controller và service. Dashboard `/overview` giữ scope theo role; `/engagement` dành cho Admin/Editor và Contributor (chỉ số bài của chính mình). Moderator chỉ nhận số liệu thông báo cá nhân, frontend không gọi engagement. `/news` dành cho Admin/Editor. Thông báo giữ kiểm tra người nhận ở cả controller và service sau khi tích hợp module Dev 3.

Mã nguồn hiện tại vẫn dùng `NewsStatus` dạng số: `0` = nháp/chưa công khai, `1` = đã công khai. Chưa triển khai state machine đầy đủ `PENDING_REVIEW`, `CHANGES_REQUESTED`, `SCHEDULED`, `ARCHIVED`, review queue/audit trail và comment/moderation. Các module đó thuộc phần nghiệp vụ riêng trong kế hoạch và phải có method security khi được bổ sung. Dashboard Moderator hiện chỉ có số liệu thông báo cá nhân, chưa có số liệu bình luận. Settings controller hiện chưa có CRUD handler. NewsTag giữ URL hiện có `/api/news-tags`.

## Database và chạy ứng dụng

Spring Boot tự chạy lại migration `001`–`006` theo thứ tự trước Hibernate ở mỗi lần khởi động IDE/Maven/JAR. Maven đóng gói các script từ `database/migrations` vào `db/migrations` trên classpath. `spring.sql.init.mode=always` bật việc chạy lặp lại; mỗi file được thực thi thành một batch T-SQL, lỗi migration dừng khởi động. `Start-Backend.ps1` chỉ chuẩn bị database rồi khởi động ứng dụng.

Migration `004-report2-account-roles.sql` mở rộng check constraint 1..5, thêm `IsVerified` và đánh dấu tài khoản nội bộ cũ đã xác minh. Chạy lại không đặt lại trạng thái xác minh đã chỉnh.

Migration `005-article-engagement-counters.sql` thêm ViewCount/LikeCount với mặc định 0 cho database đã có bài viết; chạy lại không đặt lại các bộ đếm đã tồn tại. Spring Boot tự chạy sau migration 004. Entity cũng khai báo DB default để Hibernate thêm cột được trên bảng có dữ liệu.

Cấu hình JWT/CORS vẫn dùng `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `JWT_ISSUER`, `CORS_ALLOWED_ORIGINS`. Không seed hoặc ghi đè mật khẩu tài khoản Admin. JWT secret chưa cấu hình thì dùng khóa local ngẫu nhiên, token không tồn tại qua lần khởi động lại.

## Frontend

- Login định tuyến Reader tới `/portal`, vai trò nội bộ tới `/dashboard`; có `/register` cho Reader.
- Portal dùng dữ liệu bài đã xuất bản từ API. React hiển thị nội dung dưới dạng text.
- Layout chặn Reader vào trang quản trị; route guards và sidebar áp dụng quyền theo role. Settings/Users chỉ dành cho Admin.
- `/account` là trang hồ sơ cho cả 5 vai trò, hiển thị role/Verified và form đổi mật khẩu có xác nhận. Header quản trị và Portal đều có liên kết My account. Sau khi đổi thành công, xóa phiên trên trình duyệt và chuyển về Login với thông báo.
- `src/api/auth.js` tập trung login/register/refresh/me và khôi phục OTP; `src/api/user.js` tập trung CRUD/Verify/profile/đổi mật khẩu.
- User form/filter hỗ trợ 5 vai trò, badge Verified/Unverified, nút Verify/Unverify và lọc xác minh.
- Dashboard dùng các chỉ số khác nhau theo role; Contributor không nhận tổng số người dùng hoặc thống kê toàn hệ thống.
- Trang Category đã dùng API thật; chỉ Admin thấy nút tạo/sửa/xóa. Trang News hiện vẫn dùng mock dữ liệu local.
- `api.js` không tự refresh khi login/register/OTP thất bại. Các API bảo vệ gặp 401 thì refresh một lần; 403 giữ phiên và báo thiếu quyền.

## Kiểm thử local

Test mới trong thư mục backend/frontend được Git ignore theo yêu cầu; file test đã được thành viên commit trước đó vẫn được Git theo dõi. `Report2AuthorizationTests` kiểm tra 5 role, JWT/refresh, profile, role assignment, Verify, sở hữu bản nháp, chống giả tác giả, ngăn tự xuất bản, portal không lộ bản nháp, dashboard và đổi mật khẩu. `MergeIntegrationRegressionTests` kiểm tra public taxonomy/portal/views, quyền CRUD category, Like/Unlike cho 5 role, quyền thông báo và scope engagement. Giữ các test khóa/OTP và kiểm tra Swagger/error dispatch.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\mvnw.cmd test '-Dtest=MergeIntegrationRegressionTests,Report2AuthorizationTests,UserSecurityCompletionTests,AccountLoginLockTests,AccountUnlockTests,SwaggerDocumentationTests,ApiErrorDispatchRegressionTests,DataInitializerTests,CategoryServiceTests' '-Dspring.jpa.show-sql=false'
```

Frontend: `npm test`, `npm run lint`, `npm run build`. Test tích hợp dùng SQL Server local; test gửi mail dùng SMTP mock, không gửi thư tới người dùng thật.

## Hoàn tất phạm vi Member 1 trong kế hoạch

- 5 role, `isVerified`, constraint/migration và các helper SecurityUtils dùng chung.
- JWT email subject + accountId/role/isVerified, Access/Refresh Token và cập nhật quyền từ DB.
- Đăng nhập BCrypt, chuyển đổi plaintext đúng lúc đăng nhập, khóa tài khoản và khôi phục OTP.
- Đăng ký Reader không tự chọn quyền/xác minh.
- Admin CRUD, lọc/sắp xếp/phân trang, Verify/Unverify và giao diện tương ứng.
- API profile/đổi mật khẩu và giao diện My account cho mọi vai trò.
- Phân quyền method security; API công khai giới hạn rõ ràng, lỗi 400/401/403 nhất quán.
- API client Auth/User, bảo vệ phiên refresh và điều hướng theo vai trò.

Workflow biên tập và comment/moderation vẫn là phạm vi các module khác. Engagement/Category/Dashboard/Portal của Dev 3 đã được khớp với phân quyền hiện tại.
