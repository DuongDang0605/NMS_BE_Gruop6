import { Form, Pagination } from 'react-bootstrap';

const NewsPagination = ({ safePage, totalPages, totalItems, pageSize, setCurrentPage, setPageSize }) => (
  <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mt-4">
    <div className="text-muted small">
      Hiển thị từ <strong>{(safePage - 1) * pageSize + 1}</strong> đến{' '}
      <strong>{Math.min(safePage * pageSize, totalItems)}</strong> trong tổng số{' '}
      <strong>{totalItems}</strong> bài viết
    </div>

    <div className="d-flex align-items-center gap-3">
      <Pagination className="mb-0">
        <Pagination.First disabled={safePage === 1} onClick={() => setCurrentPage(1)} />
        <Pagination.Prev
          disabled={safePage === 1}
          onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
        />

        {Array.from({ length: totalPages }, (_, i) => i + 1)
          .filter((p) => p === 1 || p === totalPages || Math.abs(p - safePage) <= 1)
          .map((p, idx, arr) => {
            const prev = arr[idx - 1];
            return (
              <span key={p} className="d-inline-flex">
                {prev && p - prev > 1 && <Pagination.Ellipsis disabled />}
                <Pagination.Item active={p === safePage} onClick={() => setCurrentPage(p)}>
                  {p}
                </Pagination.Item>
              </span>
            );
          })}

        <Pagination.Next
          disabled={safePage === totalPages}
          onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
        />
        <Pagination.Last
          disabled={safePage === totalPages}
          onClick={() => setCurrentPage(totalPages)}
        />
      </Pagination>

      <Form.Select
        size="sm"
        value={pageSize}
        onChange={(e) => {
          setPageSize(Number(e.target.value));
          setCurrentPage(1);
        }}
        style={{ width: 'auto' }}
      >
        <option value="10">10 / trang</option>
        <option value="20">20 / trang</option>
        <option value="50">50 / trang</option>
      </Form.Select>
    </div>
  </div>
);

export default NewsPagination;
