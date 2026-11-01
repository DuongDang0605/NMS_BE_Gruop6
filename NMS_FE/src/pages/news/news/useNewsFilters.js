import { useMemo, useState } from 'react';

const toTime = (d) => (d ? new Date(d).getTime() : 0);

// Tìm kiếm, lọc, sắp xếp, phân trang, chế độ xem
const useNewsFilters = (articles) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterCategory, setFilterCategory] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterTag, setFilterTag] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [viewMode, setViewMode] = useState('table'); // 'table' | 'card'
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const filteredAndSortedArticles = useMemo(() => {
    let list = [...articles];

    if (searchTerm.trim()) {
      const q = searchTerm.trim().toLowerCase();
      list = list.filter(
        (a) =>
          a.newsTitle?.toLowerCase().includes(q) ||
          a.headline?.toLowerCase().includes(q) ||
          a.newsSource?.toLowerCase().includes(q) ||
          a.createdByName?.toLowerCase().includes(q)
      );
    }

    if (filterCategory !== '') {
      list = list.filter((a) => String(a.categoryId) === String(filterCategory));
    }

    if (filterStatus !== '') {
      list = list.filter((a) => String(a.newsStatus) === String(filterStatus));
    }

    if (filterTag !== '') {
      const tId = Number(filterTag);
      list = list.filter((a) => Array.isArray(a.tagIds) && a.tagIds.includes(tId));
    }

    list.sort((a, b) => {
      switch (sortBy) {
        case 'newest':
          return toTime(b.createdDate) - toTime(a.createdDate);
        case 'oldest':
          return toTime(a.createdDate) - toTime(b.createdDate);
        case 'title_asc':
          return (a.newsTitle || '').localeCompare(b.newsTitle || '');
        case 'title_desc':
          return (b.newsTitle || '').localeCompare(a.newsTitle || '');
        case 'id_desc':
          return (b.newsArticleId || 0) - (a.newsArticleId || 0);
        case 'id_asc':
          return (a.newsArticleId || 0) - (b.newsArticleId || 0);
        default:
          return 0;
      }
    });

    return list;
  }, [articles, searchTerm, filterCategory, filterStatus, filterTag, sortBy]);

  const totalItems = filteredAndSortedArticles.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const pagedArticles = useMemo(() => {
    const start = (safePage - 1) * pageSize;
    return filteredAndSortedArticles.slice(start, start + pageSize);
  }, [filteredAndSortedArticles, safePage, pageSize]);

  const hasActiveFilters =
    Boolean(searchTerm.trim()) ||
    filterCategory !== '' ||
    filterStatus !== '' ||
    filterTag !== '';

  const handleClearFilters = () => {
    setSearchTerm('');
    setFilterCategory('');
    setFilterStatus('');
    setFilterTag('');
    setCurrentPage(1);
  };

  return {
    searchTerm, setSearchTerm,
    filterCategory, setFilterCategory,
    filterStatus, setFilterStatus,
    filterTag, setFilterTag,
    sortBy, setSortBy,
    viewMode, setViewMode,
    pageSize, setPageSize,
    setCurrentPage,
    filteredAndSortedArticles,
    pagedArticles,
    totalItems,
    totalPages,
    safePage,
    hasActiveFilters,
    handleClearFilters,
  };
};

export default useNewsFilters;
