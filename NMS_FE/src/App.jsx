import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import RequireAdmin from './components/RequireAdmin';
import Login from './pages/auth/Login';
import Dashboard from './pages/dashboard/Dashboard';
import CategoryManager from './pages/categories/CategoryManager';
import NewsManager from './pages/news/NewsManager';
import UserManager from './pages/users/UserManager';
import Settings from './pages/setting/Settings';

// Public Portal
import PortalLayout from './components/PortalLayout';
import PortalHome from './pages/portal/PortalHome';
import ArticleDetail from './pages/portal/ArticleDetail';

function App() {
  return (
    <Routes>

      <Route path="/portal" element={<PortalLayout />}>
        <Route index element={<PortalHome />} />
        <Route path="articles/:id" element={<ArticleDetail />} />
      </Route>

      <Route path="/login" element={<Login />} />

      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="category" element={<CategoryManager />} />
        <Route path="categories" element={<Navigate to="/category" replace />} />
        <Route path="news" element={<NewsManager />} />
        <Route path="users" element={<RequireAdmin><UserManager /></RequireAdmin>} />
        <Route path="settings" element={<Settings />} />
      </Route>

      <Route path="*" element={<Navigate to="/portal" replace />} />
    </Routes>
  );
}

export default App;
