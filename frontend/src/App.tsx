import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

// Layout
import Layout from './components/Layout';

// Seiten
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import IdeasPage from './pages/IdeasPage';
import IdeaDetailPage from './pages/IdeaDetailPage';
import CreateIdeaPage from './pages/CreateIdeaPage';
import SurveysPage from './pages/SurveysPage';
import ProfilePage from './pages/ProfilePage';
import AdminPage from './pages/AdminPage';
import MessagesPage from './pages/MessagesPage';

// Geschützte Routen-Komponente
function ProtectedRoute({ children, roles }: { children: React.ReactNode; roles?: string[] }) {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}

// Öffentliche Route - leitet zum Dashboard weiter, wenn bereits angemeldet
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      {/* Öffentliche Routen */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicRoute>
            <RegisterPage />
          </PublicRoute>
        }
      />

      {/* Geschützte Routen */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/login" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="ideas" element={<IdeasPage />} />
        <Route path="ideas/new" element={<CreateIdeaPage />} />
        <Route path="ideas/:id" element={<IdeaDetailPage />} />
        <Route path="ideas/:id/edit" element={<CreateIdeaPage />} />
        <Route path="surveys" element={<SurveysPage />} />
        <Route path="messages" element={<MessagesPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route
          path="admin/*"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
