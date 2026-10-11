# NMS Backend

Spring Boot 4.1.1, Java 21+, Hibernate/JPA và SQL Server.

## Chạy trên Windows

Yêu cầu SQL Server đang chạy trên `localhost:1433`, `sqlcmd` có trong PATH và tài khoản Windows có quyền tạo database. Cấu hình mặc định dùng Windows Authentication, không cần ghi mật khẩu vào source.

Tại thư mục `NMS_BE`, chạy:

```powershell
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
sqlcmd -S 'tcp:localhost,1433' -E -C -b -i .\database\create-database.sql
.\mvnw.cmd spring-boot:run
```

Maven profile `windows-auth` tự kích hoạt trên Windows, tải DLL Microsoft JDBC tương ứng vào `target/native` và thiết lập `java.library.path` cho Maven run/test. IDE cần chạy `generate-resources` trước và thêm VM option `-Djava.library.path=target/native` với working directory `NMS_BE`.

Build và chạy JAR:

```powershell
.\mvnw.cmd clean package
java '-Djava.library.path=target/native' -jar .\target\NMS_BE-0.0.1-SNAPSHOT.jar
```

`java` dùng để chạy JAR cũng phải là Java 21+. Trên máy có Java 17 trong PATH, dùng `& "$env:JAVA_HOME\bin\java.exe"` thay cho `java`.

## SQL Authentication / instance khác

Sau khi tạo database trên instance đích và cấp quyền cho SQL login, có thể cấu hình qua biến môi trường:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'sql-auth'
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=FUNewsManagementSystem;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = '<SQL login>'
$env:DB_PASSWORD = '<password>'
.\mvnw.cmd spring-boot:run
```

Không commit mật khẩu. `trustServerCertificate=true` chỉ dùng cho SQL Server local có chứng chỉ tự ký. Có thể đổi cổng HTTP bằng `SERVER_PORT` và bật SQL log bằng `SHOW_SQL=true`.

## Entity và quy tắc

| Entity | Khóa / quan hệ |
| --- | --- |
| `SystemAccount` | `AccountID` tự tăng; email duy nhất; role `1=Admin`, `2=Staff` |
| `Category` | `CategoryID` tự tăng; `ParentCategoryID` tham chiếu cùng bảng, có thể NULL |
| `NewsArticle` | `NewsArticleID` tự tăng; FK bắt buộc tới category và người tạo; người sửa có thể NULL |
| `Tag` | `TagID` tự tăng; tên và ghi chú |
| `NewsTag` | Khóa ghép `(NewsArticleID, TagID)` bằng `NewsTagId`, `@EmbeddedId` và `@MapsId` |

Giữ nguyên tên bảng/cột trong sơ đồ report. Entity dùng `@Column` cho các trường chuỗi; nội dung bài viết chỉ định `nvarchar(max)`. Trạng thái bài/danh mục chỉ nhận `0/1`. ID do SQL Server sinh, không dùng `Date.now()` từ frontend.

Quan hệ tag theo sơ đồ nhiều–nhiều: một tag được dùng bởi nhiều bài. Phần chữ trong report nói tag thuộc tối đa một bài và mâu thuẫn với sơ đồ; thiết kế hiện tại ưu tiên sơ đồ.

Xóa bài qua Hibernate sẽ xóa các `NewsTag` của bài, giữ tag/category/account. Xóa category/account/tag đang được tham chiếu bị FK chặn. SQL DELETE trực tiếp không có cascade tự động. `ParentCategoryID` không được bằng chính `CategoryID`; chu trình qua nhiều danh mục phải được kiểm tra ở service khi bổ sung CRUD.

`CreatedDate` được điền khi tạo và không cập nhật; `ModifiedDate` được điền khi sửa bài. Thời gian do entity tự sinh được lưu theo UTC. `CreatedBy` / `UpdatedBy` phải được service gán từ người đăng nhập. Entity không tự thực hiện phân quyền hoặc hash mật khẩu; khi bổ sung authentication, lưu password hash vào `AccountPassword` và dùng DTO để không trả mật khẩu/đồ thị entity trực tiếp ra API.

## Kiểm thử

Sau khi tạo database, chạy với JDK 21+:

```powershell
.\mvnw.cmd test
```

Test dùng SQL Server thật, không thay bằng H2. Kiểm tra lưu/đọc cả 5 entity, Unicode và nội dung dài, quan hệ cha–con, tag dùng chung, thời gian tạo/sửa, cascade khi xóa bài, khóa ghép, CHECK role và FK bảo vệ danh mục. Dữ liệu trong từng test được rollback; bảng do Hibernate tạo được giữ lại. Các lỗi SQL trong test kiểm tra ràng buộc là lỗi được kỳ vọng.

Package `com.mvc.nms_be.repository` có 5 interface khung: `SystemAccountRepository`, `CategoryRepository`, `NewsArticleRepository`, `TagRepository` và `NewsTagRepository`. Mỗi interface có `@Repository` và kế thừa `JpaRepository` để Spring tạo bean, hỗ trợ inject vào service qua constructor. Bốn entity có khóa đơn dùng `Integer`; `NewsTagRepository` dùng khóa ghép `NewsTagId`. Chưa khai báo query hoặc method riêng. Backend chưa có API CRUD và frontend vẫn dùng mock data.

## Controller

Package `controller` có 5 controller khung, gắn `@RestController`, `@RequestMapping` và Lombok `@RequiredArgsConstructor` để inject service qua constructor:

| Controller | Đường dẫn gốc | Service |
| --- | --- | --- |
| `UserController` | `/api/users` | `SystemAccountService` |
| `CategoryController` | `/api/categories` | `CategoryService` |
| `NewsArticleController` | `/api/news` | `NewsArticleService` |
| `TagController` | `/api/tags` | `TagService` |
| `NewsTagController` | `/api/news-tags` | `NewsTagService` |

Các controller chưa có handler GET/POST/PUT/DELETE; đường dẫn gốc chuẩn bị cho bước triển khai endpoint sau này. Spring tự quản lý controller/service, không cần thêm cấu hình bean.

## Service

Package `service` có 5 interface khung: `SystemAccountService`, `CategoryService`, `NewsArticleService`, `TagService` và `NewsTagService`. Package `service/impl` có các class tương ứng với hậu tố `Impl`, implements interface và gắn `@Service`. Spring tự phát hiện các bean trong package gốc `com.mvc.nms_be`, không cần thêm cấu hình component scan hoặc khai báo bean thủ công. Các interface/class hiện để trống, sẵn sàng bổ sung method và inject repository/mapper qua constructor khi triển khai nghiệp vụ.

## DTO và mapper

Mỗi entity có DTO riêng trong `dto/request` và `dto/response`, dùng Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`. Các request không chứa ID tự tăng, thời gian tạo/sửa hay người tạo/sửa. `NewsTagRequestDto` chứa hai ID tạo khóa ghép. Response dùng ID cho các quan hệ, không chứa entity hoặc Hibernate proxy; `SystemAccountResponseDto` không chứa mật khẩu.

Package `mapper` gồm 5 mapper viết tay, có `@Component` để inject vào service. `toEntity(...)` tạo entity mới từ request bằng setter; `toResponseDto(...)` tạo response bằng constructor. Không cần MapStruct hoặc thư viện mapping khác.

Ví dụ map danh sách trong service sau này:

```java
public List<CategoryResponseDto> getAll() {
    return categoryRepository.findAll().stream()
            .map(categoryMapper::toResponseDto)
            .toList();
}
```

Service chịu trách nhiệm validate request, lấy các entity liên quan từ ID trong request rồi truyền vào mapper. `NewsArticleMapper.toEntity` nhận category, người tạo từ phiên đăng nhập và danh sách tag đã lấy từ database. `CategoryMapper.toEntity` nhận category cha, hoặc `null` cho danh mục gốc. `NewsTagMapper.toEntity` nhận bài viết và tag ứng với hai ID trong request. `SystemAccountMapper.toEntity` nhận password hash đã được service tạo, không chép mật khẩu thô từ request vào entity.

Request DTO hiện dùng cho dữ liệu tạo/cập nhật đầy đủ, không định nghĩa PATCH. `toEntity` tạo entity mới; khi triển khai cập nhật, service phải lấy entity đang có rồi cập nhật các trường được phép, giữ ID và thông tin audit. Map response bài viết trong service có `@Transactional(readOnly = true)` hoặc sau khi đã tải danh sách tag, vì `newsTags` dùng LAZY và `open-in-view=false`.

Tài liệu: [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Hibernate mapping](https://docs.hibernate.org/orm/7.4/userguide/html_single/), [Microsoft JDBC driver](https://github.com/microsoft/mssql-jdbc).
