# User CRUD

Luồng: `UserController` → `SystemAccountService` → `SystemAccountServiceImpl` → `SystemAccountRepository` → SQL Server. Mapper chuyển đổi request/entity/response; response không chứa mật khẩu. Các API quản trị yêu cầu JWT Bearer của Admin; `/users/me` và `/users/me/password` dành cho mọi tài khoản đã đăng nhập, chỉ thao tác trên tài khoản hiện tại. Xem [hướng dẫn login và phân quyền](security-implementation.md).

| Method | Endpoint | Kết quả |
| --- | --- | --- |
| GET | `/api/v1/users` | `200`, danh sách phân trang, tìm kiếm và lọc bằng Pageable |
| GET | `/api/v1/users/{id}` | `200`, chi tiết user |
| POST | `/api/v1/users` | `201`, user mới và header `Location` |
| PUT | `/api/v1/users/{id}` | `200`, cập nhật đầy đủ user, giữ nguyên ID |
| DELETE | `/api/v1/users/{id}` | `204`, không có body |

## Phân trang, tìm kiếm và lọc

Controller nhận `Pageable`; service dùng `JpaSpecificationExecutor.findAll(specification, pageable)`
để lọc, sắp xếp, đếm và phân trang ngay trong SQL Server.

Ví dụ: `GET /api/v1/users?page=0&size=10&keyword=nguyen&role=2&sort=accountName,asc`.

| Tham số | Ý nghĩa |
| --- | --- |
| `page` | Trang bắt đầu từ 0, mặc định 0 |
| `size` | Số dòng/trang, từ 1 đến 100, mặc định 10 |
| `keyword` | Tìm trong tên hoặc email, không phân biệt hoa/thường, tối đa 254 ký tự |
| `accountId` | Lọc đúng ID nguyên dương |
| `name` | Tên có chứa chuỗi tìm kiếm, tối đa 100 ký tự |
| `email` | Email có chứa chuỗi tìm kiếm, tối đa 254 ký tự |
| `role` | `1=Admin`, `2=Contributor`, `3=Editor`, `4=Moderator`, `5=Reader`; bỏ qua để lấy tất cả |
| `isVerified` | `true` hoặc `false`; bỏ qua để lấy mọi trạng thái xác minh |
| `sort` | Chuẩn Pageable: `accountId,asc`, `accountName,desc`, `accountEmail,asc` , `accountRole,asc` hoặc `isVerified,desc` |

Các điều kiện lọc kết hợp bằng AND; riêng keyword tìm tên OR email. Chuỗi tìm kiếm được bỏ
khoảng trắng đầu/cuối, các ký tự `%`, `_`, `[` và `!` được tìm theo nghĩa đen.
Có thể lặp tham số `sort` để sắp xếp nhiều cột. Mặc định ID tăng dần; khi sắp xếp cột khác,
ID được thêm làm tiêu chí phụ để các trang có thứ tự ổn định. Không cho sắp xếp theo mật khẩu.
Tham số không hợp lệ trả HTTP 400.

Response luôn là object phân trang (thay cho mảng user trước đây):

```json
{
  "content": [
    { "accountId": 12, "accountName": "Nguyen Van A", "accountEmail": "a@example.com", "accountRole": 2, "roleName": "CONTRIBUTOR", "isVerified": true }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

Trang vượt phạm vi trả `content: []` cùng tổng số kết quả thật. FE tự chuyển về trang cuối hợp lệ
khi xóa hết dòng trong trang cuối. Đổi bộ lọc, sắp xếp hoặc số dòng/trang sẽ về trang đầu.

## Thêm và sửa User

Body POST/PUT:

```json
{
  "accountName": "Nguyen Van A",
  "accountEmail": "a@example.com",
  "accountRole": 2,
  "accountPassword": "secret123"
}
```

- `accountName`: bắt buộc, không chỉ chứa khoảng trắng, tối đa 100 ký tự. Bỏ khoảng trắng đầu/cuối khi lưu.
- `accountEmail`: bắt buộc, đúng định dạng email, tối đa 254 ký tự. Lưu chữ thường, kiểm tra trùng không phân biệt hoa/thường.
- `accountRole`: khi tạo hoặc đổi role, chỉ chấp nhận `2=Contributor`, `3=Editor`, `4=Moderator`, `5=Reader`. Không được tạo tài khoản Admin hoặc đổi tài khoản khác sang Admin. Tài khoản Admin hiện có phải giữ `accountRole=1` khi cập nhật; vẫn được sửa tên, email và reset mật khẩu. Khi bỏ qua thuộc tính, mặc định là `2`; không nhận `null`.
- `accountPassword`: POST bắt buộc; PUT có thể bỏ qua hoặc truyền `null` để giữ nguyên hash hiện tại. Khi cung cấp mật khẩu mới, không được chỉ chứa khoảng trắng, dài 6–72 ký tự và tối đa 72 byte UTF-8 (giới hạn BCrypt). Service luôn BCrypt encode mật khẩu mới trước khi lưu.
- ID trong URL phải là số nguyên dương.

DTO dùng constructor không tham số để JSON binding giữ giá trị role mặc định. Validation chạy ở controller qua `@Valid` và ở service qua Spring bean `@Validated`. Các thao tác ghi dùng transaction; PUT lấy entity hiện có rồi cập nhật.

Exception được thiết kế dùng chung: `ResourceNotFoundException`, `ResourceConflictException`, `ApiErrorResponse` và `GlobalExceptionHandler`. Handler áp dụng các controller nghiệp vụ trong package `com.mvc.nms_be.controller`. `UnauthorizedException` (401) và `ForbiddenException` (403) được ném tại MVC/Controller/Service và trả cùng cấu trúc JSON; lỗi tại security filter cũng trả cùng cấu trúc JSON.

Các mã lỗi hiện áp dụng cho User:

| Mã HTTP | Trường hợp |
| --- | --- |
| 400 | Thuộc tính không hợp lệ, ID sai, thiếu body hoặc JSON sai |
| 401 | Thiếu JWT, JWT không hợp lệ/hết hạn hoặc tài khoản không còn hợp lệ |
| 403 | Tài khoản không có role Admin |
| 404 | User không tồn tại |
| 409 | Email trùng; tạo Admin, đổi sang/đổi khỏi role Admin, xóa Admin; hoặc xung đột ràng buộc database, gồm xóa user đang được tham chiếu |
| 500 | Lỗi ngoài dự kiến, trả thông báo chung |

Service chặn xóa mọi tài khoản Admin, kể cả Admin hiện tại. Các tài khoản còn lại được database kiểm tra khóa ngoại; repository flush trong transaction để lỗi được xử lý ngay trong request.

Ví dụ lỗi validation:

```json
{
  "timestamp": "2026-10-19T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "errors": {
    "accountEmail": "Email must be a valid email address"
  }
}
```

Lỗi không chứa mật khẩu hoặc giá trị đầu vào bị từ chối. DTO tạo là `SystemAccountRequestDto`; DTO cập nhật là `SystemAccountUpdateRequestDto`, cho phép giữ mật khẩu hiện tại.

## Xác minh và tài khoản cá nhân (Report 2)

| Method | Endpoint | Quyền / Body |
| --- | --- | --- |
| PATCH | `/api/v1/users/{id}/verify` | Chỉ Admin; đảo trạng thái xác minh, trả profile mới |
| GET | `/api/v1/users/me` | Mọi tài khoản đã đăng nhập; ID lấy từ SecurityContext |
| PUT | `/api/v1/users/me/password` | Mọi tài khoản đã đăng nhập; `{currentPassword, newPassword}`, trả 204 |
| POST | `/api/v1/auth/register-reader` | Công khai; `{accountName, accountEmail, accountPassword}`, trả 201 |

Đăng ký luôn tạo Reader chưa xác minh; bỏ qua role/isVerified giả mạo trong body. Mật khẩu đăng ký dài 6–50 ký tự, tối đa 72 byte UTF-8. Các role Contributor, Editor, Moderator chỉ do Admin tạo hoặc phân quyền tại trang quản lý user qua API `/users`; tài khoản do Admin tạo mặc định đã xác minh.

Đổi mật khẩu yêu cầu mật khẩu cũ đúng và mật khẩu mới khác mật khẩu cũ, dài 6–72 ký tự, tối đa 72 byte UTF-8. Sai mật khẩu cũ trả 400 và giữ phiên, không trả 401 để tránh refresh/logout nhầm. Thành công vô hiệu cả token cũ và mã khôi phục đang tồn tại; frontend yêu cầu đăng nhập lại. Admin thay email hoặc mật khẩu cũng vô hiệu challenge OTP/reset cũ. Thay đổi này không tự mở khóa tài khoản đang khóa.

Response profile có `accountId`, `accountName`, `accountEmail`, `accountRole`, `roleName`, `isVerified`; không có hash, mật khẩu hoặc thông tin OTP/reset. Frontend truy cập hồ sơ tại `/account`.
