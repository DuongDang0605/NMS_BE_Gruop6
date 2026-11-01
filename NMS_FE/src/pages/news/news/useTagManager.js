import { useState } from 'react';
import { createTag, deleteTag } from '../../../api/tag';

// Modal quản lý thẻ Tag
const useTagManager = ({ setTags, onSuccess }) => {
  const [show, setShow] = useState(false);
  const [name, setName] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Tên thẻ tag không được để trống.');
      return;
    }
    try {
      setLoading(true);
      setError('');
      const created = await createTag({
        tagName: name.trim(),
        note: note.trim() || null,
      });
      setTags((prev) => [...prev, created]);
      setName('');
      setNote('');
      onSuccess(`Đã tạo mới thẻ tag "${created.tagName}"!`);
    } catch (err) {
      setError(
        err?.response?.data?.message || 'Không thể tạo thẻ tag. Vui lòng thử lại.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (tagId) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa thẻ tag này?')) return;
    try {
      await deleteTag(tagId);
      setTags((prev) => prev.filter((t) => t.tagId !== tagId));
      onSuccess('Đã xóa thẻ tag thành công!');
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          'Không thể xóa tag (có thể do bài viết đang sử dụng tag này).'
      );
    }
  };

  return {
    show,
    open: () => setShow(true),
    close: () => setShow(false),
    name, setName,
    note, setNote,
    error, setError,
    loading,
    handleCreate,
    handleDelete,
  };
};

export default useTagManager;
