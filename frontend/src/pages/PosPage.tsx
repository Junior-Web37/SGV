import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Product, Sale, TaxExemptionReason, TaxType, Customer, Branch } from '@/lib/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertIcon } from '@/components/ui/alert'
import { ReceiptPreviewModal } from '@/components/ReceiptPreviewModal'
import {
  Plus, Trash2, Send, Printer, MessageCircle, Save, Search, Loader2, AlertCircle, X
} from 'lucide-react'
import { formatMZN, formatNuit } from '@/lib/utils'
import { toast } from 'sonner'

interface CartItem {
  productId?: number
  description: string
  unit: string
  qty: number
  unitPrice: number
  discount: number
  iceRate: number
  taxRate: number
  taxType?: string
  motivoInexistTax?: string
}

export function PosPage() {
  const qc = useQueryClient()

  const { data: products } = useQuery({
    queryKey: ['products'],
    queryFn: async () => (await api.get<Product[]>('/products')).data,
  })

  const { data: branches } = useQuery({
    queryKey: ['branches'],
    queryFn: async () => (await api.get<Branch[]>('/branches')).data,
  })

  const { data: taxTypes } = useQuery({
    queryKey: ['catalog-tax-types'],
    queryFn: async () => (await api.get<TaxType>('/catalog/tax-types')).data,
  })

  const { data: exemptionReasons } = useQuery({
    queryKey: ['catalog-exemptions'],
    queryFn: async () => (await api.get<TaxExemptionReason>('/catalog/tax-exemption-reasons')).data,
  })

  const { data: paymentMethods } = useQuery({
    queryKey: ['catalog-payments'],
    queryFn: async () => (await api.get<Record<string, string>>('/catalog/payment-methods')).data,
  })

  // ─── Estado do formulário ──────────────────────────────────────────────────
  const [branchId, setBranchId] = useState<number | undefined>()
  const [documentType, setDocumentType] = useState('FA')
  const [series, setSeries] = useState('A')
  const [customerNuit, setCustomerNuit] = useState('999999999')
  const [customerName, setCustomerName] = useState('CONSUMIDOR FINAL')
  const [customerAddress, setCustomerAddress] = useState('')
  const [withholdingRate, setWithholdingRate] = useState(0)
  const [cart, setCart] = useState<CartItem[]>([])
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [paidAmount, setPaidAmount] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [cashSessionStatus, setCashSessionStatus] = useState<{ ok: boolean; message: string } | null>(null)
  const [cashSessionLoading, setCashSessionLoading] = useState(true)
  // Recibo acabado de emitir — quando setado, abre o modal de preview.
  const [lastSale, setLastSale] = useState<Sale | null>(null)
  const [showReceipt, setShowReceipt] = useState(false)

  // ─── Pesquisa de produtos ─────────────────────────────────────────────────
  const [search, setSearch] = useState('')
  const filteredProducts = (products || [])
    .filter((p) => !search || p.name?.toLowerCase().includes(search.toLowerCase()) || p.code?.toLowerCase().includes(search.toLowerCase()))
    .slice(0, 10)

  function addToCart(p: Product) {
    const existing = cart.find((i) => i.productId === p.id)
    if (existing) {
      setCart(cart.map((i) => i.productId === p.id ? { ...i, qty: i.qty + 1 } : i))
    } else {
      setCart([...cart, {
        productId: p.id,
        description: p.name || '',
        unit: p.unit || 'UN',
        qty: 1,
        unitPrice: p.price || 0,
        discount: 0,
        iceRate: p.iceRate || 0,
        taxRate: p.taxRate || 16,
        taxType: (p.taxRate || 0) > 0 ? 'IVA' : undefined,
        motivoInexistTax: (p.taxRate || 0) === 0 ? 'M09' : undefined,
      }])
    }
  }

  function addCustomItem() {
    setCart([...cart, {
      description: '',
      unit: 'UN',
      qty: 1,
      unitPrice: 0,
      discount: 0,
      iceRate: 0,
      taxRate: 16,
      taxType: 'IVA',
    }])
  }

  function updateItem(idx: number, patch: Partial<CartItem>) {
    setCart(cart.map((i, k) => k === idx ? { ...i, ...patch } : i))
  }

  function removeItem(idx: number) {
    setCart(cart.filter((_, k) => k !== idx))
  }

  // ─── Cálculos ──────────────────────────────────────────────────────────────
  function calcItem(i: CartItem) {
    const base = i.qty * i.unitPrice
    const discount = base * i.discount / 100
    const netBase = base - discount
    const ice = netBase * i.iceRate / 100
    const tax = netBase * i.taxRate / 100
    const total = netBase + ice + tax
    return { base, discount, netBase, ice, tax, total }
  }

  const totals = cart.reduce((acc, i) => {
    const c = calcItem(i)
    acc.subtotal += c.netBase
    acc.ice += c.ice
    acc.tax += c.tax
    acc.total += c.total
    acc.discount += c.discount
    return acc
  }, { subtotal: 0, ice: 0, tax: 0, total: 0, discount: 0 })

  const withholdingAmount = totals.subtotal * withholdingRate / 100
  const finalTotal = totals.total - withholdingAmount
  const change = Math.max(0, paidAmount - finalTotal)

  useEffect(() => {
    let active = true
    async function loadCashSessionStatus() {
      try {
        const res = await api.get<{ ok: boolean; message: string }>('/sales/cash-session-status')
        if (active) {
          setCashSessionStatus(res.data)
        }
      } catch {
        if (active) {
          setCashSessionStatus({ ok: false, message: 'Não foi possível verificar o estado do caixa.' })
        }
      } finally {
        if (active) {
          setCashSessionLoading(false)
        }
      }
    }

    loadCashSessionStatus()
    return () => { active = false }
  }, [])

  async function submit() {
    if (!branchId) {
      toast.error('Seleccione uma filial')
      return
    }
    if (cart.length === 0) {
      toast.error('Adicione pelo menos um item')
      return
    }
    if (!cashSessionStatus?.ok) {
      toast.error(cashSessionStatus?.message || 'Só é possível emitir vendas com o caixa aberto.')
      return
    }

    setSubmitting(true)
    try {
      const res = await api.post<Sale>('/sales', {
        branchId,
        documentType,
        series,
        customerNuit,
        customerName,
        customerAddress: customerAddress || undefined,
        withholdingTaxRate: withholdingRate || 0,
        items: cart.map((i) => ({
          productId: i.productId,
          description: i.description,
          unit: i.unit,
          qty: i.qty,
          unitPrice: i.unitPrice,
          discount: i.discount,
          iceRate: i.iceRate,
          taxRate: i.taxRate,
          taxType: i.taxType,
          motivoInexistTax: i.motivoInexistTax,
        })),
        payments: [{ method: paymentMethod, amount: paidAmount || finalTotal }],
      })
      toast.success(`Factura ${res.data.documentType} ${res.data.series}/${res.data.documentNumber} emitida`)
      qc.invalidateQueries({ queryKey: ['compliance'] })
      qc.invalidateQueries({ queryKey: ['reports-today'] })
      qc.invalidateQueries({ queryKey: ['sales'] })

      // Reset
      setCart([])
      setPaidAmount(0)

      // Abre o preview do recibo imediatamente (sem scroll, sem redirecionar)
      setLastSale(res.data)
      setShowReceipt(true)
    } catch (err: any) {
      const data = err.response?.data
      if (data?.error === 'Possível factura duplicada') {
        toast.error(`Duplicação detectada: ${data.existingDocument}`)
      } else {
        toast.error(typeof data === 'string' ? data : data?.error || 'Erro ao emitir factura')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Ponto de Venda</h1>
        <p className="text-muted-foreground">Emitir factura conforme AT</p>
      </div>

      {cashSessionStatus && !cashSessionStatus.ok && (
        <Alert variant="destructive">
          <AlertIcon variant="destructive" />
          <AlertDescription>{cashSessionStatus.message}</AlertDescription>
        </Alert>
      )}

      {cashSessionStatus?.ok && (
        <Alert variant="success">
          <AlertIcon variant="success" />
          <AlertDescription>O caixa está aberto. As vendas podem ser emitidas.</AlertDescription>
        </Alert>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* COLUNA ESQUERDA — Produtos + Cliente */}
        <div className="lg:col-span-2 space-y-4">
          {/* Header da factura */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Cabeçalho</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>Filial</Label>
                  <Select value={branchId} onChange={(e) => setBranchId(parseInt(e.target.value))}>
                    <option value="">Seleccione...</option>
                    {branches?.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Tipo Doc.</Label>
                  <Select value={documentType} onChange={(e) => setDocumentType(e.target.value)}>
                    <option value="FA">FA — Factura</option>
                    <option value="NC">NC — Nota Crédito</option>
                    <option value="ND">ND — Nota Débito</option>
                    <option value="RC">RC — Recibo</option>
                    <option value="TV">TV — Talão</option>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Série</Label>
                  <Input value={series} onChange={(e) => setSeries(e.target.value.toUpperCase())} maxLength={2} />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Cliente */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Cliente</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>NUIT</Label>
                  <Input value={customerNuit} onChange={(e) => setCustomerNuit(e.target.value.replace(/\D/g, '').slice(0, 9))} placeholder="999999999" />
                  <p className="text-xs text-muted-foreground">{formatNuit(customerNuit)}</p>
                </div>
                <div className="space-y-2 col-span-2">
                  <Label>Nome</Label>
                  <Input value={customerName} onChange={(e) => setCustomerName(e.target.value)} />
                </div>
              </div>
              {customerNuit !== '999999999' && (
                <div className="mt-4 space-y-2">
                  <Label>Endereço <span className="text-xs text-muted-foreground">(obrigatório para B2B)</span></Label>
                  <Input value={customerAddress} onChange={(e) => setCustomerAddress(e.target.value)} />
                </div>
              )}
            </CardContent>
          </Card>

          {/* Produtos */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Produtos</CardTitle>
              <CardDescription>Pesquise e adicione ao carrinho</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Pesquisar por nome ou código..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                />
              </div>

              {search && (
                <div className="border rounded-md max-h-60 overflow-auto">
                  {filteredProducts.length === 0 ? (
                    <div className="p-4 text-center text-sm text-muted-foreground">Sem resultados</div>
                  ) : filteredProducts.map((p) => (
                    <button
                      key={p.id}
                      onClick={() => { addToCart(p); setSearch('') }}
                      className="w-full text-left p-3 hover:bg-muted/50 flex justify-between border-b last:border-0"
                    >
                      <div>
                        <div className="font-medium">{p.name}</div>
                        <div className="text-xs text-muted-foreground">{p.code} • {p.unit} • IVA {p.taxRate}%</div>
                      </div>
                      <div className="font-semibold">{formatMZN(p.price)}</div>
                    </button>
                  ))}
                </div>
              )}

              <Button variant="outline" size="sm" onClick={addCustomItem}>
                <Plus className="h-4 w-4" /> Adicionar item personalizado
              </Button>
            </CardContent>
          </Card>

          {/* Itens do carrinho */}
          {cart.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Itens ({cart.length})</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {cart.map((item, idx) => {
                  const c = calcItem(item)
                  return (
                    <div key={idx} className="border rounded-md p-3 space-y-2">
                      <div className="flex items-start gap-2">
                        <Input
                          value={item.description}
                          onChange={(e) => updateItem(idx, { description: e.target.value })}
                          placeholder="Descrição"
                          className="flex-1"
                        />
                        <Button variant="ghost" size="icon" onClick={() => removeItem(idx)}>
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                      <div className="grid grid-cols-6 gap-2">
                        <div>
                          <Label className="text-xs">Qtd</Label>
                          <Input type="number" min={0.01} step={0.01} value={item.qty} onChange={(e) => updateItem(idx, { qty: parseFloat(e.target.value) || 0 })} />
                        </div>
                        <div>
                          <Label className="text-xs">Preço</Label>
                          <Input type="number" min={0} step={0.01} value={item.unitPrice} onChange={(e) => updateItem(idx, { unitPrice: parseFloat(e.target.value) || 0 })} />
                        </div>
                        <div>
                          <Label className="text-xs">Desc %</Label>
                          <Input type="number" min={0} max={100} value={item.discount} onChange={(e) => updateItem(idx, { discount: parseFloat(e.target.value) || 0 })} />
                        </div>
                        <div>
                          <Label className="text-xs">IVA %</Label>
                          <Input type="number" min={0} max={100} value={item.taxRate} onChange={(e) => {
                            const r = parseFloat(e.target.value) || 0
                            updateItem(idx, {
                              taxRate: r,
                              taxType: r > 0 ? (item.taxType || 'IVA') : undefined,
                              motivoInexistTax: r === 0 ? (item.motivoInexistTax || 'M09') : undefined,
                            })
                          }} />
                        </div>
                        <div>
                          <Label className="text-xs">Tipo</Label>
                          <Select value={item.taxType || ''} onChange={(e) => updateItem(idx, { taxType: e.target.value })}>
                            <option value="">—</option>
                            {taxTypes && Object.keys(taxTypes).map((c) => <option key={c} value={c}>{c}</option>)}
                          </Select>
                        </div>
                        <div>
                          <Label className="text-xs">Motivo</Label>
                          <Select value={item.motivoInexistTax || ''} onChange={(e) => updateItem(idx, { motivoInexistTax: e.target.value || undefined })} disabled={(item.taxRate || 0) > 0}>
                            <option value="">—</option>
                            {exemptionReasons && Object.entries(exemptionReasons).map(([code, desc]) => (
                              <option key={code} value={code}>{code} — {desc}</option>
                            ))}
                          </Select>
                        </div>
                      </div>
                      <div className="text-right text-sm">
                        Base: {formatMZN(c.netBase)} + IVA: {formatMZN(c.tax)} = <strong>{formatMZN(c.total)}</strong>
                      </div>
                    </div>
                  )
                })}
              </CardContent>
            </Card>
          )}
        </div>

        {/* COLUNA DIREITA — Resumo + Pagamento */}
        <div className="space-y-4">
          <Card className="sticky top-4">
            <CardHeader>
              <CardTitle className="text-lg">Resumo</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-1 text-sm">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span>{formatMZN(totals.subtotal)}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>Desconto</span>
                  <span>-{formatMZN(totals.discount)}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>ICE</span>
                  <span>{formatMZN(totals.ice)}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>IVA</span>
                  <span>{formatMZN(totals.tax)}</span>
                </div>
                {withholdingRate > 0 && (
                  <div className="flex justify-between text-amber-600">
                    <span>Retenção ({withholdingRate}%)</span>
                    <span>-{formatMZN(withholdingAmount)}</span>
                  </div>
                )}
              </div>

              <div className="border-t pt-3 space-y-2">
                <div className="space-y-1">
                  <Label className="text-xs">Retenção na fonte (%)</Label>
                  <Input
                    type="number"
                    min={0}
                    max={100}
                    step={0.5}
                    value={withholdingRate}
                    onChange={(e) => setWithholdingRate(parseFloat(e.target.value) || 0)}
                  />
                </div>

                <div className="space-y-1">
                  <Label className="text-xs">Forma de pagamento</Label>
                  <Select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
                    {paymentMethods && Object.entries(paymentMethods).map(([k, v]) => (
                      <option key={k} value={k}>{v}</option>
                    ))}
                  </Select>
                </div>

                <div className="space-y-1">
                  <Label className="text-xs">Valor recebido</Label>
                  <Input
                    type="number"
                    min={0}
                    step={0.01}
                    value={paidAmount}
                    onChange={(e) => setPaidAmount(parseFloat(e.target.value) || 0)}
                    placeholder={finalTotal.toFixed(2)}
                  />
                </div>
              </div>

              <div className="border-t pt-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">TOTAL</span>
                  <span className="text-2xl font-bold text-primary">{formatMZN(finalTotal)}</span>
                </div>
                {paidAmount > 0 && change > 0 && (
                  <div className="flex justify-between items-center mt-2 text-sm">
                    <span>Troco</span>
                    <span className="font-semibold text-emerald-600">{formatMZN(change)}</span>
                  </div>
                )}
              </div>

              <Button className="w-full" size="lg" onClick={submit} disabled={submitting || cart.length === 0 || !branchId || cashSessionLoading || !cashSessionStatus?.ok}>
                {submitting ? <><Loader2 className="h-4 w-4 animate-spin" /> A emitir...</> : <><Send className="h-4 w-4" /> Emitir Factura</>}
              </Button>

              <p className="text-xs text-muted-foreground text-center">
                Conforme Decreto 7/2024 — Facturação Electrónica
              </p>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Modal de preview do recibo — abre automaticamente após a venda.
          Mostra o documento no formato AT-padrão (80mm para TV/RC, A4 para FA) e
          expõe botões visíveis para outros formatos (A4, A5, 80mm) e ações. */}
      <ReceiptPreviewModal
        sale={lastSale}
        open={showReceipt}
        onClose={() => setShowReceipt(false)}
        title={lastSale ? `Recibo emitido — ${lastSale.documentType} ${lastSale.series}/${lastSale.documentNumber}` : undefined}
      />
    </div>
  )
}