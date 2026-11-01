import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAdmin from './components/RequireAdmin'
import Login from './pages/auth/Login'
import Dashboard from './pages/dashboard/Dashboard'
import CategoryManager from './pages/categories/CategoryManager'
import NewsManager from './pages/news/NewsManager'
import UserManager from './pages/users/UserManager'
import Settings from './pages/setting/Settings'
import RequireRoles from './components/RequireRoles'
import Register from './pages/auth/Register'
import Profile from './pages/auth/Profile'
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
      <Route path="/register" element={<Register />} />
      <Route path="/account" element={<Profile />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="category" element={<RequireRoles roles={[1, 2, 3]}><CategoryManager /></RequireRoles>} />
        <Route path="news" element={<RequireRoles roles={[1, 2, 3]}><NewsManager /></RequireRoles>} />
        <Route path="users" element={<RequireAdmin><UserManager /></RequireAdmin>} />
        <Route path="settings" element={<RequireAdmin><Settings /></RequireAdmin>} />
      </Route>

      <Route path="*" element={<Navigate to="/portal" replace />} />
    </Routes>
  );
}

export default App;
