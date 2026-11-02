# NMS Backend

Spring Boot 4.1.1, Java 21+, Hibernate/JPA và SQL Server.

## Login và phân quyền

Phân quyền theo `plan_report2`: 1=Admin, 2=Contributor, 3=Editor, 4=Moderator, 5=Reader. API nội bộ yêu cầu JWT; quyền chi tiết nằm tại controller/service qua `@PreAuthorize`. Login/đăng ký/refresh/OTP và API đọc tin công khai được mở. Access Token 30 phút, Refresh Token 7 ngày, BCrypt. Không seed tài khoản Admin; tài khoản quản trị phải tồn tại trong database. Mật khẩu plaintext cũ được chuyển sang BCrypt ngay khi đăng nhập đúng; migration hàng loạt lúc khởi động vẫn là tùy chọn.

Chi tiết cấu hình JWT/CORS, ma trận quyền, tích hợp frontend và kiểm thử: [Security implementation](docs/security-implementation.md). Request/response CRUD User: [User API](docs/user-api.md). Kế hoạch gốc: [Security plan](docs/security-plan.md).

### Khóa tài khoản khi đăng nhập sai

Sai mật khẩu quá 3 lần liên tiếp (lần sai thứ 4) sẽ khóa tài khoản hẳn, không tự hết hạn. Đăng nhập thành công trước khi bị khóa sẽ đặt `FailedLoginAttempts` về 0. Bộ đếm và `AccountLocked` được lưu trong database; tài khoản bị khóa không thể đăng nhập, dùng Access Token hoặc Refresh Token trên các API có bảo vệ. Người dùng chọn **Forgot password** tại trang đăng nhập để đặt lại mật khẩu bằng email OTP, dù tài khoản có đang bị khóa hay không.

Luồng quên mật khẩu/mở khóa dùng chung cho cả tài khoản đang hoạt động và tài khoản bị khóa: nhập email tài khoản → nhận OTP 6 chữ số qua email → xác thực OTP → nhập và xác nhận mật khẩu mới → lưu mật khẩu; nếu đang bị khóa thì đồng thời mở khóa. Xác thực OTP chưa thay đổi mật khẩu hoặc trạng thái khóa. Người dùng phải đặt mật khẩu khác mật khẩu cũ rồi đăng nhập lại. Đổi mật khẩu vô hiệu hóa Access/Refresh Token cũ. Giữ các endpoint `/unlock/*` để tương thích với frontend hiện tại.

| Endpoint POST | Request | Kết quả |
| --- | --- | --- |
| `/api/v1/auth/unlock/request` | `accountEmail` | Gửi OTP đến email trong database, kể cả tài khoản chưa bị khóa |
| `/api/v1/auth/unlock/verify` | `accountEmail`, `otp` | Trả `resetToken` dùng một lần, hiệu lực 5 phút |
| `/api/v1/auth/unlock/reset-password` | `accountEmail`, `resetToken`, `newPassword` | Đổi mật khẩu BCrypt, xóa thông tin khôi phục, đặt bộ đếm về 0 và mở khóa |

OTP hết hạn sau 5 phút, chỉ dùng một lần; sai 5 lần sẽ vô hiệu mã hiện tại. Mỗi tài khoản chỉ gửi lại sau 60 giây. Gửi lại sẽ thay mã cũ và vô hiệu quyền đổi mật khẩu đã cấp. OTP và reset token chỉ lưu dạng hash; API không trả OTP. Request gửi OTP trả thông báo chung, không tiết lộ email có tồn tại hay trạng thái khóa. Nếu gửi email thất bại, challenge được rollback để có thể thử lại.

Spring Boot tự chạy các migration `001`–`006` trước Hibernate mỗi lần khởi động, bao gồm migration khóa tài khoản và OTP. Cơ chế này áp dụng cả khi chạy IntelliJ, Maven và JAR. Thời điểm hết hạn được lưu theo UTC.

### Cấu hình email OTP

Gửi mail dùng [Spring Boot Mail / JavaMailSender](https://docs.spring.io/spring-boot/reference/io/email.html). Cấu hình SMTP trong biến môi trường của backend (hoặc Run Configuration của IDE), ví dụ:

```powershell
$env:MAIL_HOST = 'smtp.example.com'
$env:MAIL_PORT = '587'
$env:MAIL_USERNAME = 'noreply@example.com'
$env:MAIL_PASSWORD = '<SMTP password or app password>'
$env:MAIL_FROM = 'noreply@example.com'
# Sau đó chạy backend với cấu hình database như bên dưới.
```

Mặc định bật SMTP authentication và STARTTLS (bắt buộc). `MAIL_SMTP_AUTH=false` và `MAIL_STARTTLS=false` chỉ dùng cho SMTP local phục vụ kiểm thử nếu cần. Backend vẫn khởi động khi chưa cấu hình email; yêu cầu gửi OTP cho tài khoản tồn tại sẽ trả HTTP 503 nếu chưa cấu hình hoặc SMTP không gửi được. Không có mã OTP giả hoặc fallback ghi OTP ra log.

## Swagger UI

Dùng `springdoc-openapi` 3.1.1 cho Spring Boot 4; tham khảo [tài liệu chính thức](https://springdoc.org/getting-started.html).

Sau khi khởi động backend:

- Swagger UI: http://localhost:8080/swagger-ui/index.html (hoặc `/swagger-ui.html`).
- OpenAPI JSON: http://localhost:8080/v3/api-docs; YAML: `/v3/api-docs.yaml`.
- Tài liệu chỉ hiển thị các API nghiệp vụ `/api/v1/**` đã được triển khai.

Để thử API có bảo vệ, mở `POST /api/v1/auth/login`, chọn **Try it out**, nhập email/mật khẩu của tài khoản trong database và **Execute**. Sao chép trường `accessToken` trong response, bấm **Authorize**, dán token (không thêm `Bearer `) và xác nhận. Swagger tự gửi Bearer header; backend kiểm tra role từ database như các request thông thường. Khi token hết hạn hoặc restart với khóa JWT ngẫu nhiên, đăng nhập và Authorize lại.

Trang tài liệu mở công khai; API nghiệp vụ vẫn áp dụng xác thực và quyền theo vai trò.

## Chạy trên Windows

Yêu cầu SQL Server đang chạy trên `localhost:1433`, bật SQL Server Authentication (Mixed Mode), login `sa` được bật và `sqlcmd` có trong PATH. Cấu hình mặc định dùng login `sa`; mật khẩu lấy từ biến môi trường `DB_PASSWORD`. Có thể đặt `DB_USERNAME` để dùng login khác có quyền tạo database và chạy migration.

Tại thư mục `NMS_BE`, chạy:

```powershell
$env:DB_PASSWORD = '<mật khẩu sa trong SQL Server>'
.\Start-Backend.ps1
```

Script tìm JDK 21+ trong `JAVA_HOME`, `%USERPROFILE%\.jdks` hoặc thư mục cài Java, tạo database `FUNewsManagementSystem` nếu chưa có và khởi động backend tại `http://localhost:8080`. Có thể chỉ định JDK:

```powershell
.\Start-Backend.ps1 -JdkHome 'C:\path\to\jdk-21'
```

Hibernate dùng `ddl-auto=update` để tạo/cập nhật bảng và giữ dữ liệu hiện có. Hibernate không tạo database SQL Server; script `database/create-database.sql` thực hiện bước đó. Cấu hình này dành cho phát triển local; khi triển khai dùng migration và `ddl-auto=validate`.

## Chạy thủ công hoặc bằng IDE

Chỉ cần database đích đã tồn tại và tài khoản kết nối có quyền chạy migration. Khi khởi động, Spring Boot tự chạy lại các script trong `database/migrations` theo thứ tự `001` đến `006`, trước khi Hibernate khởi tạo schema. Các script được đóng gói trong JAR tại `db/migrations`, nên không phụ thuộc working directory. Khi mở dự án bằng IntelliJ, reload Maven để nhận cấu hình resource mới.

`006-article-editorial-workflow.sql` gỡ ràng buộc trạng thái `0/1` cũ, đổi `NewsStatus` sang `varchar(30)`, chuyển `1` thành `PUBLISHED`, `0` thành `ARCHIVED` và bổ sung thời điểm xuất bản. Các migration có thể chạy lại và giữ dữ liệu hiện có, không đặt lại password, trạng thái khóa/Verify hoặc bộ đếm tương tác đã tồn tại. Migration lỗi sẽ dừng khởi động, không bị bỏ qua.

Cấu hình trong `application.properties`: `spring.sql.init.mode=always`, `spring.sql.init.schema-locations` liệt kê các script theo thứ tự và `spring.jpa.defer-datasource-initialization=false`. Mỗi file T-SQL được chạy thành một batch để giữ nguyên các khối `BEGIN/END` và câu SQL động. Khi thêm migration mới, bổ sung vào danh sách `schema-locations`; script phải hỗ trợ chạy lặp lại. Có thể tắt riêng cho môi trường do công cụ khác quản lý schema bằng `SPRING_SQL_INIT_MODE=never`.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:DB_PASSWORD = '<mật khẩu sa trong SQL Server>'
$env:SQLCMDPASSWORD = $env:DB_PASSWORD
sqlcmd -S 'tcp:localhost,1433' -U sa -C -b -i .\database\create-database.sql
.\mvnw.cmd spring-boot:run
```

Maven profile `windows-auth` hiện vẫn tự kích hoạt trên Windows và tải DLL Microsoft JDBC vào `target/native`; kết nối SQL Authentication không cần DLL này. Khi chạy bằng IDE, đặt biến môi trường `DB_PASSWORD` trong Run Configuration và working directory là `NMS_BE`.

Build và chạy JAR:

```powershell
.\mvnw.cmd clean package
java '-Djava.library.path=target/native' -jar .\target\NMS_BE-0.0.1-SNAPSHOT.jar
```

`java` dùng để chạy JAR cũng phải là Java 21+. Trên máy có Java 17 trong PATH, dùng `& "$env:JAVA_HOME\bin\java.exe"` thay cho `java`.

## Login hoặc instance khác

Sau khi tạo database trên instance đích và cấp quyền cho SQL login, có thể cấu hình qua biến môi trường:

```powershell
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=FUNewsManagementSystem;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = '<SQL login>'
$env:DB_PASSWORD = '<password>'
.\mvnw.cmd spring-boot:run
```

Không cần bật profile `sql-auth`; profile này vẫn được giữ để tương thích với cấu hình chạy cũ. Không commit mật khẩu. `trustServerCertificate=true` chỉ dùng cho SQL Server local có chứng chỉ tự ký. Có thể đổi cổng HTTP bằng `SERVER_PORT` và bật SQL log bằng `SHOW_SQL=true`. Script `Start-Backend.ps1` tạo database trên `localhost:1433`; khi dùng instance khác, tạo database đích trước rồi chạy Maven với `DB_URL` tương ứng. Các migration tự chạy trên datasource được cấu hình.

## Entity và quy tắc

| Entity | Khóa / quan hệ |
| --- | --- |
| `SystemAccount` | `AccountID` tự tăng; email duy nhất; role `1=Admin`, `2=Contributor`, `3=Editor`, `4=Moderator`, `5=Reader` |
| `Category` | `CategoryID` tự tăng; `ParentCategoryID` tham chiếu cùng bảng, có thể NULL |
| `NewsArticle` | `NewsArticleID` tự tăng; FK bắt buộc tới category và người tạo; người sửa có thể NULL |
| `Tag` | `TagID` tự tăng; tên và ghi chú |
| `NewsTag` | Khóa ghép `(NewsArticleID, TagID)` bằng `NewsTagId`, `@EmbeddedId` và `@MapsId` |
| `Notification` | `NotificationID` tự tăng; FK bắt buộc tới người nhận; FK bài viết có thể NULL; `IsRead` chỉ nhận `0/1` |

Giữ nguyên tên bảng/cột trong sơ đồ report. Entity dùng `@Column` cho các trường chuỗi; nội dung bài viết chỉ định `nvarchar(max)`. Trạng thái bài viết theo Report 2: `DRAFT`, `PENDING_REVIEW`, `CHANGES_REQUESTED`, `APPROVED`, `SCHEDULED`, `PUBLISHED`, `ARCHIVED`; trạng thái danh mục vẫn là `0/1`. ID do SQL Server sinh, không dùng `Date.now()` từ frontend.

Các cột văn bản đa ngôn ngữ được khai báo `nvarchar` rõ ràng. Migration `database/migrations/001-unicode-text-columns.sql` tự chạy trước Hibernate cho cả IDE/Maven/JAR. Script giữ nguyên bản ghi và bỏ qua cột đã là `nvarchar`.

Quan hệ tag theo sơ đồ nhiều–nhiều: một tag được dùng bởi nhiều bài. Phần chữ trong report nói tag thuộc tối đa một bài và mâu thuẫn với sơ đồ; thiết kế hiện tại ưu tiên sơ đồ.

Xóa bài qua Hibernate sẽ xóa các `NewsTag` của bài, giữ tag/category/account. Xóa category/account/tag đang được tham chiếu bị FK chặn. SQL DELETE trực tiếp không có cascade tự động. `ParentCategoryID` không được bằng chính `CategoryID`; chu trình qua nhiều danh mục phải được kiểm tra ở service khi bổ sung CRUD.

`CreatedDate` được điền khi tạo và không cập nhật; `ModifiedDate` được điền khi sửa bài. Thời gian do entity tự sinh được lưu theo UTC. `CreatedBy` / `UpdatedBy` phải được service gán từ người đăng nhập. Entity không tự thực hiện phân quyền hoặc hash mật khẩu; khi bổ sung authentication, lưu password hash vào `AccountPassword` và dùng DTO để không trả mật khẩu/đồ thị entity trực tiếp ra API.

`Notification` gồm `NotificationID`, `RecipientID`, `NewsArticleID`, `Title`, `Content`, `IsRead` và `CreatedDate`. Mặc định chưa đọc (`0`); đọc rồi là `1`. Tiêu đề/nội dung dùng `nvarchar` qua `@Column`, không dùng `@Nationalized`. Có index theo người nhận, trạng thái đọc, thời gian tạo và bài viết. Không cascade xóa account hoặc bài viết; FK bảo vệ các tham chiếu, service phải xử lý thông báo liên quan trước khi xóa bài. Thông báo hệ thống có thể để `NewsArticleID` NULL.

## Kiểm thử

Sau khi tạo database, chạy với JDK 21+:

```powershell
.\mvnw.cmd test
```

Toàn bộ `src/test/` và thư mục test frontend được Git ignore, chỉ giữ trên máy local. Test local dùng SQL Server thật; các test thông thường rollback dữ liệu, còn test khóa/mở khóa kiểm tra commit qua từng request rồi xóa tài khoản test. Test OTP thay SMTP bằng mock để kiểm tra nội dung email, mã sai/hết hạn/dùng lại, đổi mật khẩu, chặn token cũ và rollback khi gửi email lỗi. Test error dispatch dùng HTTP thật, test sinh DDL Unicode không cần kết nối database. Các lỗi SQL trong test kiểm tra ràng buộc là lỗi được kỳ vọng. Xem lệnh `clean verify` trong tài liệu Security để kiểm thử với seed/migration tắt và schema ở chế độ validate.

Package `com.mvc.nms_be.repository` có 6 interface kế thừa `JpaRepository`, cùng các query cho tài khoản, thông báo và Dashboard. Năm entity có khóa đơn dùng `Integer`; `NewsTagRepository` dùng khóa ghép `NewsTagId`. Frontend hiện vẫn dùng mock data.

## Controller

Package `controller` có 8 controller, gắn `@RestController`, `@RequestMapping` và Lombok `@RequiredArgsConstructor` để inject service qua constructor. Tất cả API nghiệp vụ dùng `/api/v1`; không giữ alias URL cũ:

| Controller | Đường dẫn gốc | Service |
| --- | --- | --- |
| `UserController` | `/api/v1/users` | `SystemAccountService` |
| `CategoryController` | `/api/v1/categories` | `CategoryService` |
| `NewsArticleController` | `/api/v1/news` | `NewsArticleService` |
| `TagController` | `/api/v1/tags` | `TagService` |
| `NewsTagController` | `/api/v1/news-tags` | `NewsTagService` |
| `NotificationController` | `/api/v1/notifications` | `NotificationService` |
| `DashboardController` | `/api/v1/dashboard` | `DashboardService` |
| `AuthController` | `/api/v1/auth` | `AuthService` |

Auth, User, Notification và Dashboard đã có handler nghiệp vụ. Category, NewsArticle, Tag và NewsTag hiện vẫn là khung. Spring tự quản lý controller/service, không cần thêm cấu hình bean.

## Service

Package `service` định nghĩa nghiệp vụ qua interface; `service/impl` chứa implementation gắn `@Service`. Auth, SystemAccount, Notification và Dashboard đã có implementation. Category, NewsArticle, Tag và NewsTag sẵn sàng để các thành viên bổ sung nghiệp vụ.

## DTO và mapper

Mỗi entity có DTO riêng trong `dto/request` và `dto/response`, dùng Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`. Các request không chứa ID tự tăng, thời gian tạo/sửa hay người tạo/sửa. `NewsTagRequestDto` chứa hai ID tạo khóa ghép. Response dùng ID cho các quan hệ, không chứa entity hoặc Hibernate proxy; `SystemAccountResponseDto` không chứa mật khẩu.

Package `mapper` gồm 6 mapper viết tay, có `@Component` để inject vào service. `toEntity(...)` tạo entity mới từ request bằng setter; `toResponseDto(...)` tạo response bằng constructor. Không cần MapStruct hoặc thư viện mapping khác.

Ví dụ map danh sách trong service sau này:

```java
public List<CategoryResponseDto> getAll() {
    return categoryRepository.findAll().stream()
            .map(categoryMapper::toResponseDto)
            .toList();
}
```

Service chịu trách nhiệm validate request, lấy các entity liên quan từ ID trong request rồi truyền vào mapper. `NewsArticleMapper.toEntity` nhận category, người tạo từ phiên đăng nhập và danh sách tag đã lấy từ database. `CategoryMapper.toEntity` nhận category cha, hoặc `null` cho danh mục gốc. `NewsTagMapper.toEntity` nhận bài viết và tag ứng với hai ID trong request. `SystemAccountMapper.toEntity` nhận password hash đã được service tạo, không chép mật khẩu thô từ request vào entity.

`NotificationMapper.toEntity` nhận `NotificationRequestDto`, người nhận tương ứng `recipientId` và bài viết tương ứng `newsArticleId` (hoặc NULL). Response chỉ trả ID quan hệ, tiêu đề, nội dung, trạng thái đọc và thời gian tạo. Chưa triển khai tạo thông báo tự động, đánh dấu đã đọc, realtime hay gửi email; các chức năng đó được bổ sung ở service/controller sau này.

Request DTO hiện dùng cho dữ liệu tạo/cập nhật đầy đủ, không định nghĩa PATCH. `toEntity` tạo entity mới; khi triển khai cập nhật, service phải lấy entity đang có rồi cập nhật các trường được phép, giữ ID và thông tin audit. Map response bài viết trong service có `@Transactional(readOnly = true)` hoặc sau khi đã tải danh sách tag, vì `newsTags` dùng LAZY và `open-in-view=false`.

## API Category và Setting

| Method | Đường dẫn | Mô tả |
| --- | --- | --- |
| GET | `/api/v1/categories`, `/api/v1/categories/{id}` | Danh sách / chi tiết danh mục |
| POST | `/api/v1/categories` | Tạo danh mục (201); `parentCategoryId = null` là danh mục gốc |
| PUT | `/api/v1/categories/{id}` | Cập nhật; chặn danh mục cha tạo vòng lặp (400) |
| DELETE | `/api/v1/categories/{id}` | Xóa (204); còn danh mục con hoặc bài viết thì 409 |
| GET | `/api/v1/settings`, `/api/v1/settings/{id}`, `/api/v1/settings/key/{key}` | Danh sách / chi tiết cấu hình |
| POST / PUT / DELETE | `/api/v1/settings`, `/api/v1/settings/{id}` | Tạo (201) / sửa / xóa (204); `settingKey` trùng thì 409 |

`SystemSetting` là bảng cấu hình key/value (`SettingID`, `SettingKey` duy nhất, `SettingValue`, `Description`). Lỗi nghiệp vụ trả qua `ResponseStatusException` dạng JSON `{status, error, message, path}`. Swagger UI: `http://localhost:8080/swagger-ui.html`; Postman collection: `docs/NMS_Category_Setting.postman_collection.json`. Category và Setting cần JWT: đăng nhập `POST /api/v1/auth/login`, gửi header `Authorization: Bearer <token>`. Chỉ Admin quản lý cấu hình hệ thống; taxonomy đọc công khai, ghi chỉ dành cho Admin. Các controller Category/Settings hiện chưa có CRUD handler.

Tài liệu: [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Hibernate mapping](https://docs.hibernate.org/orm/7.4/userguide/html_single/), [Microsoft JDBC driver](https://github.com/microsoft/mssql-jdbc).
