import { useEffect, useState, useMemo } from "react";
import { Spinner } from "react-bootstrap";
import { useNavigate, useOutletContext } from "react-router-dom";

import useNewsData from "./news/useNewsData";
import useNewsFilters from "./news/useNewsFilters";
import useArticleForm from "./news/useArticleForm";
import useArticleActions from "./news/useArticleActions";
import useTagManager from "./news/useTagManager";
import { exportArticlesToCSV } from "./news/utils";

import NewsHeader from "./news/components/NewsHeader";
import NewsAlerts from "./news/components/NewsAlerts";
import NewsStats from "./news/components/NewsStats";
import NewsFilters from "./news/components/NewsFilters";
import NewsEmptyState from "./news/components/NewsEmptyState";
import NewsTable from "./news/components/NewsTable";
import NewsCardGrid from "./news/components/NewsCardGrid";
import NewsPagination from "./news/components/NewsPagination";
import ArticleFormModal from "./news/components/ArticleFormModal";
import ArticleDetailModal from "./news/components/ArticleDetailModal";
import TagManagerModal from "./news/components/TagManagerModal";
import DeleteConfirmModal from "./news/components/DeleteConfirmModal";

const NewsManager = () => {
  const navigate = useNavigate();
  const { user } = useOutletContext();

  // --- Logic phân quyền (RBAC) từ nhánh develop ---
  const canPublish = [1, 3].includes(user?.accountRole);
  const canEdit = (article) =>
    user?.accountRole === 1 ||
    (article?.createdById === user?.accountId && article?.newsStatus === 0);

  const {
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
  } = useNewsData();

  // Lọc bài viết theo quyền: Role 2 (Contributor) chỉ thấy bài của mình
  const allowedArticles = useMemo(() => {
    if (!articles) return [];
    return articles.filter(
      (n) => user?.accountRole !== 2 || n.createdById === user?.accountId,
    );
  }, [articles, user]);

  const [actionError, setActionError] = useState("");
  const [successToast, setSuccessToast] = useState("");
  const [showDetail, setShowDetail] = useState(false);
  const [detailArticle, setDetailArticle] = useState(null);

  // Tự ẩn thông báo thành công sau 4 giây
  useEffect(() => {
    if (!successToast) return;
    const timer = setTimeout(() => setSuccessToast(""), 4000);
    return () => clearTimeout(timer);
  }, [successToast]);

  // Sử dụng allowedArticles đã được phân quyền thay vì toàn bộ articles
  const filters = useNewsFilters(allowedArticles);
  const form = useArticleForm({
    setActionError,
    onSuccess: setSuccessToast,
    reloadData,
  });
  const actions = useArticleActions({
    articles: allowedArticles,
    setArticles,
    setActionError,
    onSuccess: setSuccessToast,
    reloadData,
  });
  const tagManager = useTagManager({ setTags, onSuccess: setSuccessToast });

  const openDetail = (article) => {
    setDetailArticle(article);
    setShowDetail(true);
  };
  const closeDetail = () => setShowDetail(false);

  const listProps = {
    articles: filters.pagedArticles,
    categoryMap,
    tagMap,
    user, // Truyền user xuống để table check quyền
    canEdit, // Truyền hàm check quyền edit
    canPublish, // Truyền cờ check quyền publish
    onView: openDetail,
    onEdit: form.open,
    onToggleStatus: actions.toggleStatus,
    onDelete: actions.openDelete,
  };

  return (
    <div className="pb-5">
      <NewsHeader
        total={allowedArticles.length}
        tagCount={tags.length}
        onReload={reloadData}
        onOpenTags={tagManager.open}
        onExport={() =>
          exportArticlesToCSV(
            filters.filteredAndSortedArticles,
            categoryMap,
            tagMap,
          )
        }
        onCreate={() => form.open()}
      />

      <NewsAlerts
        successToast={successToast}
        onCloseSuccess={() => setSuccessToast("")}
        error={error}
        onRetry={reloadData}
        actionError={!form.show && !actions.showDelete ? actionError : ""}
        onCloseAction={() => setActionError("")}
      />

      <NewsStats articles={allowedArticles} />
      <NewsFilters filters={filters} categories={categories} tags={tags} />

      {loading ? (
        <div className="text-center py-5 bg-white rounded-4 shadow-sm border">
          <Spinner animation="border" variant="primary" />
          <div className="mt-2 text-muted fw-semibold">
            Đang tải dữ liệu bài viết...
          </div>
        </div>
      ) : filters.totalItems === 0 ? (
        <NewsEmptyState
          hasActiveFilters={filters.hasActiveFilters}
          onClear={filters.handleClearFilters}
          onCreate={() => form.open()}
        />
      ) : filters.viewMode === "table" ? (
        <NewsTable {...listProps} />
      ) : (
        <NewsCardGrid {...listProps} />
      )}

      {filters.totalPages > 1 && (
        <NewsPagination
          safePage={filters.safePage}
          totalPages={filters.totalPages}
          totalItems={filters.totalItems}
          pageSize={filters.pageSize}
          setCurrentPage={filters.setCurrentPage}
          setPageSize={filters.setPageSize}
        />
      )}

      <ArticleFormModal
        form={form}
        categories={categories}
        tags={tags}
        categoryMap={categoryMap}
        tagMap={tagMap}
        actionError={actionError}
        onOpenTagModal={tagManager.open}
        user={user}
        canPublish={canPublish}
        canEdit={canEdit}
      />

      <ArticleDetailModal
        show={showDetail}
        article={detailArticle}
        categoryMap={categoryMap}
        onHide={closeDetail}
        onEdit={() => {
          closeDetail();
          form.open(detailArticle);
        }}
        onOpenPortal={() => {
          closeDetail();
          navigate(`/portal/articles/${detailArticle.newsArticleId}`);
        }}
        canEdit={canEdit}
      />

      <TagManagerModal tags={tags} manager={tagManager} />

      <DeleteConfirmModal
        show={actions.showDelete}
        target={actions.deleteTarget}
        submitting={actions.submitting}
        onHide={actions.closeDelete}
        onConfirm={actions.confirmDelete}
      />
    </div>
  );
};

export default NewsManager;
