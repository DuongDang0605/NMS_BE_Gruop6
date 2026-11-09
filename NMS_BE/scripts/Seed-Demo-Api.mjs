import { appendFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// All application writes go through HTTP. SQL, when enabled, only reads row counts.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const base = (process.env.DEMO_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const runId = process.env.DEMO_RUN_ID || `demo-${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)}`;
if (!/^[a-zA-Z0-9-]{4,40}$/.test(runId)) throw new Error('DEMO_RUN_ID must contain 4–40 letters, numbers or hyphens.');
const out = path.resolve(root, 'docs', 'demo-data', runId);
const completeNotificationsOnly = process.env.DEMO_MODE === 'complete-notifications';
const reportOnly = process.env.DEMO_MODE === 'report';
if (existsSync(path.join(out, 'api-log.jsonl')) && !completeNotificationsOnly && !reportOnly) throw new Error(`Run ${runId} already exists; use another DEMO_RUN_ID to avoid duplicate inserts.`);
const previous = completeNotificationsOnly || reportOnly ? JSON.parse(readFileSync(path.join(out, 'summary.json'), 'utf8')) : null;
mkdirSync(out, { recursive: true });
const logPath = path.join(out, 'api-log.jsonl');
const demoPassword = process.env.DEMO_PASSWORD || 'Demo@2026!';
const adminEmail = process.env.DEMO_ADMIN_EMAIL || 'admin@fu.edu.vn';
const adminPassword = process.env.DEMO_ADMIN_PASSWORD;
if (!adminPassword && !reportOnly) throw new Error('Set DEMO_ADMIN_PASSWORD before running. Admin credentials are never written to reports.');
const suffix = runId.replace(/^demo-/, '').toLowerCase();
const records = { SystemAccount: [], Category: [], Tag: [], SystemSetting: [], BannedKeyword: [], NewsArticle: [], NewsTag: [], Comment: [], CommentReport: [], ArticleReaction: [], Notification: [], ArticleReviewNote: [], ArticleAuditLog: [] };
const log = [], failures = [], skips = [], checks = [];
if (previous) {
  Object.assign(records, previous.records);
  for (const entry of readFileSync(logPath, 'utf8').trim().split(/\r?\n/).filter(Boolean).map(line => JSON.parse(line))) {
    if (entry.outcome === 'FAILED' && entry.path === '/api/v1/notifications' && entry.request?.isRead === 1
        && entry.response?.message === 'isRead must be 0 when creating a notification') {
      entry.failureCategory = 'SEED_INPUT';
      entry.explanation = 'The seed script submitted isRead=1. The API correctly rejected it; create unread, then call the read endpoint.';
    }
    log.push(entry);
    if (entry.outcome === 'FAILED') failures.push(entry);
    if (entry.outcome === 'SKIPPED') skips.push(entry);
  }
}
const failedEndpoints = new Map();
const sessions = new Map();
const roleNames = { 1: 'Admin', 2: 'Contributor', 3: 'Editor', 4: 'Moderator', 5: 'Reader' };
let beforeCounts = null;
let apiSpec = null;
let finished = false;
if (previous) {
  beforeCounts = Object.fromEntries(previous.tables.map(t => [t.table, t.before]));
  apiSpec = { paths: {} };
  for (const entry of previous.inventory) (apiSpec.paths[entry.path] ??= {})[entry.method.toLowerCase()] = {};
}

function clean(value) {
  if (Array.isArray(value)) return value.map(clean);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(Object.entries(value).map(([key, val]) => [key, /password|token|secret|authorization/i.test(key) ? '[REDACTED]' : clean(val)]));
}
function summarize(value, method) {
  if (method !== 'GET') return clean(value);
  if (Array.isArray(value)) return { rows: value.length, sampleIds: value.slice(0, 5).map(v => Object.fromEntries(Object.entries(v).filter(([k]) => /Id$/.test(k)))) };
  if (Array.isArray(value?.content)) return { totalElements: value.totalElements, totalPages: value.totalPages, page: value.number ?? value.page, rows: value.content.length };
  if (value?.openapi) return { openapi: value.openapi, paths: Object.keys(value.paths || {}).length };
  return clean(value);
}
function endpointKey(method, uri) {
  const actualPath = uri.split('?')[0];
  const specPath = apiSpec?.paths?.[actualPath] ? actualPath : Object.keys(apiSpec?.paths || {}).find(candidate => {
    const parts = candidate.split('/').map(part => /^\{[^}]+\}$/.test(part) ? '[^/]+' : part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    return new RegExp(`^${parts.join('/')}$`).test(actualPath);
  });
  return `${method} ${(specPath || actualPath.replace(/\/\d+(?=\/|$)/g, '/{id}')).replace(/\{[^}]+\}/g, '{id}')}`;
}
function record(entry) {
  log.push(entry);
  appendFileSync(logPath, JSON.stringify(entry) + '\n', 'utf8');
}
function skip(label, reason) {
  const entry = { at: new Date().toISOString(), outcome: 'SKIPPED', label, reason };
  skips.push(entry); record(entry);
  return null;
}
async function api(method, uri, actor = null, body = undefined, label = uri, expected = undefined) {
  const key = endpointKey(method, uri);
  if (failedEndpoints.has(key)) return skip(label, `Endpoint ${key} failed earlier; skipped as requested.`);
  if (actor && !sessions.has(actor)) return skip(label, `Missing authenticated session for ${actor}.`);
  const started = Date.now();
  const entry = { at: new Date().toISOString(), method, path: uri, actor: actor || 'anonymous', label, request: clean(body) };
  try {
    const response = await fetch(base + uri, {
      method, headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(actor ? { Authorization: `Bearer ${sessions.get(actor).accessToken}` } : {}) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }), signal: AbortSignal.timeout(15000),
    });
    const raw = await response.text();
    let data = null;
    if (raw) { try { data = JSON.parse(raw); } catch { data = raw; } }
    const ok = expected ? expected.includes(response.status) : response.ok;
    Object.assign(entry, { status: response.status, elapsedMs: Date.now() - started, outcome: ok ? 'PASS' : 'FAILED', response: ok ? summarize(data, method) : clean(data) });
    record(entry);
    if (!ok) {
      failures.push(entry); failedEndpoints.set(key, entry);
      console.log(`FAILED ${key}: HTTP ${response.status} (${label})`);
      return null;
    }
    return data ?? { ok: true };
  } catch (error) {
    Object.assign(entry, { outcome: 'FAILED', status: 0, elapsedMs: Date.now() - started, response: error.message });
    failures.push(entry); failedEndpoints.set(key, entry); record(entry);
    console.log(`FAILED ${key}: ${error.message}`);
    return null;
  }
}
async function login(key, email, password) {
  const data = await api('POST', '/api/v1/auth/login', null, { accountEmail: email, accountPassword: password }, `Login ${key}`);
  if (data?.accessToken) sessions.set(key, data);
  return data;
}
function remember(table, data, extra = {}) {
  if (data) records[table].push({ ...clean(data), ...extra });
  return data;
}
function countTables() {
  if (!process.env.DEMO_SQL_SERVER) return null;
  try {
    const query = 'SET NOCOUNT ON; SELECT t.name, SUM(p.rows) FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1) GROUP BY t.name ORDER BY t.name;';
    const raw = execFileSync('sqlcmd', ['-S', process.env.DEMO_SQL_SERVER, '-E', '-C', '-d', process.env.DEMO_SQL_DATABASE || 'FUNewsManagementSystem', '-l', '5', '-b', '-h', '-1', '-W', '-s', '|', '-Q', query], { encoding: 'utf8', timeout: 15000, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
    return Object.fromEntries(raw.trim().split(/\r?\n/).map(line => line.trim().split('|')).filter(pair => pair.length === 2 && /^\d+$/.test(pair[1])).map(([name, n]) => [name, Number(n)]));
  } catch (error) { checks.push({ outcome: 'UNVERIFIED', label: 'Read-only database row counts', detail: error.message.split('\n')[0] }); return null; }
}
const people = [
  [2, 'contributor1', 'Nguyễn Minh Anh'], [2, 'contributor2', 'Trần Gia Bảo'], [2, 'contributor3', 'Lê Hoàng Phúc'],
  [3, 'editor1', 'Phạm Thu Hà'], [3, 'editor2', 'Võ Khánh Linh'],
  [4, 'moderator1', 'Bùi Đức Huy'], [4, 'moderator2', 'Đặng Ngọc Mai'],
  [5, 'reader1', 'Đỗ Hải Yến'], [5, 'reader2', 'Ngô Quốc An'],
  [5, 'reader3', 'Hoàng Bảo Ngọc', 'register'], [5, 'reader4', 'Dương Nhật Minh', 'register'],
  [5, 'inactive', 'Tài khoản demo bị vô hiệu hóa'],
];
const users = new Map(), categories = [], tags = [], articles = [], comments = [];
const categoryDefs = [
  ['Học tập và công nghệ', null], ['Đời sống sinh viên', null], ['Sự kiện học đường', null], ['Cơ hội nghề nghiệp', null],
  ['AI và dữ liệu', 0], ['Thể thao', 1], ['Hoạt động cộng đồng', 2], ['Thực tập doanh nghiệp', 3],
];
const tagDefs = ['cong-nghe', 'tri-tue-nhan-tao', 'lap-trinh', 'sinh-vien', 'cau-lac-bo', 'the-thao', 'su-kien', 'tinh-nguyen', 'hoc-bong', 'nghe-nghiep', 'thuc-tap', 'ky-nang'];
const topics = [
  ['Ngày hội câu lạc bộ: kết nối sở thích, mở rộng tình bạn', 1, 'published', 'contributor1'],
  ['Workshop AI: từ ý tưởng đến ứng dụng hỗ trợ học tập', 4, 'published', 'contributor2'],
  ['Giải bóng đá sinh viên khởi động mùa tranh tài mới', 5, 'published', 'contributor3'],
  ['Ngày hội việc làm: chuẩn bị CV và phỏng vấn cùng doanh nghiệp', 3, 'published', 'contributor1'],
  ['Sinh viên chung tay xây dựng thư viện sách cộng đồng', 6, 'published', 'contributor2'],
  ['Học bổng khuyến khích các dự án sáng tạo của sinh viên', 0, 'published', 'contributor3'],
  ['Cuộc thi lập trình đội nhóm mở đăng ký vòng sơ loại', 0, 'published', 'contributor1'],
  ['Góc học tập: năm cách quản lý thời gian trong mùa thi', 1, 'published', 'contributor2'],
  ['Chương trình thực tập: kết nối kiến thức với trải nghiệm thực tế', 7, 'published', 'contributor3'],
  ['Talkshow an toàn dữ liệu dành cho sinh viên năm nhất', 4, 'published', 'contributor1'],
  ['Đêm nhạc gây quỹ: lan tỏa tinh thần sẻ chia', 2, 'published', 'contributor2'],
  ['Câu lạc bộ tiếng Anh ra mắt chuỗi giao lưu cuối tuần', 1, 'published', 'contributor3'],
  ['Triển lãm đồ án: những ý tưởng số hướng đến cộng đồng', 0, 'published', 'contributor1'],
  ['Ngày chạy bộ xanh khuyến khích thói quen vận động', 5, 'published', 'contributor2'],
  ['Gặp gỡ cựu sinh viên: hành trang cho công việc đầu tiên', 3, 'published', 'contributor3'],
  ['Văn hóa đọc trong thời đại số: cùng chia sẻ một cuốn sách', 6, 'published', 'contributor1'],
  ['Bản nháp: gợi ý xây dựng portfolio cá nhân', 3, 'draft', 'contributor1'],
  ['Bản nháp: câu chuyện phía sau một dự án nhóm', 0, 'draft', 'contributor2'],
  ['Bản nháp: hành trình tham gia câu lạc bộ nhiếp ảnh', 1, 'draft', 'contributor3'],
  ['Chờ duyệt: tuần lễ nghiên cứu khoa học sinh viên', 2, 'pending', 'contributor1'],
  ['Chờ duyệt: chuyên đề thiết kế hệ thống Spring Boot', 0, 'pending', 'contributor2'],
  ['Chờ duyệt: hoạt động giao lưu giữa các câu lạc bộ', 1, 'pending', 'contributor3'],
  ['Cần chỉnh sửa: kế hoạch cuộc thi sáng tạo ứng dụng', 4, 'changes', 'contributor1'],
  ['Cần chỉnh sửa: hướng dẫn đăng ký thực tập', 7, 'changes', 'contributor2'],
  ['Hẹn giờ: thông báo chương trình đào tạo kỹ năng thuyết trình', 0, 'scheduled', 'contributor2'],
  ['Hẹn giờ: lịch sinh hoạt câu lạc bộ thể thao', 5, 'scheduled', 'contributor3'],
  ['Lưu trữ: tổng kết triển lãm công nghệ của học kỳ trước', 2, 'archived', 'contributor1'],
  ['Lưu trữ: nhìn lại chiến dịch tình nguyện sinh viên', 6, 'archived', 'contributor3'],
  ['Đã phê duyệt: bản tin định hướng nghề nghiệp', 3, 'approved', 'admin'],
];

async function seedNotifications() {
  const availableArticles = (articles.length ? articles : records.NewsArticle).filter(a => a.newsStatus === 'PUBLISHED');
  for (const [key, user] of users) {
    if (key === 'inactive') continue;
    const existing = records.Notification.filter(n => n.recipientKey === key).length;
    for (let n = existing; n < 2; n++) {
      const article = availableArticles[(records.Notification.length + n) % Math.max(availableArticles.length, 1)];
      const data = await api('POST', '/api/v1/notifications', 'admin', {
        recipientId: user.accountId, ...(article ? { newsArticleId: article.newsArticleId } : {}),
        title: n ? 'Nhắc lịch trình diễn dự án' : 'Chào mừng bạn đến với NMS Demo',
        content: n ? `Nhóm chuẩn bị trình diễn các tính năng theo vai trò. Mã dữ liệu: ${runId}.` : `Xin chào ${user.accountName}. Đây là thông báo nội bộ giả lập cho tài khoản ${roleNames[user.role]}.`,
        isRead: 0,
      }, `Create unread notification for ${key}`);
      remember('Notification', data, { recipientKey: key });
      if (data && n === 1) {
        const updated = await api('PATCH', `/api/v1/notifications/${data.notificationId}/read?recipientId=${user.accountId}`, 'admin', undefined, `Mark demo notification as read for ${key}`);
        if (updated) Object.assign(records.Notification.at(-1), updated);
      }
    }
  }
}

async function completeNotifications() {
  beforeCounts = Object.fromEntries(previous.tables.map(t => [t.table, t.before]));
  apiSpec = await api('GET', '/v3/api-docs', null, undefined, 'Read API specification for notification completion');
  const admin = await login('admin', adminEmail, adminPassword);
  if (!admin?.accessToken || admin.accountRole !== 1) throw new Error('Admin login is unavailable.');
  for (const user of records.SystemAccount) {
    const key = user.demoKey || user.key;
    if (!key) throw new Error(`Missing demo identity for account ${user.accountId}.`);
    users.set(key, { ...user, role: user.accountRole, email: user.loginEmail });
  }
  for (const notification of records.Notification) {
    if (!notification.recipientKey) notification.recipientKey = [...users].find(([, user]) => user.accountId === notification.recipientId)?.[0];
  }
  console.log('Completing notifications with isRead=0; keeping the original invalid-request evidence.');
  await seedNotifications();
  for (const [key, user] of users) {
    if (key !== 'inactive') await api('GET', `/api/v1/notifications?recipientId=${user.accountId}`, 'admin', undefined, `Verify completed notifications for ${key}`);
  }
}

async function seed() {
  apiSpec = await api('GET', '/v3/api-docs', null, undefined, 'Read live API specification');
  const admin = await login('admin', adminEmail, adminPassword);
  if (!admin?.accessToken || admin.accountRole !== 1) throw new Error('Admin authentication is unavailable; private API inserts require an Admin.');
  beforeCounts = countTables();
  await api('GET', '/api/v1/users?size=100', 'admin', undefined, 'Existing user list');
  console.log('Stage 1/7: Creating demo accounts for all permitted roles');
  for (const [role, key, name, via] of people) {
    const email = `demo.${key}.${suffix}@example.com`;
    const accountBody = { accountName: `${name} (Demo)`, accountEmail: email, accountPassword: demoPassword, ...(via === 'register' ? {} : { accountRole: role }) };
    const data = await api('POST', via === 'register' ? '/api/v1/auth/register-reader' : '/api/v1/users', via === 'register' ? null : 'admin', accountBody, `Create ${key}`);
    if (!data?.accountId) continue;
    users.set(key, { ...data, email, role, key, publicRegistration: via === 'register' });
    remember('SystemAccount', data, { demoKey: key, loginEmail: email });
    await login(key, email, demoPassword);
  }
  if (users.has('reader2')) await api('PUT', `/api/v1/users/${users.get('reader2').accountId}`, 'admin', { accountName: 'Ngô Quốc An — Bạn đọc demo', accountEmail: users.get('reader2').email, accountRole: 5 }, 'Update new Reader without resetting password');
  if (users.has('inactive')) {
    const id = users.get('inactive').accountId;
    await api('DELETE', `/api/v1/users/${id}`, 'admin', undefined, 'Deactivate new demo account');
    await api('PATCH', `/api/v1/users/${id}/activate`, 'admin', undefined, 'Reactivate new demo account');
    await api('DELETE', `/api/v1/users/${id}`, 'admin', undefined, 'Leave inactive example for login popup');
    await api('POST', '/api/v1/auth/login', null, { accountEmail: users.get('inactive').email, accountPassword: demoPassword }, 'Expected inactive login rejection', [401]);
    users.get('inactive').isActive = false;
  }
  console.log('Stage 2/7: Creating categories, tags, settings and moderation dictionary');
  for (const [name, parent] of categoryDefs) {
    if (parent !== null && !categories[parent]) { categories.push(null); skip(`Category ${name}`, 'Parent category was not created.'); continue; }
    const data = await api('POST', '/api/v1/categories', 'admin', { categoryName: `Demo ${suffix} • ${name}`, categoryDescription: `Danh mục phục vụ trình diễn ${runId}: ${name}.`, parentCategoryId: parent === null ? null : categories[parent].categoryId, isActive: 1 }, `Create category ${name}`);
    categories.push(data); remember('Category', data);
  }
  for (const name of tagDefs) {
    const data = await api('POST', '/api/v1/tags', 'admin', { tagName: `demo-${suffix}-${name}`, note: `Nhãn nội dung ${name}, bộ demo ${runId}` }, `Create tag ${name}`);
    if (data) tags.push(data); remember('Tag', data);
  }
  for (const [key, value, description] of [['EVENT_NAME', 'Ngày hội trải nghiệm sinh viên', 'Tên sự kiện giả lập'], ['SUPPORT_CONTACT', 'demo-support@example.com', 'Địa chỉ hỗ trợ giả lập, không gửi thư'], ['SEMESTER', 'Fall 2026', 'Học kỳ trình diễn']]) {
    const body = { settingKey: `DEMO_${suffix}_${key}`, settingValue: value, description: `${description} — ${runId}` };
    const data = await api('POST', '/api/v1/settings', 'admin', body, `Create demo setting ${key}`); remember('SystemSetting', data);
    if (data && key === 'SEMESTER') await api('PUT', `/api/v1/settings/${data.settingId}`, 'admin', { ...body, description: `${body.description}; đã kiểm thử cập nhật qua API` }, 'Update only the new demo setting');
  }
  for (const [word, severity, active] of [['demoquangcao', 'FLAG', true], ['demospam', 'BLOCK', true], ['demolienket', 'FLAG', true], ['demotuluu', 'FLAG', false]]) {
    const data = await api('POST', '/api/v1/moderation/keywords', 'moderator1', { word: `${word}${suffix}`, severity, isActive: active }, `Create ${severity} moderation marker`); remember('BannedKeyword', data);
  }
  console.log('Stage 3/7: Creating articles and exercising editorial workflows');
  for (let i = 0; i < topics.length; i++) {
    const [title, cat, scenario, author] = topics[i];
    if (!categories[cat]) { skip(`Article ${title}`, 'Required category is unavailable.'); continue; }
    const body = {
      newsTitle: `${title} [Demo]`, headline: `Bản tin mô phỏng dành cho buổi demo: ${title.toLowerCase()}.`,
      newsContent: `DỮ LIỆU DEMO — ${runId}.\n\n${title}. Đây là nội dung giả lập để nhóm trình diễn hệ thống quản lý tin tức, không phải thông báo chính thức.\n\nChương trình hướng đến việc kết nối sinh viên, phát triển kỹ năng và chia sẻ kinh nghiệm học tập. Người tham gia có thể theo dõi thông tin, đặt câu hỏi và trao đổi trong phần bình luận.\n\nBan tổ chức mô phỏng giới thiệu hoạt động trải nghiệm, phiên thảo luận và cơ hội hợp tác theo nhóm. Nội dung này phục vụ kiểm thử tạo bài, phân loại, tìm kiếm, gắn tag và quy trình biên tập.`,
      newsSource: `NMS Demo • ${runId}`, categoryId: categories[cat].categoryId, newsStatus: scenario === 'approved' ? 'APPROVED' : 'DRAFT', allowComments: true,
      tagIds: tags.length ? [tags[i % tags.length].tagId, tags[(i + 3) % tags.length].tagId] : [],
    };
    // Exercise both article-creation controllers, with each contributor owning their own drafts.
    const createPath = i % 7 === 0 || scenario === 'approved' ? '/api/v1/news' : '/api/v1/author/articles';
    const data = await api('POST', createPath, author, body, `Create article ${i + 1}: ${scenario}`);
    if (!data?.newsArticleId) continue;
    const article = { ...data, scenario, actor: author, ordinal: i + 1 };
    articles.push(article); remember('NewsArticle', data, { scenario, demoActor: author });
    for (const tagId of data.tagIds || []) remember('NewsTag', { newsArticleId: data.newsArticleId, tagId });
    const id = data.newsArticleId;
    if (scenario === 'draft' || scenario === 'approved') continue;
    const submitted = await api('POST', `/api/v1/author/articles/${id}/submit`, author, undefined, `Submit article ${id} for review`);
    if (!submitted) { skip(`Editorial workflow for ${id}`, 'Article submission failed.'); continue; }
    const editor = i % 2 ? 'editor1' : 'editor2';
    let changed;
    if (scenario === 'pending') continue;
    if (scenario === 'changes') {
      await api('POST', `/api/v1/editorial/articles/${id}/request-changes`, editor, { feedbackContent: `Vui lòng bổ sung thời gian, đối tượng tham gia và thông tin liên hệ mô phỏng. Kiểm tra lại tiêu đề trước khi nộp duyệt lần tiếp theo. (${runId})` }, `Request changes for ${id}`);
    } else if (scenario === 'scheduled') {
      const scheduledPublishDate = new Date(Date.now() + (i % 2 + 1) * 86400000).toISOString().slice(0, 19);
      changed = await api('POST', `/api/v1/editorial/articles/${id}/schedule`, editor, { scheduledPublishDate }, `Schedule article ${id} in the future (UTC)`);
    } else {
      changed = await api('POST', `/api/v1/editorial/articles/${id}/publish-now`, editor, undefined, `Publish article ${id}`);
      if (scenario === 'archived' && changed) changed = await api('POST', `/api/v1/editorial/articles/${id}/archive`, editor, undefined, `Archive article ${id}`);
    }
    if (changed?.newsStatus) Object.assign(article, changed);
  }
  const draft = articles.find(a => a.scenario === 'draft');
  if (draft) await api('PUT', `/api/v1/author/articles/${draft.newsArticleId}`, draft.actor, { headline: 'Bản nháp đã được tác giả cập nhật qua API trong bộ demo.', tagIds: draft.tagIds }, 'Update an owned demo draft');
  const archived = articles.find(a => a.newsStatus === 'ARCHIVED');
  if (archived) {
    const restored = await api('POST', `/api/v1/editorial/articles/${archived.newsArticleId}/restore`, 'editor1', undefined, 'Exercise restore on a new archived article');
    if (restored) await api('POST', `/api/v1/editorial/articles/${archived.newsArticleId}/archive`, 'editor1', undefined, 'Return demo article to archive');
  }
  const linkArticle = articles[0];
  if (linkArticle && tags.length > 1) {
    const missingTag = tags.find(t => !linkArticle.tagIds?.includes(t.tagId));
    if (missingTag) remember('NewsTag', await api('POST', '/api/news-tags', 'admin', { newsArticleId: linkArticle.newsArticleId, tagId: missingTag.tagId }, 'Create article/tag link through legacy controller'));
  }
  console.log('Stage 4/7: Creating comments, replies, reports and moderation cases');
  const published = articles.filter(a => a.newsStatus === 'PUBLISHED');
  const commentTexts = [
    'Bài viết rất hữu ích, mình sẽ chia sẻ với nhóm học tập.', 'Mình mong có thêm hướng dẫn đăng ký chi tiết cho sinh viên mới.',
    'Nội dung trình bày rõ ràng, cảm ơn ban biên tập.', 'Mình quan tâm đến hoạt động này và muốn tham gia cùng các bạn.',
    'Có thể bổ sung phần hỏi đáp để mọi người dễ theo dõi không?', 'Hy vọng chương trình sẽ có thêm nhiều hoạt động thực hành.',
  ];
  for (let i = 0; i < Math.min(24, published.length * 3); i++) {
    const article = published[i % published.length];
    const actor = ['reader1', 'reader2', 'reader3', 'reader4'][i % 4];
    const flagged = i === 20;
    const content = `${commentTexts[i % commentTexts.length]} (${runId}, bình luận ${i + 1})${flagged ? ` demoquangcao${suffix}` : ''}`;
    const data = await api('POST', '/api/v1/comments', actor, { newsArticleId: article.newsArticleId, content }, `Create comment ${i + 1}${flagged ? ' with FLAG marker' : ''}`);
    if (!data?.commentId) continue;
    const comment = { ...data, actor }; comments.push(comment); remember('Comment', data);
    if (i < 14 && data.status !== 'APPROVED') {
      const approved = await api('PATCH', `/api/v1/moderation/comments/${data.commentId}/approve`, 'moderator1', undefined, `Approve demo comment ${data.commentId}`);
      if (approved) comment.status = 'APPROVED';
    }
    if (i === 15) { const rejected = await api('PATCH', `/api/v1/moderation/comments/${data.commentId}/reject`, 'moderator2', undefined, 'Leave rejected comment example'); if (rejected) comment.status = 'REJECTED'; }
  }
  const approvedComments = comments.filter(c => c.status === 'APPROVED');
  for (const [i, parent] of approvedComments.slice(0, 8).entries()) {
    const data = await api('POST', '/api/v1/comments', 'reader2', { newsArticleId: parent.newsArticleId, parentCommentId: parent.commentId, content: `Mình đồng ý với bạn. Nhóm sẽ cập nhật thêm thông tin trong bản tin tiếp theo. (${runId}, trả lời ${i + 1})` }, `Reply to comment ${parent.commentId}`);
    remember('Comment', data);
    if (data?.status !== 'APPROVED' && data?.commentId) await api('PATCH', `/api/v1/moderation/comments/${data.commentId}/approve`, 'moderator1', undefined, 'Approve a demo reply');
  }
  for (const [i, comment] of approvedComments.slice(0, 8).entries()) {
    const reporter = comment.actor === 'reader1' ? 'reader2' : 'reader1';
    const result = await api('POST', `/api/v1/comments/${comment.commentId}/report`, reporter, { reason: ['SPAM', 'MISINFORMATION', 'OTHER'][i % 3], note: `Tình huống báo cáo giả lập ${i + 1} trong ${runId}; không phản ánh vi phạm thực tế.` }, `Report demo comment ${comment.commentId}`);
    if (result) remember('CommentReport', { commentId: comment.commentId, reporter: users.get(reporter)?.accountId });
    if (result && i === 0) await api('PATCH', `/api/v1/moderation/comments/${comment.commentId}/keep`, 'moderator1', undefined, 'Dismiss a demo report and keep its comment');
    if (result && i === 1) await api('PATCH', `/api/v1/moderation/comments/${comment.commentId}/reject`, 'moderator2', undefined, 'Resolve a demo report by rejecting its comment');
  }
  console.log('Stage 5/7: Creating engagement and notifications');
  for (const [i, article] of published.entries()) {
    for (const actor of ['reader1', 'reader2', 'reader3', 'reader4'].slice(0, i % 4 + 1)) {
      const result = await api('POST', `/api/v1/news/${article.newsArticleId}/reactions`, actor, undefined, `Like article ${article.newsArticleId} as ${actor}`);
      if (result) remember('ArticleReaction', { newsArticleId: article.newsArticleId, actor, ...result });
    }
    for (let n = 0; n < i % 6 + 2; n++) await api('POST', `/api/v1/news/${article.newsArticleId}/views`, null, undefined, `View demo article ${article.newsArticleId}`);
  }
  await seedNotifications();
  const note = records.Notification.find(n => n.recipientKey === 'reader1');
  if (note) await api('PATCH', `/api/v1/notifications/${note.notificationId}/read?recipientId=${users.get('reader1').accountId}`, 'reader1', undefined, 'Reader marks a new notification as read');
  console.log('Stage 6/7: Reading persisted records and exercising query APIs');
  for (const [uri, actor, label] of [
    ['/api/v1/users?size=100&keyword=demo.', 'admin', 'Find demo accounts'], ['/api/v1/users/me', 'reader1', 'Read current profile'], ['/api/v1/auth/me', 'editor1', 'Read current editor'],
    ['/api/v1/categories', null, 'Read categories'], ['/api/v1/categories/tree', null, 'Read category tree'], ['/api/v1/tags', null, 'Read tags'], ['/api/v1/tags/search?keyword=demo', 'admin', 'Search demo tags'],
    ['/api/v1/settings', 'admin', 'Read settings'], ['/api/v1/moderation/settings', 'moderator1', 'Read current moderation mode without changing it'], ['/api/v1/moderation/keywords', 'moderator1', 'Read moderation dictionary'],
    ['/api/v1/moderation/comments/pending?size=100', 'moderator1', 'Read moderation queue'], ['/api/v1/moderation/reports?status=PENDING&size=100', 'moderator1', 'Read pending reports'],
    ['/api/v1/moderation/reports?status=RESOLVED&size=100', 'moderator1', 'Read resolved reports'], ['/api/v1/moderation/reports?status=DISMISSED&size=100', 'moderator1', 'Read dismissed reports'],
    ['/api/v1/news', 'admin', 'Read internal news'], ['/api/v1/news/search?keyword=Demo', 'admin', 'Search demo news'], ['/api/v1/news/public', null, 'Read public news'], ['/api/v1/portal/news', null, 'Read legacy portal news'], ['/api/v1/portal/articles?size=100', null, 'Read paginated portal'],
    ['/api/v1/author/articles?size=100', 'contributor1', 'Read contributor drafts and articles'], ['/api/v1/editorial/queue?size=100', 'editor1', 'Read editorial queue'],
    ['/api/v1/dashboard/overview', 'admin', 'Read admin dashboard'], ['/api/v1/dashboard/overview', 'contributor1', 'Read contributor dashboard'], ['/api/v1/dashboard/news', 'editor1', 'Read dashboard news'], ['/api/v1/dashboard/engagement', 'editor1', 'Read engagement dashboard'],
    ['/api/news-tags', 'admin', 'Read article-tag links omitted from versioned Swagger'],
  ]) await api('GET', uri, actor, undefined, label);
  for (const article of articles) {
    const detail = await api('GET', `/api/v1/news/${article.newsArticleId}`, 'admin', undefined, `Verify saved article ${article.newsArticleId}`);
    if (detail) Object.assign(article, detail);
    const audit = await api('GET', `/api/v1/author/articles/${article.newsArticleId}/audit-trail`, article.actor, undefined, `Read generated audit trail ${article.newsArticleId}`);
    if (Array.isArray(audit)) for (const row of audit) if (!records.ArticleAuditLog.some(r => r.logId === row.logId)) remember('ArticleAuditLog', row);
    if (article.scenario === 'changes') {
      const feedback = await api('GET', `/api/v1/author/articles/${article.newsArticleId}/feedback`, article.actor, undefined, `Read generated review notes ${article.newsArticleId}`);
      if (Array.isArray(feedback)) for (const row of feedback) remember('ArticleReviewNote', row);
    }
  }
  const firstPublished = articles.find(a => a.newsStatus === 'PUBLISHED');
  if (firstPublished) {
    const id = firstPublished.newsArticleId;
    for (const uri of [`/api/v1/portal/articles/${id}`, `/api/v1/portal/news/${id}`, `/api/v1/news/public/${id}`, `/api/v1/comments/article/${id}`, `/api/news-tags/article/${id}`]) await api('GET', uri, uri.startsWith('/api/news-tags') ? 'admin' : null, undefined, 'Read demo public detail and related data');
    await api('GET', `/api/v1/news/category/${firstPublished.categoryId}`, 'admin', undefined, 'Filter by demo category');
  }
  if (tags[0]) {
    await api('GET', `/api/v1/tags/${tags[0].tagId}`, null, undefined, 'Read individual tag');
    await api('GET', `/api/v1/news/tag/${tags[0].tagId}`, 'admin', undefined, 'Filter articles by demo tag');
    await api('GET', `/api/news-tags/tag/${tags[0].tagId}`, 'admin', undefined, 'Read associations by tag');
  }
  if (categories[0]) await api('GET', `/api/v1/categories/${categories[0].categoryId}`, null, undefined, 'Read individual category');
  if (records.SystemSetting[0]) {
    const setting = records.SystemSetting[0];
    await api('GET', `/api/v1/settings/${setting.settingId}`, 'admin', undefined, 'Read individual demo setting');
    await api('GET', `/api/v1/settings/key/${setting.settingKey}`, 'admin', undefined, 'Read demo setting by key');
  }
  for (const key of ['contributor1', 'editor1', 'moderator1', 'reader1']) {
    const user = users.get(key); if (!user) continue;
    for (const uri of [`/api/v1/notifications?recipientId=${user.accountId}`, `/api/v1/notifications/unread-count?recipientId=${user.accountId}`, ...(user.role !== 5 ? [`/api/v1/dashboard/notifications?recipientId=${user.accountId}`] : [])]) await api('GET', uri, key, undefined, `Read notifications for ${key}`);
  }
  console.log('Stage 7/7: Recording final states and read-only table counts');
  records.NewsArticle = articles.map(({ actor, scenario, ordinal, ...a }) => ({ ...a, scenario, demoActor: actor, ordinal }));
  for (const [key, user] of users) {
    const current = await api('GET', `/api/v1/users/${user.accountId}`, 'admin', undefined, `Verify final account ${key}`);
    if (current) Object.assign(user, current);
  }
  records.SystemAccount = [...users.values()].map(({ email, role, ...u }) => ({ ...u, demoKey: u.key, loginEmail: email, roleName: roleNames[role] }));
}

function renderReport(fatal = null) {
  if (finished) return; finished = true;
  const afterCounts = countTables();
  const tables = Object.keys(records).map(table => ({ table, before: beforeCounts?.[table] ?? null, after: afterCounts?.[table] ?? null, inserted: beforeCounts && afterCounts ? afterCounts[table] - beforeCounts[table] : null, observedDemoRecords: records[table].length }));
  const methods = new Set(['get', 'post', 'put', 'patch', 'delete']);
  const covered = new Set(log.filter(r => r.method).map(r => endpointKey(r.method, r.path)));
  const inventory = Object.entries(apiSpec?.paths || {}).flatMap(([uri, ops]) => Object.keys(ops).filter(m => methods.has(m)).map(m => ({ method: m.toUpperCase(), path: uri, covered: covered.has(endpointKey(m.toUpperCase(), uri)) })));
  const summary = { runId, baseUrl: base, generatedAt: new Date().toISOString(), fatal, requests: log.filter(r => r.method).length, passed: log.filter(r => r.outcome === 'PASS').length, failures: failures.length, backendFailures: failures.filter(f => f.failureCategory !== 'SEED_INPUT').length, seedInputFailures: failures.filter(f => f.failureCategory === 'SEED_INPUT').length, skipped: skips.length, tables, checks, failedEndpoints: failures, inventory, records };
  writeFileSync(path.join(out, 'summary.json'), JSON.stringify(summary, null, 2), 'utf8');
  const cell = v => String(v ?? '—').replace(/\|/g, '\\|').replace(/\n/g, ' ');
  const lines = [`# Dữ liệu demo NMS — ${runId}`, '', `Đích API: ${base}. Mọi ghi dữ liệu đều thực hiện qua HTTP API. SQL chỉ dùng SELECT để đối chiếu số dòng trước/sau.`, '',
    `Kết quả: ${summary.requests} request; ${summary.passed} đạt kỳ vọng; ${failures.length} lỗi; ${skips.length} bước bỏ qua.`, ...(fatal ? ['', `Lỗi chặn: ${fatal}`] : []), '',
    '## Tài khoản dùng để demo', '', `Mật khẩu chung của các tài khoản demo mới: \`${demoPassword}\`. Admin dùng tài khoản quản trị có sẵn, không tạo thêm Admin.`, '',
    '| Vai trò | Email | Xác minh | Trạng thái |', '| --- | --- | --- | --- |',
    ...records.SystemAccount.map(u => `| ${cell(u.roleName)} | ${cell(u.loginEmail)} | ${u.isVerified ? 'Verified' : 'Unverified'} | ${u.isActive ? 'Active' : 'Inactive'} |`), '',
    '## Đối chiếu tất cả bảng', '', '| Bảng | Trước | Sau | Số dòng tăng | Bản ghi demo quan sát qua API |', '| --- | ---: | ---: | ---: | ---: |',
    ...tables.map(t => `| ${t.table} | ${cell(t.before)} | ${cell(t.after)} | ${cell(t.inserted)} | ${t.observedDemoRecords} |`), '',
    '## Kịch bản demo', '',
    '- Admin: quản lý user, xác minh Reader, Deactivate/Activate tài khoản, danh mục, tag, cấu hình và thông báo.',
    '- Contributor: mở các bài DRAFT hoặc CHANGES_REQUESTED, xem góp ý, sửa nội dung và gửi duyệt.',
    '- Editor: mở hàng đợi PENDING_REVIEW; yêu cầu chỉnh sửa, xuất bản hoặc hẹn giờ; xem lịch sử và bài lưu trữ.',
    '- Moderator: xem bình luận đang chờ, báo cáo PENDING/RESOLVED/DISMISSED, từ khóa FLAG/BLOCK và duyệt/từ chối bình luận.',
    '- Reader: đọc tin công khai, thích bài, bình luận, trả lời và báo cáo. Tài khoản reader3/reader4 chưa xác minh dùng để demo tiền kiểm.',
    '- Tài khoản inactive dùng để demo popup bị vô hiệu hóa. Tất cả nội dung được đánh dấu Demo và là thông tin mô phỏng.', '',
    'Không sửa/xóa dữ liệu cũ; không gửi email OTP; không đổi cấu hình kiểm duyệt toàn hệ thống. Không thực hiện các API DELETE xóa hẳn bản ghi. Bảng NewsTag, ArticleAuditLog và ArticleReviewNote được tạo qua liên kết/quy trình nghiệp vụ.', '',
    '## API lỗi cần xử lý', '', `Lỗi backend ghi nhận: ${summary.backendFailures}. Lỗi đầu vào của script: ${summary.seedInputFailures}.`, '', ...(failures.length ? ['| Method | Endpoint thực tế | HTTP | Phân loại | Tình huống |', '| --- | --- | ---: | --- | --- |', ...failures.map(f => `| ${f.method} | ${cell(f.path)} | ${f.status} | ${f.failureCategory === 'SEED_INPUT' ? 'Đầu vào script đã sửa' : 'Cần kiểm tra'} | ${cell(f.label)} |`)] : ['Không ghi nhận API lỗi trong các request đã chạy.']), '',
    ...failures.flatMap((f, i) => [`### Lỗi ${i + 1}: ${f.method} ${f.path}`, '', `Actor: ${f.actor}. HTTP ${f.status}.`, ...(f.explanation ? ['', f.explanation] : []), '', 'Request:', '```json', JSON.stringify(f.request ?? null, null, 2), '```', '', 'Response:', '```json', JSON.stringify(f.response, null, 2), '```', '']),
    '## Phạm vi kiểm thử', '', `Nhật ký chi tiết: [api-log.jsonl](api-log.jsonl). ID và trạng thái: [summary.json](summary.json). Token và mật khẩu request đã được che trong nhật ký.`, '',
    'API không gọi:', '', ...inventory.filter(r => !r.covered).map(r => `- \`${r.method} ${r.path}\``), '',
    'API không gọi không được kết luận là lỗi. Các luồng email OTP, đổi mật khẩu cá nhân, xóa hẳn dữ liệu và sửa thiết lập toàn cục được để lại cho demo chủ động. Khi một endpoint lỗi, các lần gọi cùng endpoint sau đó bị bỏ qua; không tự sửa backend và không thay bằng INSERT SQL.', '',
    '## Chạy lại với một bộ mới', '', 'Tại thư mục backend, đặt DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD, tùy chọn DEMO_PASSWORD và DEMO_RUN_ID rồi chạy:', '', '```powershell', 'node ./scripts/Seed-Demo-Api.mjs', '```', '', 'Một run ID chỉ được chạy một lần để tránh tạo trùng ngoài ý muốn. Không có thao tác dọn/xóa tự động.', '',
  ];
  writeFileSync(path.join(out, 'README.md'), lines.join('\n'), 'utf8');
  console.log(JSON.stringify({ runId, report: path.join(out, 'README.md'), requests: summary.requests, passed: summary.passed, errors: failures.length, skipped: skips.length, tableDeltas: tables.map(t => [t.table, t.inserted]), fatal }, null, 2));
}

try { if (completeNotificationsOnly) await completeNotifications(); else if (!reportOnly) await seed(); renderReport(); }
catch (error) { renderReport(error.message); process.exitCode = 1; }
