import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { ReconciliationReport } from '@/lib/types'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertIcon } from '@/components/ui/alert'
import { Calculator, Download, AlertTriangle, CheckCircle2 } from 'lucide-react'
import { formatMZN } from '@/lib/utils'

export function ReconciliationPage() {
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))

  const { data: report, isLoading } = useQuery({
    queryKey: ['reconciliation', date],
    queryFn: async () => (await api.get<ReconciliationReport>('/reconciliation/daily', { params: { date } })).data,
  })

  function exportXml() {
    const [y, m] = date.split('-')
    window.open(`/api/sales/export/xml?year=${y}&month=${parseInt(m)}`, '_blank')
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <Calculator className="h-8 w-8" />
            Reconciliação Diária
          </h1>
          <p className="text-muted-foreground">Conferência de fim de dia</p>
        </div>
        <div className="flex gap-2">
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="w-44" />
          <Button variant="outline" onClick={exportXml}>
            <Download className="h-4 w-4" /> Exportar SAF-T
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="text-center py-8 text-muted-foreground">A calcular...</div>
      ) : report ? (
        <>
          {/* Resumo geral */}
          <div className="grid gap-4 md:grid-cols-4">
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-sm">Documentos</CardTitle></CardHeader>
              <CardContent><div className="text-2xl font-bold">{report.totalDocuments}</div></CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-sm">Emitidos</CardTitle></CardHeader>
              <CardContent><div className="text-2xl font-bold text-emerald-600">{report.emittedCount}</div></CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-sm">Anulados</CardTitle></CardHeader>
              <CardContent><div className="text-2xl font-bold text-destructive">{report.annulledCount}</div></CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-sm">Total Vendas</CardTitle></CardHeader>
              <CardContent><div className="text-2xl font-bold">{formatMZN(report.totalVendas)}</div></CardContent>
            </Card>
          </div>

          {/* Alertas */}
          {report.hasDiscrepancies ? (
            <Alert variant="warning">
              <AlertIcon variant="warning" />
              <AlertDescription>
                <strong>Discrepâncias detectadas:</strong>
                <ul className="mt-2 list-disc pl-4 text-sm">
                  {report.discrepancies.map((d, i) => <li key={i}>{d}</li>)}
                </ul>
              </AlertDescription>
            </Alert>
          ) : (
            <Alert variant="success">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" />
              <AlertDescription>Tudo conferido — sem discrepâncias.</AlertDescription>
            </Alert>
          )}

          {/* Totais por tipo */}
          <Card>
            <CardHeader><CardTitle>Totais por Tipo de Documento</CardTitle></CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tipo</TableHead>
                    <TableHead className="text-right">Qtd</TableHead>
                    <TableHead className="text-right">Total</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {Object.entries(report.porTipoDocumento).map(([tipo, d]) => (
                    <TableRow key={tipo}>
                      <TableCell className="font-medium">{tipo}</TableCell>
                      <TableCell className="text-right">{d.count}</TableCell>
                      <TableCell className="text-right">{formatMZN(d.total)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Por taxa IVA */}
          <Card>
            <CardHeader>
              <CardTitle>Resumo por Taxa de IVA</CardTitle>
              <CardDescription>Para submissão à AT</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Taxa</TableHead>
                    <TableHead className="text-right">Base Tributável</TableHead>
                    <TableHead className="text-right">IVA</TableHead>
                    <TableHead className="text-right">Itens</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {report.porTaxaIva.map((t, i) => (
                    <TableRow key={i}>
                      <TableCell className="font-medium">{t.taxRate}%</TableCell>
                      <TableCell className="text-right">{formatMZN(t.base)}</TableCell>
                      <TableCell className="text-right">{formatMZN(t.tax)}</TableCell>
                      <TableCell className="text-right">{t.count}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Por forma pagamento */}
          <Card>
            <CardHeader><CardTitle>Por Forma de Pagamento</CardTitle></CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Método</TableHead>
                    <TableHead className="text-right">Total</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {Object.entries(report.porFormaPagamento).map(([m, v]) => (
                    <TableRow key={m}>
                      <TableCell className="font-medium">{m}</TableCell>
                      <TableCell className="text-right">{formatMZN(v)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Séries */}
          <Card>
            <CardHeader><CardTitle>Séries Documentais</CardTitle></CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Série</TableHead>
                    <TableHead>Primeiro</TableHead>
                    <TableHead>Último</TableHead>
                    <TableHead className="text-right">Qtd</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {Object.entries(report.series).map(([k, s]) => (
                    <TableRow key={k}>
                      <TableCell className="font-medium">{k}</TableCell>
                      <TableCell>{s.first}</TableCell>
                      <TableCell>{s.last}</TableCell>
                      <TableCell className="text-right">{s.count}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Anulações */}
          {report.annulled.length > 0 && (
            <Card>
              <CardHeader><CardTitle>Documentos Anulados</CardTitle></CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Documento</TableHead>
                      <TableHead>Motivo</TableHead>
                      <TableHead className="text-right">Total</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {report.annulled.map((a) => (
                      <TableRow key={a.id}>
                        <TableCell className="font-medium">{a.document}</TableCell>
                        <TableCell>{a.reason}</TableCell>
                        <TableCell className="text-right">{formatMZN(a.total)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </>
      ) : null}
    </div>
  )
}