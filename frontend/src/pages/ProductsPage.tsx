import { useState } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Product } from '@/lib/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Plus, Edit, Trash2, Save, X, Search, Package } from 'lucide-react'
import { toast } from 'sonner'
import { formatMZN } from '@/lib/utils'

interface FormData {
  code: string
  name: string
  unit: string
  priceCost: number
  priceSale: number
  taxRate: number
  iceRate: number
  isService: boolean
}

const empty: FormData = { code: '', name: '', unit: 'UN', priceCost: 0, priceSale: 0, taxRate: 16, iceRate: 0, isService: false }

export function ProductsPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<Product | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<FormData>(empty)
  const [search, setSearch] = useState('')

  const { data: products, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: async () => (await api.get<Product[]>('/products')).data,
  })

  const filtered = (products || []).filter((p) =>
    !search || p.name?.toLowerCase().includes(search.toLowerCase()) || p.code?.toLowerCase().includes(search.toLowerCase())
  )

  const saveMutation = useMutation({
    mutationFn: async (data: FormData) => {
      if (editing) {
        return (await api.put(`/products/${editing.id}`, data)).data
      }
      return (await api.post('/products', data)).data
    },
    onSuccess: () => {
      toast.success(editing ? 'Produto actualizado' : 'Produto criado')
      qc.invalidateQueries({ queryKey: ['products'] })
      setEditing(null)
      setCreating(false)
      setForm(empty)
    },
    onError: (err: any) => toast.error(err.response?.data?.error || err.response?.data || 'Erro ao gravar'),
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/products/${id}`),
    onSuccess: () => {
      toast.success('Produto removido')
      qc.invalidateQueries({ queryKey: ['products'] })
    },
    onError: () => toast.error('Não foi possível remover (pode haver vendas associadas)'),
  })

  function startEdit(p: Product) {
    setEditing(p)
    setForm({
      code: p.code || '',
      name: p.name || '',
      unit: p.unit || 'UN',
      priceCost: p.cost || p.priceCost || 0,
      priceSale: p.price || p.priceSale || 0,
      taxRate: p.taxRate || 0,
      iceRate: p.iceRate || 0,
      isService: !!p.service,
    })
    setCreating(true)
  }

  function cancel() {
    setEditing(null)
    setCreating(false)
    setForm(empty)
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.code || !form.name) {
      toast.error('Código e nome são obrigatórios')
      return
    }
    saveMutation.mutate(form)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <Package className="h-8 w-8" /> Produtos
          </h1>
          <p className="text-muted-foreground">{(products || []).length} produto(s) registado(s)</p>
        </div>
        <Button onClick={() => { setEditing(null); setCreating(true); setForm(empty) }}>
          <Plus className="h-4 w-4" /> Novo Produto
        </Button>
      </div>

      {creating && (
        <Card>
          <CardHeader>
            <CardTitle>{editing ? `Editar: ${editing.name}` : 'Novo Produto'}</CardTitle>
            <CardDescription>Preencha os dados básicos do produto</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="space-y-2">
                  <Label>Código *</Label>
                  <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} placeholder="P001" required />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <Label>Nome *</Label>
                  <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
                </div>
                <div className="space-y-2">
                  <Label>Unidade</Label>
                  <Input value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value.toUpperCase() })} />
                </div>
                <div className="space-y-2">
                  <Label>Preço Custo</Label>
                  <Input type="number" min={0} step={0.01} value={form.priceCost} onChange={(e) => setForm({ ...form, priceCost: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="space-y-2">
                  <Label>Preço Venda *</Label>
                  <Input type="number" min={0} step={0.01} value={form.priceSale} onChange={(e) => setForm({ ...form, priceSale: parseFloat(e.target.value) || 0 })} required />
                </div>
                <div className="space-y-2">
                  <Label>Taxa IVA (%)</Label>
                  <Input type="number" min={0} max={100} step={0.01} value={form.taxRate} onChange={(e) => setForm({ ...form, taxRate: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="space-y-2">
                  <Label>Taxa ICE (%)</Label>
                  <Input type="number" min={0} max={100} step={0.01} value={form.iceRate} onChange={(e) => setForm({ ...form, iceRate: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="flex items-center space-x-2 pt-6">
                  <input id="isService" type="checkbox" checked={form.isService} onChange={(e) => setForm({ ...form, isService: e.target.checked })} className="h-4 w-4" />
                  <Label htmlFor="isService">É um serviço (não desconta stock)</Label>
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="outline" type="button" onClick={cancel}><X className="h-4 w-4" /> Cancelar</Button>
                <Button type="submit" disabled={saveMutation.isPending}>
                  <Save className="h-4 w-4" /> {saveMutation.isPending ? 'A guardar...' : 'Guardar'}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input placeholder="Pesquisar por nome ou código..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" />
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">A carregar...</div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">Sem produtos. Crie o primeiro!</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Código</TableHead>
                  <TableHead>Nome</TableHead>
                  <TableHead>Un.</TableHead>
                  <TableHead className="text-right">Custo</TableHead>
                  <TableHead className="text-right">Venda</TableHead>
                  <TableHead className="text-right">IVA</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead className="text-right">Acções</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="font-mono">{p.code}</TableCell>
                    <TableCell className="font-medium">{p.name}</TableCell>
                    <TableCell>{p.unit || 'UN'}</TableCell>
                    <TableCell className="text-right text-muted-foreground">{formatMZN(p.cost)}</TableCell>
                    <TableCell className="text-right font-semibold">{formatMZN(p.price)}</TableCell>
                    <TableCell className="text-right">{p.taxRate}%</TableCell>
                    <TableCell>
                      <Badge variant={p.service ? 'secondary' : 'outline'}>{p.service ? 'Serviço' : 'Produto'}</Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" onClick={() => startEdit(p)}><Edit className="h-4 w-4" /></Button>
                        <Button variant="ghost" size="icon" onClick={() => {
                          if (confirm(`Remover "${p.name}"?`)) deleteMutation.mutate(p.id!)
                        }}><Trash2 className="h-4 w-4 text-destructive" /></Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}