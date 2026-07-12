import { Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout.jsx';
import { ProtectedRoute } from './components/ProtectedRoute.jsx';
import { AccountPage } from './pages/AccountPage.jsx';
import { AdminCatalogPage } from './pages/AdminCatalogPage.jsx';
import { AdminDashboardPage } from './pages/AdminDashboardPage.jsx';
import { AdminEditorPage } from './pages/AdminEditorPage.jsx';
import { AdminUsersPage } from './pages/AdminUsersPage.jsx';
import { CatalogPage } from './pages/CatalogPage.jsx';
import { ComparePage } from './pages/ComparePage.jsx';
import { DetailPage } from './pages/DetailPage.jsx';
import { ForbiddenPage, NotFoundPage } from './pages/ErrorPages.jsx';
import { LandingPage } from './pages/LandingPage.jsx';
import { LoginPage } from './pages/LoginPage.jsx';
import { RegisterPage } from './pages/RegisterPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<LandingPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forbidden" element={<ForbiddenPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="catalog" element={<CatalogPage />} />
          <Route path="heat-exchangers/:slug" element={<DetailPage />} />
          <Route path="compare" element={<ComparePage />} />
          <Route path="account" element={<AccountPage />} />
        </Route>

        <Route element={<ProtectedRoute admin />}>
          <Route path="admin" element={<AdminDashboardPage />} />
          <Route path="admin/catalog" element={<AdminCatalogPage />} />
          <Route path="admin/catalog/new" element={<AdminEditorPage />} />
          <Route path="admin/catalog/:id" element={<AdminEditorPage />} />
          <Route path="admin/users" element={<AdminUsersPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
