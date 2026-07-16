import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { TodayReport, TopProductsReport } from '@/lib/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { BarChart3, TrendingUp, Receipt, Award } from 'lucide-react'
import { formatMZN } from '@/lib/utils'

export function ReportsPage() {
  const [from, setFrom] = useState(new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10))
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10))

  const { data: today } = useQuery({
    queryKey: ['reports-today'],
    queryFn: async () => (await api.get<TodayReport>('/reports/today')).data,
  })

  const { data: period } = useQuery({
    queryKey: ['reports-period', from, to],
    queryFn: async () => (await api.get<TodayReport>('/reports/period', { params: { from, to } })).data,
  })

  const { data: top } = useQuery({
    queryKey: ['reports-top'],
    queryFn: async () => (await api.get<TopProductsReport>('/reports/top-products', { params: { limit: 10 } })).data,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <BarChart3 className="h-8 w-8" />
          Relatórios
        </h1>
        <p className="text-muted-foreground">Análise de vendas e performance</p>
      </div>

      {/* Hoje */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2"><Receipt className="h-5 w-5" /> Hoje</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <div className="text-sm text-muted-foreground">Vendas</div>
              <div className="text-2xl font-bold">{today?.totalSales || 0}</div>
            </div>
            <div>
              <div className="text-sm text-muted-foreground">Faturação</div>
              <div className="text-2xl font-bold">{formatMZN(today?.totalAmount)}</div>
            </div>
            <div>
              <div className="text-sm text-muted-foreground">IVA</div>
              <div className="text-2xl font-bold">{formatMZN(today?.totalTax)}</div>
            </div>
            <div>
              <div className="text-sm text-muted-foreground">Ticket Médio</div>
              <div className="text-2xl font-bold">{formatMZN(today?.averageTicket)}</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Período */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            Período Personalizado
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4 mb-4">
            <div>
              <Label>De</Label>
              <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
            </div>
            <div>
              <Label>Até</Label>
              <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
            </div>
          </div>
          {period && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <div className="text-sm text-muted-foreground">Vendas</div>
                <div className="text-2xl font-bold">{period.totalSales}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Faturação</div>
                <div className="text-2xl font-bold">{formatMZN(period.totalAmount)}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">IVA</div>
                <div className="text-2xl font-bold">{formatMZN(period.totalTax)}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Ticket Médio</div>
                <div className="text-2xl font-bold">{formatMZN(period.averageTicket)}</div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Top produtos */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2"><Award className="h-5 w-5" /> Top 10 Produtos</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>#</TableHead>
                <TableHead>Produto</TableHead>
                <TableHead className="text-right">Qtd Vendida</TableHead>
                <TableHead className="text-right">Receita</TableHead>
                <TableHead className="text-right">Transacções</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {top?.top.map((p, i) => (
                <TableRow key={p.code}>
                  <TableCell className="font-mono">{i + 1}</TableCell>
                  <TableCell className="font-medium">{p.name}</TableCell>
                  <TableCell className="text-right">{p.qtySold.toFixed(2)}</TableCell>
                  <TableCell className="text-right">{formatMZN(p.revenue)}</TableCell>
                  <TableCell className="text-right">{p.transactions}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}