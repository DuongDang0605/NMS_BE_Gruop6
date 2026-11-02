# KẾ HOẠCH CHI TIẾT: CHỈ SỐ TƯƠNG TÁC, DANH MỤC/TAG, CÀI ĐẶT HỆ THỐNG & DASHBOARD TOÀN DIỆN
**Người đảm nhiệm**: Dev 3 - Trưởng phụ trách Tương tác Độc giả, Phân loại Tin tức & Bảng điều khiển Quản trị  
**Phạm vi phụ trách**: 
1. **Phần 1**: Đo lường Tương tác Độc giả & Trải nghiệm Đọc Báo Công khai (Article Reactions/Likes, Real-time View Counter, Public Portal Article View).
2. **Phần 2**: Quản trị Danh mục/Thẻ (Taxonomy), Cài đặt Hệ thống (System Settings) & Bảng điều khiển Phân tích Đa chiều (Unified Analytics Dashboard).  
**Tài liệu tham chiếu**: Report 2 (Mục 4. FR-EN-004 Basic Engagement Metrics, NFR-EN-001..005, AC-EN-004) & Report 1 (Category, Tag, SystemSetting, Dashboard).

---

## 1. THIẾT KẾ CƠ SỞ DỮ LIỆU & ENTITY CỦA DEV 3

Dev 3 sở hữu thực thể mới `ArticleReaction.java` và nâng cấp các thực thể `Category.java`, `Tag.java`, `SystemSetting.java`:

### 1.1. Entity `ArticleReaction.java` (Lượt thích bài viết)
```java
package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ArticleReaction", uniqueConstraints =
        @UniqueConstraint(name = "UQ_Article_Account_Reaction", columnNames = {"NewsArticleID", "AccountID", "ReactionType"}))
@Getter
@Setter
@NoArgsConstructor
public class ArticleReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReactionID")
    private Integer reactionId;

    @Column(name = "NewsArticleID", nullable = false)
    private Integer newsArticleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    private SystemAccount account;

    @Column(name = "ReactionType", nullable = false, length = 20)
    private String reactionType = "LIKE";

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}
```

---

## 2. LOGIC CHI TIẾT TỪNG PHẦN & ĐẶC TẢ CÁC HÀM (BUSINESS LOGIC)

### PHẦN 1: ĐO LƯỜNG TƯƠNG TÁC & TRẢI NGHIỆM ĐỌC TIN CÔNG KHAI (AUDIENCE ENGAGEMENT & PORTAL)

#### 1. Lớp Controller: `EngagementController.java`
- **Đường dẫn**: `/api/v1/news/{id}/reactions` & `/api/v1/news/{id}/views`
- **Các hàm chính**:
  1. `toggleReaction(Integer articleId)`: Độc giả bấm Like hoặc Hủy Like bài viết.
  2. `incrementViewCount(Integer articleId, HttpServletRequest request)`: Tăng lượt xem bài viết theo thời gian thực (Real-time).

#### 2. Chi tiết Thuật toán & Logic Hàm trong `EngagementServiceImpl.java`

##### A. Hàm `toggleReaction(Integer articleId)`
- **Yêu cầu**: Thao tác an toàn giao dịch (Atomic Transaction). Nếu user chưa like $\rightarrow$ Tạo record `ArticleReaction` và tăng `NewsArticle.likeCount` lên 1; Nếu user đã like rồi $\rightarrow$ Xóa record và giảm `NewsArticle.likeCount` đi 1.
- **Mã nguồn giải thuật chi tiết**:
  ```java
  @Transactional
  public ReactionResponseDto toggleReaction(Integer articleId) {
      Integer currentUserId = SecurityUtils.getCurrentAccountId();

      NewsArticle article = newsArticleRepository.findById(articleId)
              .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));

      Optional<ArticleReaction> existingReaction = reactionRepository
              .findByNewsArticleIdAndAccount_AccountId(articleId, currentUserId);

      boolean isLikedNow;
      if (existingReaction.isPresent()) {
          // Người dùng đã like trước đó -> Hủy like (Unlike)
          reactionRepository.delete(existingReaction.get());
          article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
          isLikedNow = false;
      } else {
          // Người dùng chưa like -> Thêm mới like
          ArticleReaction reaction = new ArticleReaction();
          reaction.setNewsArticleId(articleId);
          reaction.setAccount(systemAccountRepository.getReferenceById(currentUserId));
          reaction.setReactionType("LIKE");
          reactionRepository.save(reaction);

          article.setLikeCount(article.getLikeCount() + 1);
          isLikedNow = true;
      }

      newsArticleRepository.save(article);

      return new ReactionResponseDto(
              articleId,
              article.getLikeCount(),
              isLikedNow
      );
  }
  ```

##### B. Hàm `incrementViewCount(Integer articleId)` (Đáp ứng FR-EN-004 & AC-EN-004)
- **Cơ chế**:
  - Khi bạn đọc mở trang chi tiết bài viết, Frontend gọi API `POST /api/v1/news/{id}/views`.
  - Thực thi câu lệnh UPDATE nguyên tử trực tiếp trong database để đảm bảo hiệu năng cao:
    ```sql
    UPDATE NewsArticle SET ViewCount = ViewCount + 1 WHERE NewsArticleID = :id
    ```
  - Trả về số lượt xem mới nhất phục vụ hiển thị `AC-EN-004`.

#### 3. Lớp Controller: `PublicPortalController.java` (Trang Báo Công Khai Cho Độc Giả)
- **Đường dẫn**: `/api/v1/portal`
- **Bảo mật**: Cho phép Public truy cập (`permitAll()`).
- **Các hàm chính**:
  1. `getPublishedArticles(String keyword, Integer categoryId, Integer tagId, Pageable pageable)`:
     - **Điều kiện lọc cứng**: Chỉ lấy các bài viết có `newsStatus = 'PUBLISHED'` và `publishedDate <= now()`.
     - Sắp xếp mặc định: Bài viết mới xuất bản nhất lên đầu (`publishedDate DESC`).
  2. `getArticleDetail(Integer id)`:
     - Lấy thông tin bài viết đầy đủ: Tiêu đề, Tóm tắt, Nội dung, Tác giả, Ngày xuất bản, Tên danh mục, Danh sách Tags.
     - Kèm các chỉ số tương tác thời gian thực: `viewCount`, `likeCount`, `commentCount`.
     - Kèm cờ `isLikedByCurrentUser`: Nếu độc giả đã đăng nhập, kiểm tra xem độc giả đó đã thả tim bài này chưa để hiển thị trái tim đỏ.

---

### PHẦN 2: QUẢN TRỊ DANH MỤC/TAG, CẤU HÌNH HỆ THỐNG & DASHBOARD ĐA CHIỀU

#### 1. Nâng cấp Danh mục (Category) & Thẻ (Tag)
- **Controller**: `CategoryController.java` và `TagController.java`.
- **Nghiệp vụ nâng cấp**:
  - Quản lý danh mục đa cấp: Hỗ trợ tạo danh mục cha và các danh mục con (Sub-category).
  - API Cây danh mục: `GET /api/v1/categories/tree` trả về cấu trúc phân cấp dùng để render Menu Navbar trên Frontend.
  - Kiểm tra an toàn trước khi xóa: Không cho phép xóa danh mục nếu đang có bài viết thuộc danh mục đó hoặc đang có danh mục con trực thuộc.

#### 2. Quản trị Cấu hình Hệ thống (`SystemSettingController.java`)
- **Đường dẫn**: `/api/v1/settings`
- **Bảo mật**: `@PreAuthorize("hasRole('ADMIN')")`
- **Nghiệp vụ quản lý các cờ hệ thống theo Report 2**:
  1. `COMMENT_GLOBAL_ENABLED`: Cấu hình toàn trang ("true" / "false") cho phép hoặc tạm dừng tính năng bình luận.
  2. `COMMENT_MODERATION_MODE`: Chế độ kiểm duyệt toàn trang:
     - `"PRE_MODERATION"`: Mọi bình luận mới đều phải qua kiểm duyệt viên duyệt mới được hiển thị.
     - `"POST_MODERATION"`: Bình luận từ tài khoản hợp lệ được hiển thị ngay.
  3. `SITE_NAME`, `ITEMS_PER_PAGE`: Các thiết lập hiển thị chung.
- Cung cấp API `GET /api/v1/settings/public-configs` để Frontend lấy nhanh trạng thái bật/tắt comment mà không cần quyền Admin.

#### 3. Bảng điều khiển Phân tích Đa chiều (`DashboardController.java` & `DashboardServiceImpl.java`)
- **Đường dẫn**: `/api/v1/dashboard/stats`
- **Bảo mật**: Dành cho nhân sự nội bộ (`@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'MODERATOR', 'CONTRIBUTOR')")`).
- **Nội dung tổng hợp đa nguồn (Unified Analytics)**:
  1. **Thống kê Vận hành Tòa soạn (Editorial Metrics)**:
     - Số lượng bài nháp (`DRAFT`).
     - Số lượng bài chờ duyệt (`PENDING_REVIEW`) $\rightarrow$ Cảnh báo Editor cần vào việc.
     - Số lượng bài đã lên lịch chờ xuất bản (`SCHEDULED`).
     - Số lượng bài đã xuất bản (`PUBLISHED`).
     - Số lượng bài đã gỡ/lưu trữ (`ARCHIVED`).
  2. **Thống kê Tương tác Bạn đọc (Engagement Metrics - FR-EN-004)**:
     - Tổng lượt xem toàn trang (Tổng `ViewCount`).
     - Tổng lượt thích (Tổng `LikeCount`).
     - Tổng số bình luận và số bình luận đang chờ duyệt (`PENDING_MODERATION`).
     - Top 5 bài viết thịnh hành nhất (**Trending Campus Topics**) dựa trên lượt xem cao nhất trong 7 ngày qua.
     - Top 5 bài viết được yêu thích nhất dựa trên lượt Like.

##### Chi tiết Phương thức trong `DashboardServiceImpl.java`:
```java
@Transactional(readOnly = true)
public DashboardResponseDto getComprehensiveStats() {
    // 1. Thống kê bài viết theo trạng thái
    long draftCount = newsArticleRepository.countByNewsStatus(NewsArticle.STATUS_DRAFT);
    long pendingReviewCount = newsArticleRepository.countByNewsStatus(NewsArticle.STATUS_PENDING_REVIEW);
    long scheduledCount = newsArticleRepository.countByNewsStatus(NewsArticle.STATUS_SCHEDULED);
    long publishedCount = newsArticleRepository.countByNewsStatus(NewsArticle.STATUS_PUBLISHED);
    long archivedCount = newsArticleRepository.countByNewsStatus(NewsArticle.STATUS_ARCHIVED);

    // 2. Thống kê tương tác
    long totalViews = newsArticleRepository.sumTotalViews().orElse(0L);
    long totalLikes = reactionRepository.count();
    long totalComments = commentRepository.countByStatus(Comment.STATUS_APPROVED);
    long pendingComments = commentRepository.countByStatus(Comment.STATUS_PENDING_MODERATION);

    // 3. Top 5 bài viết xu hướng
    List<TrendingArticleDto> trendingArticles = newsArticleRepository
            .findTop5ByNewsStatusOrderByViewCountDesc(NewsArticle.STATUS_PUBLISHED)
            .stream()
            .map(a -> new TrendingArticleDto(a.getNewsArticleId(), a.getNewsTitle(), a.getViewCount(), a.getLikeCount()))
            .toList();

    return new DashboardResponseDto(
            new EditorialStatsDto(draftCount, pendingReviewCount, scheduledCount, publishedCount, archivedCount),
            new EngagementStatsDto(totalViews, totalLikes, totalComments, pendingComments),
            trendingArticles
    );
}
```

---

## 3. GIAO DIỆN FRONTEND (REACT VITE) CHO DEV 3

Dev 3 làm việc độc lập trong `src/pages/portal/`, `src/pages/settings/`, `src/pages/dashboard/` và `src/api/engagement.js`:

1. **Trang Báo Công Khai Cho Độc Giả (`src/pages/portal/PortalHome.jsx` & `ArticleDetail.jsx`)**:
   - Giao diện đọc tin hiện đại chuẩn FPT University.
   - Thanh menu điều hướng danh mục dạng cây (Dropdown Category Tree) và thanh tìm kiếm từ khóa.
   - Chi tiết bài viết:
     - Tiêu đề lớn, thông tin tác giả, ngày đăng.
     - **Thanh đo lường tương tác trực tiếp (Engagement Bar)**:
       + Icon con mắt kèm số lượt xem thời gian thực (Cập nhật sau mỗi lần đọc).
       + Nút Trái tim (Thả Like): Khi bấm có animation trái tim nảy, đổi màu đỏ, tăng/giảm số Like tức thì.
       + Icon bình luận kèm tổng số lượng phản hồi.
     - **Tích hợp Khung bình luận**: Nhúng component `CommentSection` (do Dev 2 phụ trách) ngay dưới bài viết.
2. **Bảng điều khiển Thống kê Đa chiều (`src/pages/dashboard/Dashboard.jsx`)**:
   - Hàng Cards chỉ số: Bài chờ duyệt, Bài đã xuất bản, Tổng lượt xem, Bình luận chờ duyệt.
   - Bảng **Top 5 Bài viết Xu hướng (Trending Topics)**: Xếp hạng bài xem nhiều nhất.
   - Biểu đồ phân bố trạng thái tin bài tòa soạn.
3. **Trang Quản lý Cài đặt Hệ thống (`src/pages/setting/Settings.jsx`)**:
   - Switch Bật/Tắt bình luận toàn trang.
   - Dropdown chọn Chế độ kiểm duyệt: `PRE_MODERATION` (Kiểm duyệt trước) hoặc `POST_MODERATION` (Kiểm duyệt sau).
   - Nút Lưu cấu hình cập nhật nhanh vào bảng `SystemSetting`.
