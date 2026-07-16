import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '@/lib/api'
import type { Sale } from '@/lib/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ReceiptPreviewModal, defaultFormatFor, type PrintFormat } from '@/components/ReceiptPreviewModal'
import { Printer, Eye, AlertCircle, X } from 'lucide-react'
import { formatMZN, formatDateTime, formatNuit } from '@/lib/utils'
import { toast } from 'sonner'

export function SalesListPage() {
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<'all' | 'EMITIDA' | 'ANULADA'>('all')
  const [selectedSale, setSelectedSale] = useState<Sale | null>(null)
  const [previewSale, setPreviewSale] = useState<Sale | null>(null)
  const [previewDefaultFormat, setPreviewDefaultFormat] = useState<PrintFormat | undefined>(undefined)

  const { data: sales, isLoading } = useQuery({
    queryKey: ['sales'],
    queryFn: async () => (await api.get<Sale[]>('/sales')).data,
  })

  const filtered = (sales || [])
    .filter((s) => filter === 'all' || s.state === filter)
    .filter((s) =>
      !search ||
      s.customerName?.toLowerCase().includes(search.toLowerCase()) ||
      s.customerNuit?.includes(search) ||
      String(s.documentNumber).includes(search)
    )
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 100)

  async function annul(sale: Sale) {
    const reason = prompt('Motivo da anulação:')
    if (!reason) return
    try {
      await api.post(`/sales/${sale.id}/annul`, { reason })
      toast.success('Factura anulada')
      location.reload()
    } catch (err: any) {
      toast.error(err.response?.data || 'Erro ao anular')
    }
  }

  async function reprint(sale: Sale) {
    const reason = prompt('Motivo da reimpressão:')
    if (!reason) return
    try {
      await api.post(`/sales/${sale.id}/reprint`, { reason })
      toast.success('Reimpressão registada — abra o PDF')
      // Abre PDF A4
      window.open(`/api/sales/${sale.id}/pdf`, '_blank')
    } catch (err: any) {
      toast.error(err.response?.data || 'Erro')
    }
  }

  function printThermal(sale: Sale) {
    // Abre o preview com o formato AT-padrão (80mm para TV/RC, A4 para FA).
    setPreviewSale(sale)
  }

  function sendWhatsApp(sale: Sale) {
    const phone = prompt('Número WhatsApp (ex: 841234567):')
    if (!phone) return
    api.post(`/receipts/${sale.id}/whatsapp`, null, { params: { phone } })
      .then((res) => {
        const data = res.data
        if (data.status === 'manual_link' && data.shareUrl) {
          window.open(data.shareUrl, '_blank')
        }
        toast.success('Recibo enviado')
      })
      .catch(() => toast.error('Erro ao enviar'))
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Facturas</h1>
          <p className="text-muted-foreground">Histórico e operações</p>
        </div>
        <Link to="/pos" className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          Nova Factura
        </Link>
      </div>

      <Card>
        <CardHeader>
          <div className="flex gap-2 flex-wrap items-center">
            <Input
              placeholder="Pesquisar por cliente, NUIT ou nº..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="max-w-sm"
            />
            <div className="flex gap-2">
              <Button variant={filter === 'all' ? 'default' : 'outline'} size="sm" onClick={() => setFilter('all')}>Todas</Button>
              <Button variant={filter === 'EMITIDA' ? 'default' : 'outline'} size="sm" onClick={() => setFilter('EMITIDA')}>Emitidas</Button>
              <Button variant={filter === 'ANULADA' ? 'default' : 'outline'} size="sm" onClick={() => setFilter('ANULADA')}>Anuladas</Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">A carregar...</div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">Sem facturas</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Documento</TableHead>
                  <TableHead>Data</TableHead>
                  <TableHead>Cliente</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead className="text-right">Acções</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell className="font-medium">
                      {s.documentType} {s.series}/{s.documentNumber}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDateTime(s.createdAt)}
                    </TableCell>
                    <TableCell>
                      <div>{s.customerName}</div>
                      <div className="text-xs text-muted-foreground">{formatNuit(s.customerNuit)}</div>
                    </TableCell>
                    <TableCell className="text-right font-medium">
                      {formatMZN(s.total)}
                    </TableCell>
                    <TableCell>
                      <Badge variant={s.state === 'EMITIDA' ? 'success' : 'destructive'}>
                        {s.state}
                      </Badge>
                      {s.demoFlag && <Badge variant="warning" className="ml-1">DEMO</Badge>}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1 flex-wrap">
                        <Button variant="ghost" size="icon" onClick={() => setSelectedSale(s)} title="Ver detalhes">
                          <Eye className="h-4 w-4" />
                        </Button>
                        {s.state === 'EMITIDA' && (
                          <>
                            <Button
                              variant="outline"
                              size="sm"
                              className="h-8 px-2"
                              onClick={() => {
                                setPreviewDefaultFormat(defaultFormatFor(s))
                                setPreviewSale(s)
                              }}
                              title={`Imprimir (${defaultFormatFor(s) === 'thermal-80mm' ? '80mm AT-padrão' : 'A4 AT-padrão'})`}
                            >
                              <Printer className="h-3.5 w-3.5" />
                              <span className="hidden lg:inline ml-1">
                                {defaultFormatFor(s) === 'thermal-80mm' ? '80mm' : 'A4'}
                              </span>
                            </Button>
                            <Button variant="ghost" size="icon" onClick={() => sendWhatsApp(s)} title="Enviar WhatsApp">
                              <AlertCircle className="h-4 w-4" />
                            </Button>
                            <Button variant="ghost" size="icon" onClick={() => annul(s)} title="Anular">
                              <X className="h-4 w-4 text-destructive" />
                            </Button>
                          </>
                        )}
                        {s.state === 'EMITIDA' && (
                          <Button variant="ghost" size="icon" onClick={() => reprint(s)} title="Reimprimir">
                            <Printer className="h-4 w-4 opacity-50" />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Modal de detalhes */}
      {selectedSale && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setSelectedSale(null)}>
          <Card className="w-full max-w-2xl max-h-[90vh] overflow-auto" onClick={(e) => e.stopPropagation()}>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>{selectedSale.documentType} {selectedSale.series}/{selectedSale.documentNumber}</CardTitle>
              <Button variant="ghost" size="icon" onClick={() => setSelectedSale(null)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div><span className="text-muted-foreground">Data:</span> {formatDateTime(selectedSale.createdAt)}</div>
                <div><span className="text-muted-foreground">Estado:</span> <Badge variant={selectedSale.state === 'EMITIDA' ? 'success' : 'destructive'}>{selectedSale.state}</Badge></div>
                <div><span className="text-muted-foreground">Cliente:</span> {selectedSale.customerName}</div>
                <div><span className="text-muted-foreground">NUIT:</span> {formatNuit(selectedSale.customerNuit)}</div>
              </div>

              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Descrição</TableHead>
                    <TableHead>Qtd</TableHead>
                    <TableHead>Preço</TableHead>
                    <TableHead>IVA</TableHead>
                    <TableHead className="text-right">Total</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {selectedSale.items?.map((i, idx) => (
                    <TableRow key={idx}>
                      <TableCell>{i.description}</TableCell>
                      <TableCell>{i.qty}</TableCell>
                      <TableCell>{formatMZN(i.unitPrice)}</TableCell>
                      <TableCell>{i.taxRate}%</TableCell>
                      <TableCell className="text-right">{formatMZN(i.lineTotal)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <div className="border-t pt-3 space-y-1 text-sm">
                <div className="flex justify-between"><span>Subtotal:</span><span>{formatMZN(selectedSale.subtotal)}</span></div>
                <div className="flex justify-between"><span>IVA:</span><span>{formatMZN(selectedSale.totalTax)}</span></div>
                <div className="flex justify-between font-bold text-base border-t pt-2 mt-2"><span>TOTAL:</span><span>{formatMZN(selectedSale.total)}</span></div>
              </div>

              {selectedSale.hashHash && (
                <div className="border-t pt-3 space-y-1 text-xs">
                  <div className="font-semibold">Hash AT (MD5):</div>
                  <code className="block bg-muted p-2 rounded break-all">{selectedSale.hashHash}</code>
                  {selectedSale.hashControl && <div className="text-muted-foreground">Controle: {selectedSale.hashControl}</div>}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Modal de preview/print do recibo com seletor de formato AT */}
      <ReceiptPreviewModal
        sale={previewSale}
        open={previewSale !== null}
        onClose={() => {
          setPreviewSale(null)
          setPreviewDefaultFormat(undefined)
        }}
        defaultFormat={previewDefaultFormat}
      />
    </div>
  )
}