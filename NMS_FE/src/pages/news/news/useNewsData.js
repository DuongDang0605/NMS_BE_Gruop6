import { useEffect, useMemo, useState } from 'react';
import { getNewsArticles } from '../../../api/news';
import { getCategories } from '../../../api/category';
import { getTags } from '../../../api/tag';

// Tải bài viết, danh mục, thẻ từ Backend API
const useNewsData = () => {
  const [articles, setArticles] = useState([]);
  const [categories, setCategories] = useState([]);
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refreshIndex, setRefreshIndex] = useState(0);

  useEffect(() => {
    let isMounted = true;

    const fetchAllData = async () => {
      try {
        const [newsRes, catRes, tagRes] = await Promise.all([
          getNewsArticles(),
          getCategories().catch(() => []),
          getTags().catch(() => []),
        ]);

        if (isMounted) {
          setArticles(Array.isArray(newsRes) ? newsRes : []);
          setCategories(Array.isArray(catRes) ? catRes : []);
          setTags(Array.isArray(tagRes) ? tagRes : []);
          setError('');
          setLoading(false);
        }
      } catch (err) {
        if (isMounted) {
          setError(
            err?.response?.data?.message ||
              'Không thể kết nối đến máy chủ backend để tải tin tức.'
          );
          setLoading(false);
        }
      }
    };

    fetchAllData();
    return () => {
      isMounted = false;
    };
  }, [refreshIndex]);

  const reloadData = () => {
    setLoading(true);
    setRefreshIndex((prev) => prev + 1);
  };

  // Tra cứu Map ID -> Name
  const categoryMap = useMemo(() => {
    const map = new Map();
    categories.forEach((c) => map.set(c.categoryId, c.categoryName));
    return map;
  }, [categories]);

  const tagMap = useMemo(() => {
    const map = new Map();
    tags.forEach((t) => map.set(t.tagId, t));
    return map;
  }, [tags]);

  return {
    articles,
    setArticles,
    categories,
    tags,
    setTags,
    categoryMap,
    tagMap,
    loading,
    error,
    reloadData,
  };
};

export default useNewsData;
