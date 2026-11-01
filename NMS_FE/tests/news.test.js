import test from 'node:test';
import assert from 'node:assert/strict';

test('news payload normalizes title, content and maps categoryId to number', () => {
  const raw = {
    newsTitle: '  React 19 Released  ',
    headline: '  Exciting new features  ',
    newsContent: 'Full article body here',
    newsSource: 'Tech Blog',
    categoryId: '3',
    newsStatus: 1,
    tagIds: [1, 2],
  };

  const payload = {
    newsTitle: raw.newsTitle.trim(),
    headline: raw.headline.trim(),
    newsContent: raw.newsContent.trim(),
    newsSource: raw.newsSource.trim(),
    categoryId: Number(raw.categoryId),
    newsStatus: Number(raw.newsStatus),
    tagIds: raw.tagIds,
  };

  assert.equal(payload.newsTitle, 'React 19 Released');
  assert.equal(payload.headline, 'Exciting new features');
  assert.equal(payload.categoryId, 3);
  assert.equal(payload.newsStatus, 1);
  assert.deepEqual(payload.tagIds, [1, 2]);
});

test('article validation requires non-empty title, content, and valid category', () => {
  const validateArticle = (item) => {
    const errors = {};
    if (!item.newsTitle || !item.newsTitle.trim()) {
      errors.newsTitle = 'Tiêu đề không được để trống';
    }
    if (!item.newsContent || !item.newsContent.trim()) {
      errors.newsContent = 'Nội dung không được để trống';
    }
    if (!item.categoryId) {
      errors.categoryId = 'Vui lòng chọn danh mục';
    }
    return errors;
  };

  const invalid = validateArticle({ newsTitle: '', newsContent: '   ', categoryId: '' });
  assert.ok(invalid.newsTitle);
  assert.ok(invalid.newsContent);
  assert.ok(invalid.categoryId);

  const valid = validateArticle({
    newsTitle: 'Thông báo',
    newsContent: 'Nội dung chi tiết',
    categoryId: 2,
  });
  assert.deepEqual(valid, {});
});

test('status toggle correctly switches between 0 and 1', () => {
  const toggle = (current) => (current === 1 ? 0 : 1);
  assert.equal(toggle(1), 0);
  assert.equal(toggle(0), 1);
});
