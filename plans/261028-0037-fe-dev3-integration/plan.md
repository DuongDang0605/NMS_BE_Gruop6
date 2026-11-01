# Kế Hoạch Kết Nối Frontend & Backend (Dev 3: Engagement, Taxonomy, Dashboard, Portal, Notifications)

**Thời gian tạo:** 2026-10-28 00:37 (Asia/Saigon)  
**Nhánh:** `feat/notification-dashboard-hqm`  
**Trạng thái:** DRAFT - CHỜ PHÊ DUYỆT (Chưa triển khai code)  
**Phạm vi:** Kết nối Frontend `NMS_FE` với toàn bộ API Backend của Dev 3 đã hoàn thiện trên nhánh `develop`.

---

## 1. Mục Tiêu & Tiêu Chí Nghiệm Thu (Acceptance Criteria)

1. **Dashboard:** Hiển thị đủ 5 Card thống kê (News, Categories, Users, Views, Likes) và Bảng Top 5 Trending bài viết có lượt xem/like cao nhất.
2. **Category Tree:** `CategoryManager.jsx` loại bỏ 100% Mock Data, kết nối API thật, hỗ trợ chọn danh mục cha và hiển thị quan hệ phân cấp.
3. **Public Portal:** Độc giả (kể cả chưa đăng nhập) có thể duyệt danh sách bài báo đã xuất bản (Status = 1), tìm kiếm theo từ khóa/danh mục/tag, và xem chi tiết bài báo.
4. **Engagement (Real-time View & Like):**
   - Mở bài báo tự động tăng 1 lượt xem trên DB và UI.
   - Nút Like đổi màu trạng thái (đã like / chưa like) và cập nhật số like tức thì; yêu cầu đăng nhập trước khi like.
5. **Notification Bell:** Header có chuông thông báo, badge đếm số tin chưa đọc, click mở xem danh sách và đánh dấu đã đọc.

---

## 2. Phân Chia Giai Đoạn (Phased Roadmap)

| Phase | Tên Giai Đoạn | Độ Khó | File Bị Ảnh Hưởng Chính |
|---|---|---|---|
| **Phase 1** | Tầng API Client & Dashboard Mở Rộng | Thấp | `NMS_FE/src/api/dashboard.js`, `Dashboard.jsx` |
| **Phase 2** | Nối API Cây Danh Mục (Category Tree) | Trung bình | `NMS_FE/src/api/category.js`, `CategoryManager.jsx` |
| **Phase 3** | Xây dựng Cổng Đọc Báo Công Khai (Portal) | Trung bình | `PortalHome.jsx`, `ArticleDetail.jsx`, `App.jsx` |
| **Phase 4** | Tích hợp Tương tác (Like & Tăng View) | Thấp - TB | `NMS_FE/src/api/engagement.js`, `ArticleDetail.jsx` |
| **Phase 5** | Chuông Thông Báo (Notification Bell) | Trung bình | `NMS_FE/src/api/notification.js`, `Header.jsx` |

---

## 3. Chi Tiết Từng Giai Đoạn

### Giai đoạn 1: Nâng cấp Dashboard (Views, Likes & Top 5 Trending)
- **Mục đích:** Nối nhanh 2 API Dashboard mới vào màn hình hiện tại `Dashboard.jsx`.
- **API sử dụng:**
  - `GET /api/v1/dashboard/overview` (đã có)
  - `GET /api/v1/dashboard/engagement` -> trả về `{ totalViews, totalLikes, top5Trending }`
- **Các bước:**
  1. Tạo `NMS_FE/src/api/dashboard.js` chứa các hàm gọi `getOverview()`, `getEngagementStats()`.
  2. Bổ sung vào `Dashboard.jsx`:
     - 2 Card thống kê: **Total Views** (màu tím/xanh), **Total Likes** (màu đỏ/hồng).
     - 1 Khung danh sách/bảng: **Top 5 Bài Báo Nổi Bật (Trending)** với số lượt xem và lượt thích.

### Giai đoạn 2: Nối API Cây Danh Mục (Category Management)
- **Mục đích:** Bỏ dữ liệu tĩnh `mockCategories.js`, kết nối API thật và hỗ trợ phân cấp cha - con.
- **API sử dụng:**
  - `GET /api/v1/categories/tree` (lấy cây danh mục)
  - `GET /api/v1/categories` (danh sách phẳng để chọn danh mục cha)
  - `POST /api/v1/categories`, `PUT /api/v1/categories/{id}`, `DELETE /api/v1/categories/{id}`
- **Các bước:**
  1. Tạo `NMS_FE/src/api/category.js` đóng gói các API CRUD category.
  2. Cập nhật `CategoryManager.jsx`:
     - Gọi API khi load trang, hiển thị loading và phân trang/tìm kiếm.
     - Trong modal Thêm / Sửa: Thêm `<Form.Select>` chọn `Parent Category` (danh mục cha) hoặc để trống nếu là danh mục gốc.
     - Hiển thị tên danh mục cha trên bảng quản trị.

### Giai đoạn 3: Cổng Đọc Báo Công Khai (Public Portal)
- **Mục đích:** Cho phép độc giả xem tin tức mà không cần đăng nhập.
- **API sử dụng:**
  - `GET /api/v1/portal/articles?keyword=&categoryId=&tagId=&page=0&size=10`
  - `GET /api/v1/portal/articles/{id}`
- **Các bước:**
  1. Tạo `NMS_FE/src/api/portal.js` (`getPublishedArticles`, `getArticleDetail`).
  2. Tạo component `NMS_FE/src/pages/portal/PortalHome.jsx`:
     - Thanh tìm kiếm theo keyword + dropdown lọc theo Category.
     - Grid hiển thị danh sách bài báo dạng card (Tiêu đề, tóm tắt headline, tác giả, ngày đăng, lượt xem, lượt like).
     - Phân trang chuẩn React-Bootstrap.
  3. Tạo component `NMS_FE/src/pages/portal/ArticleDetail.jsx`:
     - Hiển thị chi tiết bài viết: Tiêu đề lớn, Headline, Ngày đăng, Tác giả, Nội dung chính, Danh sách Tags.
  4. Cập nhật `App.jsx`:
     - Thêm Route public: `/portal` (hoặc đặt làm trang chủ `/`) và `/portal/articles/:id`.
     - Thêm nút chuyển đổi giữa "Trang Đọc Báo (Portal)" và "Trang Quản Trị (Admin)" trên Header.

### Giai đoạn 4: Tương Tác Like & Tăng View (Engagement)
- **Mục đích:** Hoàn thiện trải nghiệm tương tác của độc giả trên bài báo.
- **API sử dụng:**
  - `POST /api/v1/news/{id}/views` (Public)
  - `POST /api/v1/news/{id}/reactions` (Cần Bearer Token)
- **Các bước:**
  1. Tạo `NMS_FE/src/api/engagement.js` (`recordView`, `toggleLike`).
  2. Trong `ArticleDetail.jsx`:
     - Thêm `useEffect`: Khi mở trang bài viết, tự động gọi `recordView(articleId)` để tăng lượt xem.
     - Thêm nút Tim (Heart Button):
       - Hiển thị số Like và icon tim đỏ (nếu `isLikedByCurrentUser = true`) hoặc viền rỗng (nếu `false`).
       - Khi bấm nút Like: nếu chưa đăng nhập thì gợi ý mở trang `/login`; nếu đã đăng nhập thì gọi `toggleLike(articleId)` và cập nhật tức thì trên giao diện mà không cần reload trang.

### Giai đoạn 5: Chuông Thông Báo (Notification Bell)
- **Mục đích:** Người dùng xem được các thông báo liên quan đến tài khoản và hệ thống.
- **API sử dụng:**
  - `GET /api/v1/notifications` (danh sách thông báo)
  - `GET /api/v1/notifications/unread-count` (số lượng tin chưa đọc)
  - `PUT /api/v1/notifications/{id}/read` (đánh dấu đã đọc)
- **Các bước:**
  1. Tạo `NMS_FE/src/api/notification.js`.
  2. Cập nhật `Header.jsx`:
     - Thêm icon quả chuông (`bi-bell`) cạnh tên người dùng.
     - Thêm Badge màu đỏ hiển thị số tin chưa đọc (nếu > 0).
     - Click vào chuông mở Dropdown danh sách thông báo, bấm vào từng tin để đánh dấu đã đọc.

---

## 4. Rủi Ro & Giải Pháp Kỹ Thuật (Risk Assessment)

1. **Rủi ro Proxy CORS khi gọi API công khai (Public API):**
   - *Phân tích:* `api.js` hiện tại luôn tự đính kèm token nếu session có sẵn, và sẽ chuyển hướng sang `/login` nếu gặp lỗi 401. Nhưng API Portal đọc báo không bắt buộc login.
   - *Giải pháp:* Tách hoặc cấu hình trong `api.js` để các route `/portal/**` và `/news/{id}/views` không bao giờ bị redirect sang login khi không có token.
2. **Rủi ro Spam View:**
   - *Phân tích:* React `useEffect` trong chế độ StrictMode (dev) chạy 2 lần làm tăng 2 view.
   - *Giải pháp:* Dùng cờ `useRef(false)` hoặc `sessionStorage` để mỗi bài chỉ gọi tăng view 1 lần trong 1 phiên đọc.
3. **Giữ nguyên vẹn code của đồng đội:**
   - Không đụng vào `UserManager.jsx`, `UserModel.jsx`, `user.js` của đồng đội vừa hoàn thành.
