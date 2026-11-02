export const ARTICLE_STATUS = {
  DRAFT: 'DRAFT',
  PENDING_REVIEW: 'PENDING_REVIEW',
  CHANGES_REQUESTED: 'CHANGES_REQUESTED',
  APPROVED: 'APPROVED',
  SCHEDULED: 'SCHEDULED',
  PUBLISHED: 'PUBLISHED',
  ARCHIVED: 'ARCHIVED',
};

export const STATUS_CONFIG = {
  PUBLISHED: {
    key: 'PUBLISHED',
    label: 'Đã xuất bản',
    badgeVariant: 'success',
    badgeClass: 'bg-success text-white',
    icon: 'bi-check-circle-fill',
    isPublished: true,
  },
  DRAFT: {
    key: 'DRAFT',
    label: 'Bản nháp',
    badgeVariant: 'secondary',
    badgeClass: 'bg-secondary text-white',
    icon: 'bi-file-earmark-text',
    isPublished: false,
  },
  PENDING_REVIEW: {
    key: 'PENDING_REVIEW',
    label: 'Chờ duyệt',
    badgeVariant: 'warning',
    badgeClass: 'bg-warning text-dark',
    icon: 'bi-hourglass-split',
    isPublished: false,
  },
  CHANGES_REQUESTED: {
    key: 'CHANGES_REQUESTED',
    label: 'Yêu cầu sửa',
    badgeVariant: 'danger',
    badgeClass: 'bg-danger text-white',
    icon: 'bi-exclamation-triangle-fill',
    isPublished: false,
  },
  APPROVED: {
    key: 'APPROVED',
    label: 'Đã duyệt',
    badgeVariant: 'info',
    badgeClass: 'bg-info text-white',
    icon: 'bi-patch-check-fill',
    isPublished: false,
  },
  SCHEDULED: {
    key: 'SCHEDULED',
    label: 'Đã lên lịch',
    badgeVariant: 'primary',
    badgeClass: 'bg-primary text-white',
    icon: 'bi-clock-history',
    isPublished: false,
  },
  ARCHIVED: {
    key: 'ARCHIVED',
    label: 'Lưu trữ / Ẩn',
    badgeVariant: 'dark',
    badgeClass: 'bg-dark text-white',
    icon: 'bi-archive-fill',
    isPublished: false,
  },
};

export const normalizeStatus = (status) => {
  if (status === null || status === undefined || status === '') return ARTICLE_STATUS.DRAFT;
  const s = String(status).trim().toUpperCase();
  if (s === '1' || s === 'ACTIVE') return ARTICLE_STATUS.PUBLISHED;
  if (s === '0' || s === 'INACTIVE') return ARTICLE_STATUS.ARCHIVED;
  if (STATUS_CONFIG[s]) return s;
  return ARTICLE_STATUS.DRAFT;
};

export const DEFAULT_ARTICLE = {
  newsArticleId: null,
  newsTitle: '',
  headline: '',
  newsContent: '',
  newsSource: '',
  categoryId: '',
  newsStatus: ARTICLE_STATUS.DRAFT,
  tagIds: [],
};
