import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { LoadingScreen } from './LoadingScreen.jsx';

export function ProtectedRoute({ admin = false }) {
  const auth = useAuth();
  const location = useLocation();

  if (auth.status === 'loading') return <LoadingScreen />;
  if (!auth.isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;
  if (admin && !auth.isAdmin) return <Navigate to="/forbidden" replace />;
  return <Outlet />;
}
