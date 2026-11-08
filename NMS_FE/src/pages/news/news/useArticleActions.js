import { useState } from 'react';
import { updateNewsArticleStatus, deleteNewsArticle } from '../../../api/news';
import { normalizeStatus, STATUS_CONFIG, ARTICLE_STATUS } from './constants';

// Đổi trạng thái nhanh + Xóa bài viết
const useArticleActions = ({ articles, setArticles, setActionError, onSuccess, reloadData }) => {
  const [showDelete, setShowDelete] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Đổi sang một trạng thái cụ thể
  const changeStatus = async (article, targetStatus, e) => {
    if (e) e.stopPropagation();
    const current = normalizeStatus(article.newsStatus);
    const newStatus = normalizeStatus(targetStatus);
    if (current === newStatus) return;

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
      const label = STATUS_CONFIG[newStatus]?.label || newStatus;
      onSuccess(`Đã chuyển trạng thái bài viết #${article.newsArticleId} sang "${label}"`);
    } catch (err) {
      setArticles(oldArticles);
      setActionError(
        err?.response?.data?.message || 'Không thể cập nhật trạng thái bài viết.'
      );
    }
  };

  // Toggle nhanh: Chỉ cho phép chuyển đổi hợp lệ giữa PUBLISHED <-> ARCHIVED
  const toggleStatus = async (article, e) => {
    if (e) e.stopPropagation();
    const current = normalizeStatus(article.newsStatus);
    if (current !== ARTICLE_STATUS.PUBLISHED && current !== ARTICLE_STATUS.ARCHIVED) {
      setActionError('Chỉ có thể chuyển đổi nhanh giữa trạng thái Đã xuất bản (PUBLISHED) và Lưu trữ (ARCHIVED). Các trạng thái khác cần xử lý theo quy trình biên tập.');
      return;
    }
    const newStatus =
      current === ARTICLE_STATUS.PUBLISHED
        ? ARTICLE_STATUS.ARCHIVED
        : ARTICLE_STATUS.PUBLISHED;
    return changeStatus(article, newStatus, e);
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
    changeStatus,
    showDelete,
    deleteTarget,
    submitting,
    openDelete,
    closeDelete,
    confirmDelete,
  };
};

export default useArticleActions;
