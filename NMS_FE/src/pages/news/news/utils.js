export const formatDate = (isoString) => {
  if (!isoString) return '—';
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
};

// Danh sách tag (object) của 1 bài viết
export const getArticleTags = (article, tagMap) =>
  Array.isArray(article.tags)
    ? article.tags
    : Array.isArray(article.tagIds)
    ? article.tagIds.map((id) => tagMap.get(id)).filter(Boolean)
    : [];

const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;

// Xuất file CSV (hỗ trợ Excel tiếng Việt UTF-8 BOM)
export const exportArticlesToCSV = (articles, categoryMap, tagMap) => {
  if (articles.length === 0) {
    alert('Không có dữ liệu bài viết để xuất file!');
    return;
  }

  const headers = [
    'ID',
    'Tiêu đề',
    'Headline',
    'Danh mục',
    'Nguồn tin',
    'Tác giả',
    'Ngày tạo',
    'Trạng thái',
    'Thẻ (Tags)',
  ];

  const rows = articles.map((a) => {
    const cat = categoryMap.get(a.categoryId) || a.categoryName || '';
    const tagNames = Array.isArray(a.tagIds)
      ? a.tagIds.map((id) => tagMap.get(id)?.tagName || '').filter(Boolean).join('; ')
      : Array.isArray(a.tags)
      ? a.tags.map((t) => t.tagName).join('; ')
      : '';

    return [
      a.newsArticleId,
      esc(a.newsTitle),
      esc(a.headline),
      esc(cat),
      esc(a.newsSource),
      esc(a.createdByName),
      esc(a.createdDate),
      a.newsStatus === 1 ? 'Active' : 'Inactive',
      esc(tagNames),
    ];
  });

  const csvContent =
    '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute(
    'download',
    `FPT_News_Export_${new Date().toISOString().slice(0, 10)}.csv`
  );
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
