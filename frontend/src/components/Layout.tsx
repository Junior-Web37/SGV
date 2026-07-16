import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '@/lib/auth'
import { Button } from '@/components/ui/button'
import { Receipt, LayoutDashboard, ShoppingCart, FileText, Calculator, BarChart3, Settings, History, LogOut, Menu, Package, Users } from 'lucide-react'
import { cn } from '@/lib/utils'

const nav = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'GESTOR'] },
  { to: '/pos', label: 'PDV', icon: ShoppingCart, roles: ['ADMIN', 'GESTOR', 'CAIXA'] },
  { to: '/sales', label: 'Facturas', icon: FileText, roles: ['ADMIN', 'GESTOR', 'CAIXA'] },
  { to: '/products', label: 'Produtos', icon: Package, roles: ['ADMIN', 'GESTOR', 'CAIXA'] },
  { to: '/warehouse', label: 'Armazém', icon: Package, roles: ['ADMIN', 'GESTOR'] },
  { to: '/customers', label: 'Clientes', icon: Users, roles: ['ADMIN', 'GESTOR', 'CAIXA'] },
  { to: '/reconciliation', label: 'Reconciliação', icon: Calculator, roles: ['ADMIN', 'GESTOR'] },
  { to: '/reports', label: 'Relatórios', icon: BarChart3, roles: ['ADMIN', 'GESTOR'] },
  { to: '/audit', label: 'Auditoria', icon: History, roles: ['ADMIN'] },
  { to: '/settings', label: 'Configurações', icon: Settings, roles: ['ADMIN'] },
  { to: '/settings/profiles', label: 'Perfis', icon: Users, roles: ['ADMIN'] },
  { to: '/settings/users', label: 'Utilizadores', icon: Users, roles: ['ADMIN'] },
]

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const userRoles = user?.roles ?? []
  const primaryRole = userRoles[0] || 'CAIXA'
  const items = nav.filter((n) => !user || n.roles.includes(primaryRole))

  return (
    <div className="min-h-screen bg-muted/30">
      {/* Mobile top bar */}
      <header className="md:hidden bg-primary text-primary-foreground sticky top-0 z-30 flex items-center px-4 h-14">
        <Button variant="ghost" size="icon" onClick={() => setSidebarOpen(!sidebarOpen)} className="text-primary-foreground hover:bg-primary/80">
          <Menu className="h-5 w-5" />
        </Button>
        <Receipt className="h-5 w-5 ml-2" />
        <span className="font-semibold ml-2">SGV</span>
      </header>

      <div className="flex">
        {/* Sidebar */}
        <aside className={cn(
          "bg-primary text-primary-foreground w-64 min-h-screen p-4 flex flex-col fixed md:static z-20 transition-transform",
          sidebarOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        )}>
          <div className="flex items-center gap-2 px-2 mb-6">
            <Receipt className="h-6 w-6" />
            <div>
              <div className="font-bold text-lg leading-tight">SGV</div>
              <div className="text-xs opacity-80">Facturação AT</div>
            </div>
          </div>

          <nav className="space-y-1 flex-1">
            {items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) => cn(
                  "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                  isActive ? "bg-white/15" : "hover:bg-white/10"
                )}
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="border-t border-white/10 pt-3 mt-3">
            <div className="px-3 mb-2 text-sm">
              <div className="font-medium">{user?.fullName || user?.username}</div>
              <div className="text-xs opacity-70">{userRoles.join(', ')}</div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="w-full justify-start text-primary-foreground hover:bg-white/10"
              onClick={() => { logout(); navigate('/login') }}
            >
              <LogOut className="h-4 w-4 mr-2" />
              Sair
            </Button>
          </div>
        </aside>

        {sidebarOpen && (
          <div
            className="fixed inset-0 bg-black/50 z-10 md:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        )}

        {/* Main content */}
        <main className="flex-1 min-w-0">
          <div className="max-w-7xl mx-auto p-4 md:p-8">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}