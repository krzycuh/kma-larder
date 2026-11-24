import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';
import { LoadingSpinner } from './components/LoadingSpinner';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { UserRole } from './types/auth';
import { AppLayout } from './components/AppLayout';
import './App.css';

// Lazy load pages for better performance
const Dashboard = lazy(() => import('./pages/Dashboard').then((m) => ({ default: m.Dashboard })));
const Profile = lazy(() => import('./pages/Profile').then((m) => ({ default: m.Profile })));
const PantryMembers = lazy(() => import('./pages/PantryMembers').then((m) => ({ default: m.PantryMembers })));
const Stock = lazy(() => import('./pages/Stock').then((m) => ({ default: m.Stock })));
const AddStock = lazy(() => import('./pages/AddStock').then((m) => ({ default: m.AddStock })));
const ProductDetail = lazy(() => import('./pages/ProductDetail').then((m) => ({ default: m.ProductDetail })));
const ProductForm = lazy(() => import('./pages/ProductForm').then((m) => ({ default: m.ProductForm })));
const BatchForm = lazy(() => import('./pages/BatchForm').then((m) => ({ default: m.BatchForm })));
const Expiring = lazy(() => import('./pages/Expiring').then((m) => ({ default: m.Expiring })));
const Replenish = lazy(() => import('./pages/Replenish').then((m) => ({ default: m.Replenish })));
const AdminUsers = lazy(() => import('./pages/admin/Users').then((m) => ({ default: m.AdminUsers })));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AuthProvider>
            <Toaster
              position="top-right"
              toastOptions={{
                duration: 4000,
                style: { background: '#29251f', color: '#faf6f0' },
                success: { duration: 3000, iconTheme: { primary: '#659631', secondary: '#fff' } },
                error: { duration: 5000, iconTheme: { primary: '#ef4444', secondary: '#fff' } },
              }}
            />
            <Suspense fallback={<LoadingSpinner fullScreen />}>
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route
                  element={
                    <ProtectedRoute>
                      <AppLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/stock" element={<Stock />} />
                  <Route path="/add" element={<AddStock />} />
                  <Route path="/products/:id" element={<ProductDetail />} />
                  <Route path="/products/:id/edit" element={<ProductForm />} />
                  <Route path="/batches/:id/edit" element={<BatchForm />} />
                  <Route path="/expiring" element={<Expiring />} />
                  <Route path="/replenish" element={<Replenish />} />
                  <Route path="/members" element={<PantryMembers />} />
                  <Route path="/profile" element={<Profile />} />
                  <Route
                    path="/admin/users"
                    element={
                      <ProtectedRoute requireRole={UserRole.ADMIN}>
                        <AdminUsers />
                      </ProtectedRoute>
                    }
                  />
                </Route>
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
