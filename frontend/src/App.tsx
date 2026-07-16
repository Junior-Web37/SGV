import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { AuthProvider, useAuth } from '@/lib/auth'
import { Layout } from '@/components/Layout'
import { LoginPage } from '@/pages/LoginPage'
import { SetupWizardPage } from '@/pages/SetupWizardPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { PosPage } from '@/pages/PosPage'
import { SalesListPage } from '@/pages/SalesListPage'
import { ReconciliationPage } from '@/pages/ReconciliationPage'
import { ReportsPage } from '@/pages/ReportsPage'
import { AuditPage } from '@/pages/AuditPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { ProfilesPage } from '@/pages/ProfilesPage'
import { UsersPage } from '@/pages/UsersPage'
import { ProductsPage } from '@/pages/ProductsPage'
import { CustomersPage } from '@/pages/CustomersPage'
import { WarehousePage } from '@/pages/WarehousePage'
import { Loader2 } from 'lucide-react'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) {
    return <div className="flex items-center justify-center min-h-screen"><Loader2 className="h-8 w-8 animate-spin" /></div>
  }
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />
  return <>{children}</>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/setup" element={<SetupWizardPage />} />
      <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
        <Route index element={<DashboardPage />} />
        <Route path="pos" element={<PosPage />} />
        <Route path="sales" element={<SalesListPage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="warehouse" element={<WarehousePage />} />
        <Route path="customers" element={<CustomersPage />} />
        <Route path="reconciliation" element={<ReconciliationPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="audit" element={<AuditPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="settings/profiles" element={<ProfilesPage />} />
        <Route path="settings/users" element={<UsersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}