# Login và phân quyền

Triển khai 12 bước Backend của [security-plan.md](security-plan.md) trên Spring Boot 4.1.1 / Spring Security do Boot quản lý (7.x), JJWT 0.12.6 và BCrypt. Phiên đăng nhập stateless; không dùng form login, HTTP Basic hoặc session cookie.

## Chạy local

Chạy BE bằng `Start-Backend.ps1` như README. Khi bảng `SystemAccount` trống, `DataInitializer` tạo Admin `admin@fu.edu.vn` / `Admin@123` với hash BCrypt. Nếu bảng đã có dữ liệu, không thêm tài khoản mặc định và không thay đổi quyền/mật khẩu của Admin đã có.

| Biến môi trường | Mặc định / ý nghĩa |
| --- | --- |
| `JWT_SECRET` | Bỏ trống: sinh khóa ngẫu nhiên cho phiên chạy local; token cũ mất hiệu lực sau restart. Khi cần token ổn định giữa các lần chạy, cấu hình Base64 của ít nhất 32 byte ngẫu nhiên hoặc hex của ít nhất 32 byte. Không dùng khóa ví dụ trong plan. |
| `JWT_EXPIRATION_MS` | `86400000` (24 giờ), phải lớn hơn 0 |
| `JWT_ISSUER` | `NMS_BE` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`; nhiều origin cách nhau bởi dấu phẩy |
| `SEED_ADMIN_ENABLED` | `true`; chỉ tạo tài khoản khi bảng trống |
| `SEED_ADMIN_EMAIL` | `admin@fu.edu.vn` |
| `SEED_ADMIN_PASSWORD` | `Admin@123` |
| `MIGRATE_PLAINTEXT_PASSWORDS` | `true`; chuyển mật khẩu cũ sang BCrypt khi khởi động |

Khóa JWT không được commit vào source. Ví dụ sinh khóa cho biến môi trường bằng PowerShell:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
.\Start-Backend.ps1
```

BCrypt giới hạn mật khẩu ở 72 byte UTF-8. Mật khẩu mới phải dài ít nhất 6 ký tự; POST/PUT và login từ chối chuỗi vượt giới hạn byte thay vì cắt ngắn. Khi chuyển dữ liệu cũ, hash BCrypt được giữ nguyên; nếu còn plaintext dài hơn 72 byte, startup dừng và chỉ ra ID cần đặt lại mật khẩu trước, không tự cắt hoặc thay mật khẩu.

## Authentication API

`POST /api/auth/login` công khai:

```json
{"accountEmail":"admin@fu.edu.vn","accountPassword":"Admin@123"}
```

Email được tìm không phân biệt hoa/thường. Đăng nhập sai email hoặc mật khẩu cùng trả `401` với thông báo chung. Login không bị chặn bởi Bearer token cũ còn trong header.

Response thành công:

```json
{
  "token": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "accountId": 1,
  "accountName": "System Administrator",
  "accountEmail": "admin@fu.edu.vn",
  "accountRole": 1,
  "roleName": "ADMIN"
}
```

Gọi `GET /api/auth/me` với `Authorization: Bearer <JWT>` để lấy `accountId`, `accountName`, `accountEmail`, `accountRole`. Không endpoint nào trả mật khẩu/hash.

JWT có chữ ký HS256, issuer, subject là account ID, thời gian hết hạn, email, role và fingerprint của hash mật khẩu. Mỗi request tải lại tài khoản từ database: xóa tài khoản, đổi email hoặc mật khẩu làm token cũ không còn hợp lệ; đổi role áp dụng quyền mới ngay. Authority lấy từ database hiện tại, không tin role do client cung cấp. Không có endpoint refresh token hoặc đăng ký công khai trong kế hoạch này.

## Ma trận quyền

| Endpoint | Quyền |
| --- | --- |
| POST `/api/auth/login` | Công khai |
| GET `/api/auth/me` | Đã đăng nhập |
| `/api/users`, `/api/users/**` | Admin; thêm `@PreAuthorize` tại UserController |
| GET `/api/categories/**` | Admin hoặc Staff |
| Ghi `/api/categories/**` | Admin |
| GET `/api/news/**` | Đã đăng nhập (chọn phương án authenticated trong plan) |
| POST/PUT/DELETE `/api/news/**` | Admin hoặc Staff; kiểm tra chủ sở hữu khi tích hợp nghiệp vụ |
| GET `/api/dashboard/**` | Admin hoặc Staff |
| POST `/api/notifications/**` | Admin |
| Các thao tác khác `/api/notifications/**` | Đã đăng nhập; Staff chỉ được dùng recipientId của mình; Admin được thao tác cho người nhận khác |
| GET `/api/tags/**`, `/api/news-tags/**` | Admin hoặc Staff |
| Ghi `/api/tags/**`, `/api/news-tags/**` | Admin |
| Endpoint khác | Từ chối mặc định |

CORS cho phép Authorization/Content-Type và các phương thức GET/POST/PUT/PATCH/DELETE/OPTIONS từ origin cấu hình. Preflight được xử lý trước xác thực. Lỗi authentication và authorization trả `ApiErrorResponse` với `401`/`403`; validation, login, nghiệp vụ User/Auth cùng dùng handler chung. Handler nghiệp vụ vẫn giới hạn cho User và Auth để các thành viên tích hợp sau.

## Hỗ trợ nghiệp vụ của các thành viên

`SecurityUtils.getCurrentUser()` lấy `SystemAccount` từ principal đã xác thực. `applyCreator(article)` gán người tạo hiện tại. `requireArticleOwnerOrAdmin(article)` kiểm tra Staff chỉ sửa/xóa bài của mình; Admin có quyền với mọi bài. `applyUpdater(article)` kiểm tra quyền rồi gán người sửa và thời gian UTC.

Hiện NewsArticleController/Service trên nhánh này còn là khung, chưa có handler CRUD. Khi thành viên triển khai nghiệp vụ, gọi `applyCreator` trong create, `applyUpdater` trong update và `requireArticleOwnerOrAdmin` trước delete; các trường audit không lấy từ request client. Các helper đã có kiểm thử, nhưng không tuyên bố đã hoàn thành CRUD bài viết hoặc nối chúng vào service đang trống.

## Tích hợp frontend

Phần 6 của plan là hướng dẫn tích hợp; thay đổi lần này nằm ở BE. Frontend hiện vẫn có login mock, chưa chuyển sang backend.

Frontend cần gửi body login như trên, lưu token và thông tin user, thêm Bearer header qua Axios interceptor, gọi `/api/auth/me` khi khôi phục phiên, ẩn/bảo vệ trang quản lý User khi `accountRole !== 1`, xóa phiên và về `/login` khi nhận `401`. Với `403`, hiển thị thiếu quyền và giữ phiên. Đăng xuất local xóa token; server không tạo session.

## Kiểm thử và đối chiếu 12 bước

| Bước trong plan | Thành phần thực hiện |
| --- | --- |
| 1 | `pom.xml`: starter security và JJWT 0.12.6 |
| 2 | `application.properties`: JWT, CORS, seed và migration |
| 3 | `SystemAccountRepository.findByAccountEmailIgnoreCase` |
| 4 | `SecurityConfig.passwordEncoder`, service create/update, validator UTF-8 |
| 5 | `DataInitializer`: seed database trống, migrate plaintext, giữ hash cũ |
| 6 | `CustomUserDetails`, `CustomUserDetailsService` |
| 7 | `JwtTokenProvider`, `JwtAuthenticationFilter` |
| 8 | `JwtAuthenticationEntryPoint`, `CustomAccessDeniedHandler`, `SecurityErrorResponseWriter` và handler chung |
| 9 | `SecurityConfig`: CORS, stateless, ma trận quyền và filter |
| 10 | DTO login, `AuthService`, `AuthController` login/me |
| 11 | `UserController.@PreAuthorize` |
| 12 | Test local `AuthSecurityTests`, `UserCrudTests` và smoke test HTTP |

Test local dùng SQL Server thật và rollback dữ liệu, bao gồm login/me, BCrypt, giữ/đổi mật khẩu, token giả/hết hạn/khác issuer, xóa tài khoản, đổi quyền, phân quyền endpoint, CORS, migration và audit helper. Các probe chỉ tồn tại trong test để kiểm tra filter chain cho module chưa có CRUD. `src/test` vẫn được Git ignore theo cấu hình có sẵn.

Smoke test trên server đã khởi động:

```powershell
.\scripts\Test-Auth.ps1 -BaseUrl http://localhost:8080 -AdminPassword 'Admin@123'
```

Script tạo tài khoản Staff tạm, kiểm tra login/quyền/đổi mật khẩu và xóa tài khoản đó trong `finally`. Tài khoản Admin dùng để chạy test phải tồn tại trước.

Tham khảo chính thức: [Spring Security DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html), [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html), [JJWT 0.12.6](https://github.com/jwtk/jjwt/blob/0.12.6/README.adoc).
