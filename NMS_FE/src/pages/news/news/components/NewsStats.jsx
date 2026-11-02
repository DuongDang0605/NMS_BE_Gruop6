import { useMemo } from 'react';
import { Row, Col } from 'react-bootstrap';
import { normalizeStatus, ARTICLE_STATUS } from '../constants';

const NewsStats = ({ articles }) => {
  const stats = useMemo(() => {
    const total = articles.length;
    const active = articles.filter(
      (a) => normalizeStatus(a.newsStatus) === ARTICLE_STATUS.PUBLISHED
    ).length;
    const inactive = total - active;
    const uniqueCategories = new Set(
      articles.map((a) => a.categoryId).filter(Boolean)
    ).size;

    return { total, active, inactive, uniqueCategories };
  }, [articles]);

  const cards = [
    {
      color: 'orange',
      label: 'Tổng bài viết',
      value: stats.total,
      valueClass: 'text-dark',
      icon: 'bi-journal-richtext',
    },
    {
      color: 'green',
      label: 'Đã xuất bản (Active)',
      value: stats.active,
      valueClass: 'text-success',
      icon: 'bi-check-circle-fill',
    },
    {
      color: 'blue',
      label: 'Lưu nháp / Ẩn',
      value: stats.inactive,
      valueClass: 'text-secondary',
      icon: 'bi-eye-slash-fill',
    },
    {
      color: 'purple',
      label: 'Chuyên mục sử dụng',
      value: stats.uniqueCategories,
      valueStyle: { color: '#7e22ce' },
      icon: 'bi-folder2-open',
    },
  ];

  return (
    <Row className="g-3 mb-4">
      {cards.map((c) => (
        <Col xs={6} md={3} key={c.color}>
          <div className={`news-stat-card ${c.color}`}>
            <div>
              <div className="text-muted small fw-semibold">{c.label}</div>
              <div
                className={`fs-4 fw-bold ${c.valueClass || ''}`}
                style={c.valueStyle}
              >
                {c.value}
              </div>
            </div>
            <div className="stat-icon-wrapper">
              <i className={`bi ${c.icon}`}></i>
            </div>
          </div>
        </Col>
      ))}
    </Row>
  );
};

export default NewsStats;
