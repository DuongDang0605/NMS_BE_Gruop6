import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAdmin from './components/RequireAdmin'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import CategoryManager from './pages/CategoryManager'
import NewsManager from './pages/NewsManager'
import UserManager from './pages/UserManager'
import Settings from './pages/Settings'

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
