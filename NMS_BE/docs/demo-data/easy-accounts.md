# Tài khoản demo dễ nhớ

Mật khẩu chung cho các tài khoản trong bảng: `123456`.

Tạo qua POST /api/v1/users bằng Admin, sau đó kiểm tra đăng nhập từng tài khoản qua POST /api/v1/auth/login. Các tài khoản mới đều Active và Verified. Không tạo thêm Admin hoặc thay đổi tài khoản đã có.

| Role | Email | ID | Đăng nhập |
| --- | --- | ---: | --- |
| CONTRIBUTOR | contributor1@example.com | 3237 | Đạt |
| CONTRIBUTOR | contributor2@example.com | 3238 | Đạt |
| CONTRIBUTOR | contributor3@example.com | 3239 | Đạt |
| EDITOR | editor1@example.com | 3240 | Đạt |
| EDITOR | editor2@example.com | 3241 | Đạt |
| EDITOR | editor3@example.com | 3242 | Đạt |
| MODERATOR | moderator1@example.com | 3243 | Đạt |
| MODERATOR | moderator2@example.com | 3244 | Đạt |
| MODERATOR | moderator3@example.com | 3245 | Đạt |
| READER | reader1@example.com | 3246 | Đạt |
| READER | reader2@example.com | 3247 | Đạt |
| READER | reader3@example.com | 3248 | Đạt |

Kết quả: 12 tài khoản được tạo; 12 lượt đăng nhập kiểm tra thành công.

[Nhật ký API và lỗi nếu có](easy-accounts-api-log.json)

Không ghi nhận API lỗi.
