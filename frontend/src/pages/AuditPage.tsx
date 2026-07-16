import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { HashHistoryItem, HashAuditResult } from '@/lib/types'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertIcon } from '@/components/ui/alert'
import { History, ShieldCheck, ShieldAlert, RefreshCw } from 'lucide-react'
import { formatDateTime, formatMZN } from '@/lib/utils'

export function AuditPage() {
  const [from, setFrom] = useState(new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10))
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10))

  const { data: history, refetch: refetchHistory } = useQuery({
    queryKey: ['hash-history', from, to],
    queryFn: async () => (await api.get<HashHistoryItem[]>('/reports/hash-history', { params: { from, to } })).data,
  })

  const { data: audit, refetch: refetchAudit } = useQuery({
    queryKey: ['hash-audit'],
    queryFn: async () => (await api.get<HashAuditResult>('/reports/hash-audit')).data,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <History className="h-8 w-8" />
            Auditoria AT
          </h1>
          <p className="text-muted-foreground">Histórico de hashes e integridade</p>
        </div>
        <Button variant="outline" onClick={() => { refetchHistory(); refetchAudit() }}>
          <RefreshCw className="h-4 w-4" /> Actualizar
        </Button>
      </div>

      {/* Auditoria global */}
      {audit && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              {audit.status === 'clean' ? (
                <ShieldCheck className="h-5 w-5 text-emerald-600" />
              ) : (
                <ShieldAlert className="h-5 w-5 text-destructive" />
              )}
              Estado da Integridade
            </CardTitle>
            <CardDescription>
              {audit.totalDocuments} documentos analisados — {audit.issueCount} {audit.issueCount === 1 ? 'problema' : 'problemas'} encontrado(s)
            </CardDescription>
          </CardHeader>
          <CardContent>
            {audit.status === 'clean' ? (
              <Alert variant="success">
                <ShieldCheck className="h-4 w-4 text-emerald-600" />
                <AlertDescription>Sem inconsistências detectadas.</AlertDescription>
              </Alert>
            ) : (
              <Alert variant="destructive">
                <ShieldAlert className="h-4 w-4" />
                <AlertDescription>
                  <strong>Possíveis problemas detectados:</strong>
                  <ul className="mt-2 list-disc pl-4 text-sm">
                    {audit.issues.map((issue, i) => (
                      <li key={i}>
                        <code className="text-xs bg-destructive/20 px-1 rounded">{issue.code}</code> — {'message' in issue ? String(issue.message) : JSON.stringify(issue)}
                      </li>
                    ))}
                  </ul>
                </AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

      {/* Histórico */}
      <Card>
        <CardHeader>
          <CardTitle>Histórico de Hashes</CardTitle>
          <div className="flex gap-2 pt-2">
            <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="w-40" />
            <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="w-40" />
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Documento</TableHead>
                <TableHead>Data</TableHead>
                <TableHead>NUIT</TableHead>
                <TableHead>HashHash (MD5)</TableHead>
                <TableHead className="text-right">Total</TableHead>
                <TableHead>Estado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {history?.map((h) => (
                <TableRow key={h.id}>
                  <TableCell className="font-medium">{h.document}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{formatDateTime(h.createdAt)}</TableCell>
                  <TableCell className="text-sm">{h.customerNuit}</TableCell>
                  <TableCell>
                    <code className="text-xs bg-muted px-1.5 py-0.5 rounded">{h.hashHash.slice(0, 16)}…</code>
                  </TableCell>
                  <TableCell className="text-right">{formatMZN(h.total)}</TableCell>
                  <TableCell>
                    <Badge variant={h.state === 'EMITIDA' ? 'success' : 'destructive'}>{h.state}</Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}