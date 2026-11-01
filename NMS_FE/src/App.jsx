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
import Portal from './pages/portal/Portal'
import Profile from './pages/auth/Profile'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/portal" element={<Portal />} />
      <Route path="/account" element={<Profile />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="category" element={<RequireRoles roles={[1, 2, 3]}><CategoryManager /></RequireRoles>} />
        <Route path="news" element={<RequireRoles roles={[1, 2, 3]}><NewsManager /></RequireRoles>} />
        <Route path="users" element={<RequireAdmin><UserManager /></RequireAdmin>} />
        <Route path="settings" element={<RequireAdmin><Settings /></RequireAdmin>} />
      </Route>
    </Routes>
  )
}

export default App
