import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '@/lib/api'
import type { ComplianceSnapshot, TodayReport } from '@/lib/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { Alert, AlertDescription, AlertIcon } from '@/components/ui/alert'
import {
  Receipt, Clock, AlertTriangle, CircleDollarSign, TrendingUp, FileText,
  Settings, ChevronRight, Activity
} from 'lucide-react'
import { formatMZN, formatDateTime } from '@/lib/utils'

export function DashboardPage() {
  const { data: compliance, isLoading } = useQuery({
    queryKey: ['compliance'],
    queryFn: async () => (await api.get<ComplianceSnapshot>('/dashboard/compliance')).data,
    refetchInterval: 30000,
  })

  const { data: today } = useQuery({
    queryKey: ['reports-today'],
    queryFn: async () => (await api.get<TodayReport>('/reports/today')).data,
  })

  if (isLoading) return <div className="text-center py-12">A carregar...</div>
  if (!compliance) return null

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Dashboard</h1>
          <p className="text-muted-foreground">Conformidade AT e visão do dia</p>
        </div>
        <Link to="/pos" className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Receipt className="h-4 w-4" /> Nova Venda
        </Link>
      </div>

      {/* Alertas críticos */}
      {compliance.alerts.length > 0 && (
        <div className="space-y-2">
          {compliance.alerts.map((a, i) => (
            <Alert key={i} variant={a.level === 'critical' ? 'destructive' : a.level === 'warning' ? 'warning' : 'default'}>
              <AlertIcon variant={a.level === 'critical' ? 'destructive' : a.level === 'warning' ? 'warning' : 'default'} />
              <AlertDescription>{a.message}</AlertDescription>
            </Alert>
          ))}
        </div>
      )}

      {/* KPIs */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Vendas Hoje</CardTitle>
            <CircleDollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatMZN(today?.totalAmount)}</div>
            <p className="text-xs text-muted-foreground">{today?.totalSales || 0} facturas</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">IVA Recolhido</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatMZN(today?.totalTax)}</div>
            <p className="text-xs text-muted-foreground">a entregar à AT</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ticket Médio</CardTitle>
            <Receipt className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatMZN(today?.averageTicket)}</div>
            <p className="text-xs text-muted-foreground">por factura emitida</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Última Venda</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {compliance.lastSale.minutesAgo < 0 ? '—' : compliance.lastSale.minutesAgo < 60 ? `${compliance.lastSale.minutesAgo}min` : `${Math.floor(compliance.lastSale.minutesAgo / 60)}h`}
            </div>
            <p className="text-xs text-muted-foreground truncate">
              {compliance.lastSale.documentType ? `${compliance.lastSale.documentType} ${compliance.lastSale.series}/${compliance.lastSale.documentNumber}` : compliance.lastSale.hint}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Acções rápidas */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Link to="/pos">
          <Card className="hover:shadow-md transition-shadow cursor-pointer">
            <CardHeader className="flex flex-row items-center gap-4">
              <Receipt className="h-8 w-8 text-primary" />
              <div>
                <CardTitle className="text-lg">Emitir Factura</CardTitle>
                <CardDescription>Ponto de venda</CardDescription>
              </div>
              <ChevronRight className="ml-auto h-5 w-5 text-muted-foreground" />
            </CardHeader>
          </Card>
        </Link>

        <Link to="/reconciliation">
          <Card className="hover:shadow-md transition-shadow cursor-pointer">
            <CardHeader className="flex flex-row items-center gap-4">
              <Activity className="h-8 w-8 text-primary" />
              <div>
                <CardTitle className="text-lg">Reconciliação</CardTitle>
                <CardDescription>Conferir dia de trabalho</CardDescription>
              </div>
              <ChevronRight className="ml-auto h-5 w-5 text-muted-foreground" />
            </CardHeader>
          </Card>
        </Link>

        <Link to="/sales">
          <Card className="hover:shadow-md transition-shadow cursor-pointer">
            <CardHeader className="flex flex-row items-center gap-4">
              <FileText className="h-8 w-8 text-primary" />
              <div>
                <CardTitle className="text-lg">Histórico</CardTitle>
                <CardDescription>Reimprimir ou anular</CardDescription>
              </div>
              <ChevronRight className="ml-auto h-5 w-5 text-muted-foreground" />
            </CardHeader>
          </Card>
        </Link>
      </div>

      {/* Detalhes */}
      <Card>
        <CardHeader>
          <CardTitle>Estado do Sistema</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          <div className="flex items-center justify-between py-2">
            <span>Modo Demonstração</span>
            <Badge variant={compliance.demoMode ? 'warning' : 'success'}>
              {compliance.demoMode ? 'Activo' : 'Inactivo'}
            </Badge>
          </div>
          <div className="flex items-center justify-between py-2">
            <span>Documentos por sincronizar</span>
            <Badge variant={compliance.pendingSyncCount > 50 ? 'destructive' : compliance.pendingSyncCount > 0 ? 'warning' : 'success'}>
              {compliance.pendingSyncCount}
            </Badge>
          </div>
          {compliance.branch && (
            <>
              <div className="flex items-center justify-between py-2">
                <span>Filial activa</span>
                <span className="text-sm text-muted-foreground">{compliance.branch.name}</span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span>Certificado AT</span>
                <span className="text-sm text-muted-foreground">{compliance.branch.softwareCertNumber || '—'}</span>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}