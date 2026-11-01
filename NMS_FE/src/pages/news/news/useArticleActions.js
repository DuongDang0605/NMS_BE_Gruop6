import { useState } from 'react';
import { updateNewsArticleStatus, deleteNewsArticle } from '../../../api/news';

// Đổi trạng thái nhanh + Xóa bài viết
const useArticleActions = ({ articles, setArticles, setActionError, onSuccess, reloadData }) => {
  const [showDelete, setShowDelete] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Toggle Status nhanh (1-click, optimistic update)
  const toggleStatus = async (article, e) => {
    e.stopPropagation();
    const newStatus = article.newsStatus === 1 ? 0 : 1;
    const oldArticles = [...articles];

    setArticles((prev) =>
      prev.map((item) =>
        item.newsArticleId === article.newsArticleId
          ? { ...item, newsStatus: newStatus }
          : item
      )
    );

    try {
      await updateNewsArticleStatus(article.newsArticleId, newStatus);
      onSuccess(
        `Đã đổi trạng thái bài viết #${article.newsArticleId} sang ${
          newStatus === 1 ? 'Đang hoạt động (Active)' : 'Đã ẩn (Inactive)'
        }`
      );
    } catch (err) {
      setArticles(oldArticles);
      setActionError(
        err?.response?.data?.message || 'Không thể cập nhật trạng thái bài viết.'
      );
    }
  };

  const openDelete = (article, e) => {
    if (e) e.stopPropagation();
    setDeleteTarget(article);
    setShowDelete(true);
  };

  const closeDelete = () => setShowDelete(false);

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      setSubmitting(true);
      await deleteNewsArticle(deleteTarget.newsArticleId);
      setShowDelete(false);
      setDeleteTarget(null);
      onSuccess(`Đã xoá thành công bài viết #${deleteTarget.newsArticleId}!`);
      reloadData();
    } catch (err) {
      setActionError(
        err?.response?.data?.message ||
          'Không thể xóa bài viết này (có thể do ràng buộc dữ liệu).'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return {
    toggleStatus,
    showDelete, deleteTarget, submitting,
    openDelete, closeDelete, confirmDelete,
  };
};

export default useArticleActions;
