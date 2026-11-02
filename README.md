# FU News Management System

Repository chung cho backend và frontend. Cấu trúc này được thử trên nhánh `addFEtoGithub`.

```text
.
├── NMS_BE/     # Spring Boot, Maven, Java 21+, SQL Server
├── NMS_FE/     # React, Vite, npm
├── .gitignore
└── README.md
```

## Chạy backend

Mở terminal tại thư mục gốc repository:

```powershell
cd NMS_BE
$env:DB_PASSWORD = '<mật khẩu sa trong SQL Server>'
.\Start-Backend.ps1
```

Yêu cầu JDK 21+, SQL Server tại `localhost:1433`, bật SQL Server Authentication cho login `sa` và có `sqlcmd`. Cấu hình mặc định dùng tài khoản `sa`, mật khẩu lấy từ biến môi trường `DB_PASSWORD`; dùng `DB_USERNAME` để chọn login khác. Script tạo database nếu chưa có, chạy migration hiện có và khởi động backend ở `http://localhost:8080`.

Khi mở bằng IDE, import `NMS_BE/pom.xml` và đặt working directory của backend là `NMS_BE/`.

Xem [hướng dẫn backend](NMS_BE/README.md) để cấu hình JDK, SQL Authentication và chạy thủ công.

## Chạy frontend

Mở terminal thứ hai tại thư mục gốc repository, dùng phiên bản Node.js đáp ứng yêu cầu của Vite trong lockfile:

```powershell
cd NMS_FE
npm ci
npm run dev
```

Mở địa chỉ Vite in ra trên terminal, mặc định `http://localhost:5173`. Vite chuyển tiếp `/api` đến backend tại `http://localhost:8080`.

Để dùng backend khác, tạo `NMS_FE/.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Khởi động lại Vite sau khi đổi cấu hình. Giá trị `VITE_*` được đưa vào frontend nên chỉ dùng cho cấu hình công khai. Xem thêm [hướng dẫn frontend](NMS_FE/README.md).

## Kiểm tra và build

Chạy tại `NMS_FE/`:

```powershell
npm test
npm run lint
npm run build
```

Chạy tại `NMS_BE/` với JDK 21+:

```powershell
.\mvnw.cmd -DskipTests package
```

Lệnh trên build backend và bỏ qua việc chạy test tích hợp. Chạy test cần cấu hình SQL Server theo hướng dẫn backend.

## Làm việc trên nhánh thử

Commit và push thay đổi cấu trúc trên `addFEtoGithub`. Chỉ gộp vào `develop` hoặc `master` sau khi nhóm thống nhất. Các thành viên cần cập nhật nhánh và đường dẫn làm việc khi nhận cấu trúc mới.

Dependencies, kết quả build, cấu hình local và thông tin bí mật được bỏ qua bằng `.gitignore`. Giữ `NMS_FE/package-lock.json` trong Git để cài dependencies bằng `npm ci`.
