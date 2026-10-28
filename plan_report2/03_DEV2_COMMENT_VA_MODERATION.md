# KẾ HOẠCH CHI TIẾT: HỆ THỐNG BÌNH LUẬN ĐA CẤP, LỌC TỪ CẤM & TRUNG TÂM KIỂM DUYỆT (COMMENTS & MODERATION)
**Người đảm nhiệm**: Dev 2 - Trưởng phụ trách Hệ thống Bình luận & Kiểm duyệt An toàn  
**Phạm vi phụ trách**: 
1. **Phần 1**: Động cơ Bình luận Phân cấp Lồng nhau & Đường ống Kiểm duyệt (Threaded Comment Engine, Nested Replies, Pre-moderation vs Post-moderation Pipeline).
2. **Phần 2**: Hàng rào Phòng vệ Tự động & Bàn làm việc Kiểm duyệt viên (Automated Profanity & Spam Filter, Report Abuse Workflow & Moderator Review Dashboard).  
**Tài liệu tham chiếu**: Report 2 (Mục 4. Module 2: Comment & Audience Engagement Management: FR-EN-001, FR-EN-002, FR-EN-003, NFR-EN-001..005, AC-EN-001..003).

---

## 1. THIẾT KẾ CƠ SỞ DỮ LIỆU & CÁC ENTITY CỦA DEV 2

Dev 2 sở hữu và quản lý hoàn toàn 3 thực thể độc lập: `Comment.java`, `CommentReport.java`, và `BannedKeyword.java`:

### 1.1. Entity `Comment.java` (Bình luận & Phân cấp lồng nhau)
```java
package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Comment", indexes = {
        @Index(name = "IX_Comment_Article_Status", columnList = "NewsArticleID, Status"),
        @Index(name = "IX_Comment_Parent", columnList = "ParentCommentID"),
        @Index(name = "IX_Comment_Account", columnList = "AccountID")
})
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    public static final String STATUS_PENDING_MODERATION = "PENDING_MODERATION";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_DELETED = "DELETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    private Integer commentId;

    @Column(name = "NewsArticleID", nullable = false)
    private Integer newsArticleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    private SystemAccount account;

    // Quan hệ phân cấp: ParentCommentID trỏ về chính CommentID cha
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentCommentID")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @OrderBy("createdDate ASC")
    private List<Comment> replies = new ArrayList<>();

    @Column(name = "Content", nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "Status", nullable = false, length = 30)
    private String status = STATUS_APPROVED;

    @Column(name = "IsFlaggedProfanity", nullable = false)
    private Boolean isFlaggedProfanity = false;

    @Column(name = "ReportCount", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
```

### 1.2. Entity `CommentReport.java` (Báo cáo vi phạm từ Độc giả)
```java
package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CommentReport")
@Getter
@Setter
@NoArgsConstructor
public class CommentReport {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_DISMISSED = "DISMISSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CommentID", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReporterID", nullable = false)
    private SystemAccount reporter;

    @Column(name = "Reason", nullable = false, length = 500)
    private String reason; // SPAM, HARASSMENT, OFFENSIVE, MISINFORMATION, OTHER

    @Column(name = "Status", nullable = false, length = 30)
    private String status = STATUS_PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ResolvedByID")
    private SystemAccount resolvedBy;

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "ResolvedDate")
    private LocalDateTime resolvedDate;
}
```

### 1.3. Entity `BannedKeyword.java` (Từ khóa cấm)
```java
package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "BannedKeyword", uniqueConstraints =
        @UniqueConstraint(name = "UQ_BannedKeyword_Word", columnNames = "Word"))
@Getter
@Setter
@NoArgsConstructor
public class BannedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KeywordID")
    private Integer keywordId;

    @Column(name = "Word", nullable = false, length = 100)
    private String word;

    @Column(name = "Severity", nullable = false, length = 20)
    private String severity = "BLOCK"; // BLOCK: Chặn không cho gửi; FLAG: Cho gửi nhưng đưa vào PENDING_MODERATION

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}
```

---

## 2. LOGIC CHI TIẾT CÁC HÀM (BUSINESS LOGIC)

### PHẦN 1: ĐỘNG CƠ BÌNH LUẬN PHÂN CẤP LỒNG NHAU (THREADED COMMENT ENGINE)

#### 1. Lớp Controller: `CommentController.java`
- **Đường dẫn**: `/api/v1/comments`
- **Bảo mật**: Xem công khai (GET), Đăng/Báo cáo cần đăng nhập (`@PreAuthorize("isAuthenticated()")`).
- **Các hàm chính**:
  1. `getArticleComments(Integer articleId)`: Lấy cây bình luận đầy đủ của một bài viết (Chỉ lấy bình luận `APPROVED` hoặc `DELETED` có trả lời con).
  2. `postComment(CreateCommentRequestDto request)`: Đăng bình luận mới hoặc trả lời bình luận cấp trước.
  3. `reportComment(Integer commentId, ReportCommentRequestDto request)`: Độc giả báo cáo bình luận xấu.

#### 2. Chi tiết Thuật toán Đăng Bình luận & Quy trình Pre-moderation vs Post-moderation (`CommentServiceImpl.java`)

##### A. Hàm `postComment(CreateCommentRequestDto request)` (Thực thi AC-EN-001, AC-EN-002)
- **Quy tắc nghiệp vụ**:
  1. Bài viết phải tồn tại và cho phép bình luận (`allowComments == true`).
  2. Quét nội dung bình luận qua bộ lọc từ cấm `ProfanityFilterService`.
     - Nếu chứa từ cấm mức `BLOCK` $\rightarrow$ Ném ngoại lệ từ chối ngay (`AC-EN-002`).
     - Nếu chứa từ cấm mức `FLAG` $\rightarrow$ Đánh dấu `isFlaggedProfanity = true`.
  3. Xác định trạng thái xuất bản ban đầu:
     - **Điều kiện vào `PENDING_MODERATION`**:
       + Bị dính từ cấm mức `FLAG`, HOẶC
       + Người dùng chưa xác minh (`isVerified == false` theo tiêu chí `AC-EN-001`), HOẶC
       + Hệ thống đang kích hoạt chế độ toàn trang `PRE_MODERATION`.
     - **Điều kiện vào `APPROVED` ngay**: Người dùng đã xác minh (`isVerified == true`) VÀ nội dung sạch từ cấm VÀ hệ thống ở chế độ `POST_MODERATION`.
- **Mã nguồn giải thuật chi tiết**:
  ```java
  @Transactional
  public CommentResponseDto postComment(CreateCommentRequestDto request) {
      Integer currentUserId = SecurityUtils.getCurrentAccountId();
      CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

      // 1. Kiểm tra bài viết và quyền mở comment
      NewsArticle article = newsArticleRepository.findById(request.newsArticleId())
              .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + request.newsArticleId()));

      if (Boolean.FALSE.equals(article.getAllowComments())) {
          throw new IllegalArgumentException("Bài viết này đã tắt tính năng bình luận");
      }

      // 2. Chạy qua Bộ lọc từ cấm tự động (Automated Safeguards)
      ProfanityCheckResult checkResult = profanityFilterService.checkContent(request.content());
      if (checkResult.hasBlockedWord()) {
          throw new IllegalArgumentException("Bình luận chứa từ ngữ không phù hợp và bị từ chối: " + checkResult.detectedWords());
      }

      // 3. Khởi tạo đối tượng Comment
      Comment comment = new Comment();
      comment.setNewsArticleId(article.getNewsArticleId());
      comment.setAccount(systemAccountRepository.getReferenceById(currentUserId));
      comment.setContent(request.content());
      comment.setIsFlaggedProfanity(checkResult.hasFlaggedWord());

      // Xử lý trả lời lồng nhau (Nested Reply)
      if (request.parentCommentId() != null) {
          Comment parent = commentRepository.findById(request.parentCommentId())
                  .orElseThrow(() -> new ResourceNotFoundException("Bình luận cha không tồn tại"));
          comment.setParentComment(parent);
      }

      // 4. Xác định trạng thái kiểm duyệt (Pre-moderation vs Post-moderation)
      boolean mustModerate = Boolean.FALSE.equals(currentUser.isVerified()) 
              || checkResult.hasFlaggedWord() 
              || moderationSettingService.isGlobalPreModeration();

      if (mustModerate) {
          comment.setStatus(Comment.STATUS_PENDING_MODERATION);
      } else {
          comment.setStatus(Comment.STATUS_APPROVED);
          // Tăng commentCount bài viết nếu hiển thị ngay
          article.setCommentCount(article.getCommentCount() + 1);
          newsArticleRepository.save(article);
      }

      Comment saved = commentRepository.save(comment);
      return commentMapper.toDto(saved);
  }
  ```

##### B. Hàm `getArticleComments(Integer articleId)` (Thuật toán dựng Cây bình luận đa cấp)
- **Yêu cầu**: Trả về cấu trúc cây phân cấp (Tree hierarchy: Bình luận gốc chứa danh sách bình luận con `replies`).
- **Xử lý bình luận bị xóa (Soft Delete)**: Nếu một comment cha bị xóa (`STATUS_DELETED`) nhưng vẫn còn comment con hợp lệ, nội dung hiển thị sẽ được thay thế bằng chuỗi `"[Bình luận này đã bị xóa bởi kiểm duyệt viên]"` để không làm đứt mạch thảo luận của các câu trả lời con bên dưới.
- **Thuật toán**:
  1. Lấy tất cả bình luận của bài viết có trạng thái `IN ('APPROVED', 'DELETED')` sắp xếp theo `createdDate ASC`.
  2. Dùng Map `Map<Integer, CommentNodeDto>` để nhóm theo ID.
  3. Lặp qua danh sách: Các comment có `parentCommentId == null` đưa vào danh sách `rootComments`; các comment có `parentCommentId` được add vào mảng `replies` của node cha tương ứng.
  4. Trả về mảng `rootComments`.

---

### PHẦN 2: HỆ THỐNG PHÒNG VỆ TỰ ĐỘNG & BÀN KIỂM DUYỆT (AUTOMATED SAFEGUARDS & MODERATION)

#### 1. Dịch vụ Lọc từ cấm: `ProfanityFilterService.java` (Đáp ứng FR-EN-003, AC-EN-002)
- **Cơ chế hoạt động**:
  - Tải danh sách `BannedKeyword` đang hoạt động (`isActive = true`) vào bộ nhớ cache.
  - Sử dụng thuật toán so khớp chuỗi không phân biệt hoa thường và loại bỏ dấu tiếng Việt (normalization).
  - Trả về đối tượng kết quả `ProfanityCheckResult`:
    - `hasBlockedWord`: Có chứa từ cấm mức BLOCK không.
    - `hasFlaggedWord`: Có chứa từ nhạy cảm cần xem xét không.
    - `detectedWords`: Danh sách từ khóa bị phát hiện.
- **Cung cấp API CRUD Từ khóa cấm**: Dành cho Moderator/Admin thêm mới từ cấm vào từ điển hệ thống.

#### 2. Dịch vụ Báo cáo vi phạm: `reportComment(...)` (Đáp ứng AC-EN-003)
- **Quy tắc**:
  1. Độc giả đăng nhập chọn một bình luận và gửi lý do báo cáo (`SPAM`, `HARASSMENT`, `OFFENSIVE`,...).
  2. Kiểm tra nếu cùng một user đã báo cáo bình luận này rồi $\rightarrow$ Không cho báo cáo lặp.
  3. Lưu bản ghi mới vào `CommentReport` với trạng thái `PENDING`.
  4. Tăng trường `reportCount` của `Comment` lên 1 đơn vị.
  5. Đưa bình luận này ưu tiên hiển thị lên đầu hàng đợi kiểm duyệt của Moderator.

#### 3. Bàn làm việc của Kiểm duyệt viên: `ModerationController.java`
- **Đường dẫn**: `/api/v1/moderation`
- **Bảo mật**: `@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")`
- **Các hàm chính**:
  1. `getPendingComments(Pageable pageable)`: Lấy danh sách bình luận đang ở trạng thái `PENDING_MODERATION` hoặc có `reportCount > 0`.
  2. `getReportList(Pageable pageable)`: Danh sách các phiếu báo cáo vi phạm cần xử lý.
  3. `approveComment(Integer commentId)`: Phê duyệt hiển thị bình luận (`status = APPROVED`).
  4. `rejectComment(Integer commentId)`: Từ chối bình luận (`status = REJECTED`).
  5. `deleteComment(Integer commentId)`: Xóa bình luận vi phạm (`status = DELETED`).

##### Logic hàm `approveComment(Integer commentId)`:
```java
@Transactional
public void approveComment(Integer commentId) {
    Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Bình luận không tồn tại"));

    comment.setStatus(Comment.STATUS_APPROVED);
    comment.setUpdatedDate(LocalDateTime.now());
    commentRepository.save(comment);

    // Cập nhật tăng số đếm bình luận của bài viết
    NewsArticle article = newsArticleRepository.findById(comment.getNewsArticleId()).orElse(null);
    if (article != null) {
        article.setCommentCount(article.getCommentCount() + 1);
        newsArticleRepository.save(article);
    }
}
```

---

## 3. GIAO DIỆN FRONTEND (REACT VITE) CHO DEV 2

Dev 2 làm việc độc lập trong `src/components/comment/`, `src/pages/moderation/` và `src/api/comment.js`:

1. **Khung Bình luận Đa cấp dưới bài viết (`src/components/comment/CommentSection.jsx`)**:
   - Form soạn thảo bình luận (Avatar người dùng, Textarea, nút Gửi).
   - Danh sách bình luận dạng cây (Tree Rendering): Hiển thị thụt đầu dòng (Indentation) cho các câu trả lời con (Replies).
   - Nút **"Trả lời" (Reply)**: Bấm vào sẽ mở ô nhập phản hồi trực tiếp ngay dưới bình luận đó.
   - Nút **"Báo cáo" (Report Abuse)**: Mở Modal chọn lý do (Spam, Xúc phạm, Sai sự thật) và gửi báo cáo (`POST /api/v1/comments/{id}/report`).
   - Cảnh báo trạng thái: Nếu bình luận đang chờ duyệt, hiển thị nhãn mờ: *"Bình luận của bạn đang chờ kiểm duyệt viên phê duyệt"*.
2. **Bàn làm việc của Kiểm duyệt viên (`src/pages/moderation/ModeratorDashboard.jsx`)**:
   - Tab 1: **Bình luận Chờ duyệt** (Hiển thị các bình luận bị cờ nhạy cảm hoặc từ tài khoản Unverified, có 2 nút bấm nhanh **"Duyệt" (Approve)** và **"Từ chối" (Reject)**).
   - Tab 2: **Bình luận Bị báo cáo Vi phạm** (Hiển thị lý do độc giả báo cáo, số lần bị báo cáo, nút **"Giữ lại"** hoặc **"Xóa bài viết vi phạm"**).
   - Tab 3: **Quản lý Từ điển Từ cấm** (Bảng danh sách từ khóa cấm, Form thêm từ cấm mới và thiết lập mức độ BLOCK/FLAG).
