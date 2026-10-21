# NMS Backend

Spring Boot 4.1.1, Java 21+, Hibernate/JPA và SQL Server.

## Login và phân quyền

Đã nâng cấp riêng luồng User/Auth: login, refresh, me tại `/api/v1/auth`, Access Token 30 phút, Refresh Token 7 ngày, BCrypt và kiểm tra quyền Admin khi CRUD User. Các module khác giữ `permitAll()` để thành viên demo và sẽ tích hợp security sau. Không seed tài khoản Admin; tài khoản quản trị phải tồn tại trong database. Mật khẩu plaintext cũ vẫn được chuyển sang BCrypt khi bật migration.

Chi tiết cấu hình JWT/CORS, ma trận quyền, tích hợp frontend và kiểm thử: [Security implementation](docs/security-implementation.md). Request/response CRUD User: [User API](docs/user-api.md). Kế hoạch gốc: [Security plan](docs/security-plan.md).

## Swagger UI

Dùng `springdoc-openapi` 3.1.1 cho Spring Boot 4; tham khảo [tài liệu chính thức](https://springdoc.org/getting-started.html).

Sau khi khởi động backend:

- Swagger UI: http://localhost:8080/swagger-ui/index.html (hoặc `/swagger-ui.html`).
- OpenAPI JSON: http://localhost:8080/v3/api-docs; YAML: `/v3/api-docs.yaml`.
- Tài liệu chỉ hiển thị các API nghiệp vụ `/api/v1/**` đã được triển khai.

Để thử API có bảo vệ, mở `POST /api/v1/auth/login`, chọn **Try it out**, nhập email/mật khẩu của tài khoản trong database và **Execute**. Sao chép trường `accessToken` trong response, bấm **Authorize**, dán token (không thêm `Bearer `) và xác nhận. Swagger tự gửi Bearer header; backend kiểm tra role từ database như các request thông thường. Khi token hết hạn hoặc restart với khóa JWT ngẫu nhiên, đăng nhập và Authorize lại.

Trang tài liệu mở công khai. Hiện chỉ User/Auth được bảo vệ; các module nghiệp vụ khác giữ `permitAll()` để demo.

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

Không cần bật profile `sql-auth`; profile này vẫn được giữ để tương thích với cấu hình chạy cũ. Không commit mật khẩu. `trustServerCertificate=true` chỉ dùng cho SQL Server local có chứng chỉ tự ký. Có thể đổi cổng HTTP bằng `SERVER_PORT` và bật SQL log bằng `SHOW_SQL=true`. Script `Start-Backend.ps1` chuẩn bị database trên `localhost:1433`; khi dùng instance khác, chạy các SQL script trên instance đích trước rồi chạy Maven như ví dụ trên.

## Entity và quy tắc

| Entity | Khóa / quan hệ |
| --- | --- |
| `SystemAccount` | `AccountID` tự tăng; email duy nhất; role `1=Admin`, `2=Staff` |
| `Category` | `CategoryID` tự tăng; `ParentCategoryID` tham chiếu cùng bảng, có thể NULL |
| `NewsArticle` | `NewsArticleID` tự tăng; FK bắt buộc tới category và người tạo; người sửa có thể NULL |
| `Tag` | `TagID` tự tăng; tên và ghi chú |
| `NewsTag` | Khóa ghép `(NewsArticleID, TagID)` bằng `NewsTagId`, `@EmbeddedId` và `@MapsId` |
| `Notification` | `NotificationID` tự tăng; FK bắt buộc tới người nhận; FK bài viết có thể NULL; `IsRead` chỉ nhận `0/1` |

Giữ nguyên tên bảng/cột trong sơ đồ report. Entity dùng `@Column` cho các trường chuỗi; nội dung bài viết chỉ định `nvarchar(max)`. Trạng thái bài/danh mục chỉ nhận `0/1`. ID do SQL Server sinh, không dùng `Date.now()` từ frontend.

Các cột văn bản đa ngôn ngữ được khai báo `nvarchar` rõ ràng. `Start-Backend.ps1` chạy migration `database/migrations/001-unicode-text-columns.sql` cho database cũ trước khi khởi động; khi chạy IDE/JAR, chạy script này trên database đích trước. Script giữ nguyên bản ghi và bỏ qua cột đã là `nvarchar`.

Quan hệ tag theo sơ đồ nhiều–nhiều: một tag được dùng bởi nhiều bài. Phần chữ trong report nói tag thuộc tối đa một bài và mâu thuẫn với sơ đồ; thiết kế hiện tại ưu tiên sơ đồ.

Xóa bài qua Hibernate sẽ xóa các `NewsTag` của bài, giữ tag/category/account. Xóa category/account/tag đang được tham chiếu bị FK chặn. SQL DELETE trực tiếp không có cascade tự động. `ParentCategoryID` không được bằng chính `CategoryID`; chu trình qua nhiều danh mục phải được kiểm tra ở service khi bổ sung CRUD.

`CreatedDate` được điền khi tạo và không cập nhật; `ModifiedDate` được điền khi sửa bài. Thời gian do entity tự sinh được lưu theo UTC. `CreatedBy` / `UpdatedBy` phải được service gán từ người đăng nhập. Entity không tự thực hiện phân quyền hoặc hash mật khẩu; khi bổ sung authentication, lưu password hash vào `AccountPassword` và dùng DTO để không trả mật khẩu/đồ thị entity trực tiếp ra API.

`Notification` gồm `NotificationID`, `RecipientID`, `NewsArticleID`, `Title`, `Content`, `IsRead` và `CreatedDate`. Mặc định chưa đọc (`0`); đọc rồi là `1`. Tiêu đề/nội dung dùng `nvarchar` qua `@Column`, không dùng `@Nationalized`. Có index theo người nhận, trạng thái đọc, thời gian tạo và bài viết. Không cascade xóa account hoặc bài viết; FK bảo vệ các tham chiếu, service phải xử lý thông báo liên quan trước khi xóa bài. Thông báo hệ thống có thể để `NewsArticleID` NULL.

## Kiểm thử

Sau khi tạo database, chạy với JDK 21+:

```powershell
.\mvnw.cmd test
```

Toàn bộ `src/test/` được Git ignore và chỉ giữ trên máy local. Test local dùng SQL Server thật và rollback dữ liệu; test error dispatch dùng HTTP thật, test sinh DDL Unicode không cần kết nối database. Các lỗi SQL trong test kiểm tra ràng buộc là lỗi được kỳ vọng. Xem lệnh `clean verify` trong tài liệu Security để kiểm thử với seed/migration tắt và schema ở chế độ validate.

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

`SystemSetting` là bảng cấu hình key/value (`SettingID`, `SettingKey` duy nhất, `SettingValue`, `Description`). Lỗi nghiệp vụ trả qua `ResponseStatusException` dạng JSON `{status, error, message, path}`. Swagger UI: `http://localhost:8080/swagger-ui.html`; Postman collection: `docs/NMS_Category_Setting.postman_collection.json`. Category và Setting cần JWT: đăng nhập `POST /api/v1/auth/login`, gửi header `Authorization: Bearer <token>`. Admin được đọc/ghi; Staff chỉ được đọc.

Tài liệu: [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Hibernate mapping](https://docs.hibernate.org/orm/7.4/userguide/html_single/), [Microsoft JDBC driver](https://github.com/microsoft/mssql-jdbc).
