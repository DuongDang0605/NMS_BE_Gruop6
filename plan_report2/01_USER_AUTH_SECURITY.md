# KẾ HOẠCH CHI TIẾT: USER, AUTHENTICATION, AUTHORIZATION & SECURITY
**Người đảm nhiệm**: Chuyên viên Quản lý Người dùng, Xác thực & Bảo mật (Member 1)  
**Phạm vi phụ trách**: 
1. **Phần 1**: Hạ tầng Xác thực & Phân quyền Đa vai trò (5 Roles, JWT Engine, Spring Method Security).
2. **Phần 2**: Quản trị Người dùng & Cơ chế Định danh Xác minh (User CRUD, Role Assignment, User Verification Flag cho Pre-moderation, API Profile Cá nhân).
**Tài liệu tham chiếu**: Report 2 (Mục 3.2 Key User Roles, Mục 4.2 Key User Roles, NFR-ED-003, NFR-EN-003, AC-EN-001).

---

## 1. MÔ HÌNH VAI TRÒ & PHÂN QUYỀN (ROLE & PERMISSION MATRIX)

Hệ thống mở rộng từ 2 role (Admin, Staff) lên **5 Roles chuẩn hóa**:

| Role ID | Enum Name | Spring Authority | Ý nghĩa nghiệp vụ |
| :---: | :--- | :--- | :--- |
| **1** | `ADMIN` | `ROLE_ADMIN` | Quản trị viên tối cao: Toàn quyền quản trị tài khoản, danh mục, gỡ bài, cấu hình hệ thống. |
| **2** | `CONTRIBUTOR` | `ROLE_CONTRIBUTOR` | Giảng viên/Nhân viên viết bài: Soạn thảo nháp, sửa bài, nộp duyệt bài viết (không thể tự xuất bản). |
| **3** | `EDITOR` | `ROLE_EDITOR` | Trưởng ban biên tập/Biên tập viên: Xem hàng đợi duyệt, yêu cầu sửa đổi kèm feedback, duyệt xuất bản, lên lịch. |
| **4** | `MODERATOR` | `ROLE_MODERATOR` | Nhân viên kiểm duyệt bình luận: Duyệt bình luận chờ duyệt, xóa bình luận vi phạm, xử lý báo cáo vi phạm. |
| **5** | `READER` | `ROLE_READER` | Độc giả/Sinh viên/Cán bộ: Đọc tin, thả Like, viết bình luận, phản hồi bình luận, gửi báo cáo vi phạm. |

---

## 2. THIẾT KẾ CƠ SỞ DỮ LIỆU & ENTITY CẬP NHẬT

### 2.1. Cập nhật Entity `SystemAccount.java`
- Thêm trường `isVerified` (kiểu `Boolean`, mặc định `false` cho tài khoản Reader tự đăng ký, mặc định `true` cho Admin/Staff/Editor/Moderator tạo từ hệ thống). Trường này là điều kiện quyết định để Module Bình luận (Dev 2) phân loại Pre-moderation vs Post-moderation (theo tiêu chí `AC-EN-001`).
- Bổ sung hằng số vai trò và cập nhật Check Constraint.

```java
package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "SystemAccount", uniqueConstraints =
        @UniqueConstraint(name = "UQ_SystemAccount_Email", columnNames = "AccountEmail"))
@Check(name = "CK_SystemAccount_Role", constraints = "AccountRole IN (1, 2, 3, 4, 5)")
@Getter
@Setter
@NoArgsConstructor
public class SystemAccount {

    public static final int ADMIN_ROLE = 1;
    public static final int CONTRIBUTOR_ROLE = 2; // (Thay thế Staff cũ)
    public static final int EDITOR_ROLE = 3;
    public static final int MODERATOR_ROLE = 4;
    public static final int READER_ROLE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private Integer accountId;

    @Column(name = "AccountName", nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String accountName;

    @Column(name = "AccountEmail", nullable = false, length = 254)
    private String accountEmail;

    @Column(name = "AccountRole", nullable = false)
    private Integer accountRole = READER_ROLE;

    @Column(name = "AccountPassword", nullable = false, length = 255)
    private String accountPassword;

    // Phục vụ quy tắc kiểm duyệt: Tài khoản chưa xác thực (isVerified = false)
    // khi comment sẽ rơi vào PENDING_MODERATION
    @Column(name = "IsVerified", nullable = false)
    private Boolean isVerified = false;
}
```

---

## 3. LOGIC CÁC LỚP BẢO MẬT & XÁC THỰC (PART 1: AUTH & SECURITY INFRASTRUCTURE)

### 3.1. Lớp `CustomUserDetails.java`
- **Mục đích**: Cung cấp thông tin tài khoản đầy đủ cho Spring Security Context để các Controller khác có thể lấy ra trực tiếp ID, Email, Role, trạng thái Verified.
- **Chi tiết hàm `getRoleName()`**:
  ```java
  public String getRoleName() {
      return switch (account.getAccountRole()) {
          case SystemAccount.ADMIN_ROLE -> "ADMIN";
          case SystemAccount.CONTRIBUTOR_ROLE -> "CONTRIBUTOR";
          case SystemAccount.EDITOR_ROLE -> "EDITOR";
          case SystemAccount.MODERATOR_ROLE -> "MODERATOR";
          case SystemAccount.READER_ROLE -> "READER";
          default -> throw new IllegalStateException("Role không hợp lệ: " + account.getAccountRole());
      };
  }
  ```
- **Hàm `getAuthorities()`**:
  ```java
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of(new SimpleGrantedAuthority("ROLE_" + getRoleName()));
  }
  ```
- **Bổ sung getter**:
  - `public Integer getAccountId()`: Trả về ID tài khoản.
  - `public Boolean isVerified()`: Trả về trạng thái xác minh.

### 3.2. Lớp `JwtTokenProvider.java`
- **Nâng cấp Token Claims**:
  - Thêm `accountId`, `role` (dạng string: `ADMIN`, `CONTRIBUTOR`, v.v.), `isVerified` vào JWT Payload.
  - Giúp Frontend giải mã token lấy nhanh thông tin hiển thị mà không cần gọi API liên tục.
- **Logic hàm `generateAccessToken(CustomUserDetails userDetails)`**:
  1. Trích xuất thông tin từ `userDetails`.
  2. Đưa vào `claims`:
     - `"sub"`: `userDetails.getUsername()` (Email)
     - `"accountId"`: `userDetails.getAccountId()`
     - `"role"`: `userDetails.getRoleName()`
     - `"isVerified"`: `userDetails.isVerified()`
  3. Ký token bằng thuật toán HMAC SHA-256 với `jwt.secret`.

### 3.3. Lớp `SecurityUtils.java` (Cung cấp tiện ích dùng chung cho toàn đội)
- Cung cấp các static method dùng cho Dev 1, Dev 2, Dev 3 gọi trong Service của họ để xác định ai đang thực hiện hành vi:
  - `public static Integer getCurrentAccountId()`: Lấy ID người dùng đang đăng nhập từ `SecurityContextHolder`. Ném `UnauthorizedException` nếu chưa đăng nhập.
  - `public static CustomUserDetails getCurrentUser()`: Lấy toàn bộ UserDetails.
  - `public static boolean hasRole(String role)`: Kiểm tra xem user hiện tại có quyền đó không (ví dụ: `hasRole("EDITOR")`).
  - `public static boolean isAdmin()`: Kiểm tra nhanh quyền Admin.

### 3.4. Lớp `SecurityConfig.java` (Đóng băng cấu hình - Zero Conflict)
Cấu hình chuẩn giúp các lập trình viên khác không bao giờ bị 403 oan uổng và không cần sửa file này:
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true) // Kích hoạt @PreAuthorize trên từng Controller
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthenticationManager authManager,
            JwtTokenProvider tokens, CustomUserDetailsService users,
            UrlBasedCorsConfigurationSource corsSource) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokens, users);

        return http.cors(cors -> cors.configurationSource(corsSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(authManager)
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép Public hoàn toàn Swagger và Tài liệu OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 2. Cho phép Public các endpoint Auth (Đăng nhập, Đăng ký Reader, Refresh)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 3. Cho phép Public các API đọc tin công khai (Portal đọc bài viết, xem danh mục)
                        .requestMatchers(HttpMethod.GET, "/api/v1/portal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        // 4. Mọi API còn lại yêu cầu phải có Token hợp lệ
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

---

## 4. CHI TIẾT NGHIỆP VỤ & LOGIC CÁC HÀM (PART 2: USER & AUTH LOGIC)

### 4.1. `AuthController` & `AuthService`
Endpoint prefix: `/api/v1/auth`

#### A. Hàm `login(LoginRequestDto request)`
- **Luồng logic**:
  1. Kiểm tra Email có tồn tại trong database không qua `SystemAccountRepository.findByAccountEmail(email)`. Ném `UnauthorizedException("Email hoặc mật khẩu không chính xác")` nếu không có.
  2. Kiểm tra mật khẩu: Hỗ trợ mã hóa BCrypt. Nếu mật khẩu trong DB chưa mã hóa (dữ liệu seed cũ dạng plaintext), so khớp trực tiếp; nếu khớp, tự động mã hóa BCrypt và lưu ngược lại DB (Cơ chế Password Migration an toàn).
  3. Tạo `CustomUserDetails`, gọi `jwtTokenProvider.generateAccessToken(userDetails)` và `generateRefreshToken(userDetails)`.
  4. Trả về `LoginResponseDto` gồm: `accessToken`, `refreshToken`, `accountId`, `accountName`, `accountEmail`, `accountRole`, `roleName`, `isVerified`.

#### B. Hàm `registerReader(RegisterReaderRequestDto request)` (Tính năng mới theo Report 2)
- **Input**: `accountName`, `accountEmail`, `password`.
- **Luồng logic**:
  1. Validate định dạng Email (đuôi hợp lệ, ví dụ `@fpt.edu.vn` hoặc chuẩn RFC).
  2. Kiểm tra trùng email: `if (repository.existsByAccountEmail(email))` $\rightarrow$ Ném `ResourceConflictException("Email đã được sử dụng")`.
  3. Khởi tạo đối tượng `SystemAccount`:
     - `accountRole = SystemAccount.READER_ROLE (5)`
     - `isVerified = false` (Mặc định Reader tự đăng ký sẽ chưa được verify, cần Admin xác nhận hoặc sau này kích hoạt qua email để chống spam).
     - `accountPassword = passwordEncoder.encode(password)`.
  4. Lưu DB và trả về kết quả thành công HTTP 201 Created.

#### C. Hàm `refreshToken(RefreshTokenRequestDto request)`
- Xác thực Refresh token hợp lệ, còn hạn sử dụng, trích xuất email và sinh `accessToken` mới.

---

### 4.2. `UserController` & `SystemAccountService`
Endpoint prefix: `/api/v1/users`  
Bảo vệ Controller: `@PreAuthorize("hasRole('ADMIN')")` (Chỉ Admin mới có quyền quản trị toàn bộ người dùng).

#### A. Hàm `search(UserSearchRequestDto filter, Pageable pageable)`
- **Input**:
  - `keyword`: Tìm kiếm gần đúng theo `AccountName` hoặc `AccountEmail`.
  - `role`: Lọc chính xác theo vai trò (1: ADMIN, 2: CONTRIBUTOR, 3: EDITOR, 4: MODERATOR, 5: READER).
  - `isVerified`: Lọc theo trạng thái đã xác thực hay chưa.
  - Phân trang: `page`, `size`, `sort`.
- **Logic**: Sử dụng JPA Specification (`SystemAccountSpecifications`) để ghép nối động các điều kiện `WHERE` trong SQL Server, trả về `PageResponseDto<SystemAccountResponseDto>`.

#### B. Hàm `create(SystemAccountRequestDto request)`
- **Logic**:
  1. Kiểm tra quyền: Chỉ Admin mới được tạo tài khoản có Role là CONTRIBUTOR, EDITOR, MODERATOR hoặc ADMIN.
  2. Kiểm tra trùng lặp email.
  3. Mã hóa mật khẩu BCrypt.
  4. Đặt `isVerified = true` (Do Admin trực tiếp tạo nên mặc định được tin cậy).
  5. Lưu thực thể và trả về `SystemAccountResponseDto`.

#### C. Hàm `toggleVerify(Integer accountId)` (Tính năng mới trọng tâm)
- **Endpoint**: `PATCH /api/v1/users/{id}/verify`
- **Mục đích**: Admin duyệt xác minh danh tính cho độc giả/sinh viên (Reader). Khi `isVerified = true`, bình luận của độc giả này sẽ được hiển thị ngay (theo cơ chế Post-moderation).
- **Luồng logic**:
  1. Tìm tài khoản theo `accountId`: Ném `ResourceNotFoundException("Tài khoản không tồn tại")` nếu không thấy.
  2. Đảo trạng thái: `account.setIsVerified(!account.getIsVerified())`.
  3. Lưu xuống cơ sở dữ liệu.
  4. Trả về trạng thái mới.

#### D. API Cá nhân: `GET /api/v1/users/me` & `PUT /api/v1/users/me/password`
- Cho phép bất kỳ người dùng nào đã đăng nhập (`@PreAuthorize("isAuthenticated()")`) tự xem thông tin của mình và tự đổi mật khẩu cá nhân.
- Lấy `accountId` từ `SecurityUtils.getCurrentAccountId()`.

---

## 5. ĐẶC TẢ DTO & CONTRACT GIAO TIẾP

### 5.1. DTO Request / Response
```java
// DTO Đăng ký Reader
public record RegisterReaderRequestDto(
    @NotBlank(message = "Tên người dùng không được để trống")
    @Size(max = 100)
    String accountName,

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String accountEmail,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
    String accountPassword
) {}

// DTO Response thông tin tài khoản
public record SystemAccountResponseDto(
    Integer accountId,
    String accountName,
    String accountEmail,
    Integer accountRole,
    String roleName, // "ADMIN", "CONTRIBUTOR", "EDITOR", "MODERATOR", "READER"
    Boolean isVerified
) {}
```

---

## 6. GIAO DIỆN FRONTEND (REACT VITE)

Member 1 toàn quyền phát triển trong thư mục `src/pages/users/` và `src/pages/auth/`, đồng thời tạo file `src/api/auth.js` và `src/api/user.js`:

1. **Trang Đăng ký Độc giả (`src/pages/auth/Register.jsx`)**:
   - Form gồm Họ tên, Email, Mật khẩu, Xác nhận mật khẩu.
   - Gọi API `POST /api/v1/auth/register-reader`. Chuyển hướng sang Đăng nhập khi thành công.
2. **Trang Đăng nhập nâng cấp (`src/pages/auth/Login.jsx`)**:
   - Sau khi đăng nhập thành công, lưu token và user info vào `localStorage`.
   - Điều hướng thông minh dựa trên Role:
     - `ADMIN`, `EDITOR`, `CONTRIBUTOR`, `MODERATOR` $\rightarrow$ Điều hướng vào Admin Layout (`/dashboard`).
     - `READER` $\rightarrow$ Điều hướng về Trang đọc báo công khai (`/portal`).
3. **Trang Quản lý Người dùng (`src/pages/users/UserManager.jsx`)**:
   - Bảng phân trang danh sách người dùng.
   - Thanh lọc Dropdown: Tất cả vai trò, Admin, Contributor, Editor, Moderator, Reader.
   - Badge trạng thái xác thực: Hiển thị nhãn xanh lá **"Verified"** hoặc vàng **"Unverified"**.
   - Nút hành động nhanh:
     - Nút **"Verify / Unverify"**: Gọi API `PATCH /api/v1/users/{id}/verify` để cấp quyền uy tín cho bạn đọc nhanh chóng.
     - Nút Tạo/Sửa qua Popup Dialog, Nút Xóa có xác nhận.
