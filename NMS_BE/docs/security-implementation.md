# Security implementation — phạm vi User/Auth

Áp dụng hướng nâng cấp trong `security-plan.md` riêng cho luồng User/Auth. Các module của thành viên khác được giữ nguyên nghiệp vụ và `permitAll()` để demo; việc tích hợp security cho những module đó sẽ thực hiện sau.

## Phạm vi hiện tại

| API | Quyền |
| --- | --- |
| POST `/api/v1/auth/login` | Công khai |
| POST `/api/v1/auth/refresh` | Công khai |
| GET `/api/v1/auth/me` | Đã đăng nhập bằng Access Token |
| `/api/v1/users`, `/api/v1/users/**` | Admin (`AccountRole = 1`) |
| Category, Settings, News, Tag, NewsTag, Notification, Dashboard | `permitAll()` để demo |
| Swagger/OpenAPI | Công khai |

Controller/service của các module ngoài User/Auth đã được khôi phục về trước lần nâng cấp. Không gắn kiểm tra role, người nhận thông báo hoặc chủ sở hữu bài viết vào những module này. NewsTag giữ đường dẫn sẵn có `/api/news-tags`; không đổi URL của thành viên khác.

`SecurityConfig` cho request đi qua bằng `permitAll()`. `UserController` và `SystemAccountServiceImpl` gọi `SecurityUtils.requireAdmin()`, còn `AuthService.me()` yêu cầu principal đã đăng nhập. Không có interceptor bắt đăng nhập toàn bộ API. Anonymous trả 401, Staff trả 403, Admin được CRUD User.

`GlobalExceptionHandler` chỉ áp dụng cho `UserController` và `AuthController`: `UnauthorizedException` → 401; `ForbiddenException` → 403; cùng cấu trúc `ApiErrorResponse`. Các module khác giữ cách xử lý lỗi trước đó.

## Đăng nhập và cấp lại token

`POST /api/v1/auth/login`:

```json
{"accountEmail":"user@example.com","accountPassword":"your-password"}
```

Email không phân biệt hoa/thường; sai email hoặc mật khẩu đều trả 401 với thông báo chung. Response:

```json
{
  "accessToken": "<JWT ACCESS>",
  "refreshToken": "<JWT REFRESH>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "accountId": 1,
  "accountName": "Example User",
  "accountEmail": "user@example.com",
  "accountRole": 1,
  "roleName": "ADMIN"
}
```

Dùng `Authorization: Bearer <accessToken>` cho User và `GET /api/v1/auth/me`. Response không chứa mật khẩu/hash. Trường `token` cũ được thay bằng `accessToken`.

`POST /api/v1/auth/refresh` nhận:

```json
{"refreshToken":"<JWT REFRESH>"}
```

Response:

```json
{"accessToken":"<JWT ACCESS mới>","refreshToken":"<JWT REFRESH ban đầu>","tokenType":"Bearer","expiresIn":1800}
```

Access Token mặc định 30 phút; Refresh Token 7 ngày. Refresh không gia hạn thời điểm hết hạn 7 ngày ban đầu. Claim `type` ngăn dùng hai loại token thay nhau. Login/refresh bỏ qua Bearer header cũ. Refresh hết hạn/sai chữ ký/issuer/loại trả 401; body thiếu/sai định dạng trả 400.

JWT ký HS256 bằng JJWT 0.12.6. Mỗi request có Access Token hoặc mỗi lần refresh đều tải lại tài khoản từ database: xóa tài khoản, đổi email hoặc mật khẩu vô hiệu cả hai token; role mới có hiệu lực ngay. Refresh là JWT stateless, không có bảng session/rotation. Logout xóa token phía client; token đã cấp vẫn hợp lệ tới khi hết hạn hoặc thông tin tài khoản/khóa ký thay đổi.

## Cấu hình và mật khẩu

| Biến môi trường | Mặc định / ý nghĩa |
| --- | --- |
| `JWT_SECRET` | Trống: khóa ngẫu nhiên mỗi lần chạy; hoặc Base64/hex của ít nhất 32 byte ngẫu nhiên |
| `JWT_ACCESS_EXPIRATION_MS` | `1800000`; fallback `JWT_EXPIRATION_MS` để tương thích cấu hình cũ |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` |
| `JWT_ISSUER` | `NMS_BE` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`; nhiều origin cách nhau dấu phẩy |
| `MIGRATE_PLAINTEXT_PASSWORDS` | `true`; chuyển plaintext cũ sang BCrypt, giữ hash hiện có |

Không seed Admin, không thêm hoặc ghi đè tài khoản mặc định. Tài khoản quản trị cần tồn tại trong database. Role Admin là 1, Staff là 2; không dựa vào email/ID. User create/update dùng BCrypt; mật khẩu mới tối thiểu 6 ký tự và tối đa 72 byte UTF-8. Migration không cắt ngắn mật khẩu quá dài.

Chạy BE bằng `Start-Backend.ps1` với JDK 21+ và SQL Server như README. Unicode/schema và các migration hiện có được giữ nguyên.

## Frontend User/Auth

`session.js` lưu cặp token và thông tin user. Phiên hết hạn Access Token nhưng còn Refresh Token được khôi phục qua `/auth/me`.

`api.js` gắn Access Token; 401 tự refresh một lần rồi thử lại. Các request đồng thời dùng chung một refresh. Refresh bị từ chối hoặc request thử lại vẫn 401 thì xóa riêng khóa xác thực và về `/login`. Lỗi mạng khi refresh giữ phiên để thử lại. 403 hiển thị thiếu quyền, không refresh hoặc logout. Response refresh cũ không thể khôi phục phiên sau logout hoặc xóa một phiên đăng nhập mới.

Sidebar chỉ hiện Users cho Admin; truy cập trực tiếp `/users` của Staff hiển thị thiếu quyền. Các trang quản lý khác giữ nguyên dữ liệu và thao tác demo.

## Kiểm thử theo phạm vi

Test được lưu trong Git: `DualTokenSecurityTests`, bao gồm login/refresh, hết hạn, đổi email/password/role, quyền CRUD User, và xác nhận các API demo ngoài User/Auth vẫn dùng được không cần đăng nhập (cả khi header chứa token sai).

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\mvnw.cmd clean verify '-Dtest=DualTokenSecurityTests' '-Dspring.jpa.hibernate.ddl-auto=validate' '-Dapp.migrate-plaintext-passwords=false'
```

Các test tích hợp local cũ về User/Auth có thể chạy thêm bằng `-Dtest=DualTokenSecurityTests,AuthSecurityTests,UserCrudTests,DataInitializerTests,SwaggerDocumentationTests`. Những test local cũ yêu cầu security ở module khác không còn phản ánh phạm vi demo hiện tại.

Frontend: `npm test`, `npm run lint`, `npm run build`.

Smoke test server đang chạy (chỉ User/Auth):

```powershell
.\scripts\Test-Auth.ps1 -BaseUrl http://localhost:8080 -AdminEmail '<email Admin>' -AdminPassword '<password>'
```

Script tạo Staff tạm và xóa trong `finally`.

Tham khảo: [JJWT 0.12.6](https://github.com/jwtk/jjwt/blob/0.12.6/README.adoc), [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html).
