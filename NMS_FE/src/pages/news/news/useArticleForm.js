import { useState } from 'react';
import { createNewsArticle, updateNewsArticle } from '../../../api/news';
import { getSession } from '../../../services/session';
import { DEFAULT_ARTICLE } from './constants';

// Modal Thêm / Sửa bài viết
const useArticleForm = ({ setActionError, onSuccess, reloadData }) => {
  const [show, setShow] = useState(false);
  const [formData, setFormData] = useState(DEFAULT_ARTICLE);
  const [activeTab, setActiveTab] = useState('write'); // 'write' | 'preview'
  const [submitting, setSubmitting] = useState(false);

  const open = (article = null) => {
    setActionError('');
    setActiveTab('write');
    if (article) {
      setFormData({
        newsArticleId: article.newsArticleId,
        newsTitle: article.newsTitle || '',
        headline: article.headline || '',
        newsContent: article.newsContent || '',
        newsSource: article.newsSource || '',
        categoryId: article.categoryId ? String(article.categoryId) : '',
        newsStatus: article.newsStatus !== undefined ? article.newsStatus : 1,
        tagIds: Array.isArray(article.tagIds)
          ? article.tagIds
          : Array.isArray(article.tags)
          ? article.tags.map((t) => t.tagId)
          : [],
      });
    } else {
      setFormData(DEFAULT_ARTICLE);
    }
    setShow(true);
  };

  const close = () => {
    setShow(false);
    setActionError('');
  };

  const toggleTag = (tagId) => {
    setFormData((prev) => {
      const exists = prev.tagIds.includes(tagId);
      return {
        ...prev,
        tagIds: exists
          ? prev.tagIds.filter((id) => id !== tagId)
          : [...prev.tagIds, tagId],
      };
    });
  };

  const save = async (e) => {
    e.preventDefault();

    if (!formData.newsTitle.trim()) {
      setActionError('Tiêu đề bài viết không được để trống.');
      return;
    }
    if (!formData.newsContent.trim()) {
      setActionError('Nội dung bài viết không được để trống.');
      return;
    }
    if (!formData.categoryId) {
      setActionError('Vui lòng chọn danh mục cho bài viết.');
      return;
    }

    try {
      setSubmitting(true);
      setActionError('');

      const session = getSession();
      const currentUserId = session?.user?.accountId || null;

      const payload = {
        newsTitle: formData.newsTitle.trim(),
        headline: formData.headline ? formData.headline.trim() : null,
        newsContent: formData.newsContent.trim(),
        newsSource: formData.newsSource ? formData.newsSource.trim() : null,
        categoryId: Number(formData.categoryId),
        newsStatus: Number(formData.newsStatus),
        tagIds: formData.tagIds || [],
      };

      if (formData.newsArticleId) {
        if (currentUserId) payload.updatedById = currentUserId;
        await updateNewsArticle(formData.newsArticleId, payload);
        onSuccess(`Cập nhật bài viết #${formData.newsArticleId} thành công!`);
      } else {
        if (currentUserId) payload.createdById = currentUserId;
        await createNewsArticle(payload);
        onSuccess('Đã thêm bài viết mới thành công!');
      }

      setShow(false);
      reloadData();
    } catch (err) {
      setActionError(
        err?.response?.data?.message ||
          'Không thể lưu bài viết. Vui lòng kiểm tra lại thông tin.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return {
    show, open, close,
    formData, setFormData,
    activeTab, setActiveTab,
    submitting,
    toggleTag, save,
  };
};

export default useArticleForm;
