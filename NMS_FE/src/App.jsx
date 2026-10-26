import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAdmin from './components/RequireAdmin'
import Login from './pages/auth/Login'
import Dashboard from './pages/dashboard/Dashboard'
import CategoryManager from './pages/categories/CategoryManager'
import NewsManager from './pages/news/NewsManager'
import UserManager from './pages/users/UserManager'
import Settings from './pages/setting/Settings'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="category" element={<CategoryManager />} />
        <Route path="news" element={<NewsManager />} />
        <Route path="users" element={<RequireAdmin><UserManager /></RequireAdmin>} />
        <Route path="settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}

export default App
