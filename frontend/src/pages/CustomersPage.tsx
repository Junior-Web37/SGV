import { useState } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Customer } from '@/lib/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Plus, Edit, Trash2, Save, X, Search, Users } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { toast } from 'sonner'
import { formatMZN, formatNuit } from '@/lib/utils'

interface FormData {
  code: string
  name: string
  nuit: string
  type: string
  contact: string
  address: string
  creditLimit: number
  defaultDiscount: number
}

const empty: FormData = { code: '', name: '', nuit: '', type: 'B2B', contact: '', address: '', creditLimit: 0, defaultDiscount: 0 }

export function CustomersPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<Customer | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<FormData>(empty)
  const [search, setSearch] = useState('')

  const { data: customers, isLoading } = useQuery({
    queryKey: ['customers'],
    queryFn: async () => (await api.get<Customer[]>('/customers')).data,
  })

  const filtered = (customers || []).filter((c) =>
    !search || c.name?.toLowerCase().includes(search.toLowerCase()) || c.nuit?.includes(search) || c.code?.toLowerCase().includes(search.toLowerCase())
  )

  const saveMutation = useMutation({
    mutationFn: async (data: FormData) => {
      if (editing) {
        return (await api.put(`/customers/${editing.id}`, data)).data
      }
      return (await api.post('/customers', data)).data
    },
    onSuccess: () => {
      toast.success(editing ? 'Cliente actualizado' : 'Cliente criado')
      qc.invalidateQueries({ queryKey: ['customers'] })
      setEditing(null)
      setCreating(false)
      setForm(empty)
    },
    onError: (err: any) => toast.error(err.response?.data?.error || err.response?.data || 'Erro ao gravar'),
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/customers/${id}`),
    onSuccess: () => {
      toast.success('Cliente removido')
      qc.invalidateQueries({ queryKey: ['customers'] })
    },
    onError: () => toast.error('Não foi possível remover (pode haver vendas)'),
  })

  function startEdit(c: Customer) {
    setEditing(c)
    setForm({
      code: c.code || '',
      name: c.name || '',
      nuit: c.nuit || '',
      type: c.type || 'B2B',
      contact: c.contact || '',
      address: c.address || '',
      creditLimit: c.creditLimit || 0,
      defaultDiscount: c.defaultDiscount || 0,
    })
    setCreating(true)
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name) { toast.error('Nome é obrigatório'); return }
    if (form.nuit && !/^\d{9}$/.test(form.nuit)) { toast.error('NUIT deve ter 9 dígitos'); return }
    saveMutation.mutate(form)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <Users className="h-8 w-8" /> Clientes
          </h1>
          <p className="text-muted-foreground">{(customers || []).length} cliente(s) registado(s)</p>
        </div>
        <Button onClick={() => { setEditing(null); setCreating(true); setForm(empty) }}>
          <Plus className="h-4 w-4" /> Novo Cliente
        </Button>
      </div>

      {creating && (
        <Card>
          <CardHeader>
            <CardTitle>{editing ? `Editar: ${editing.name}` : 'Novo Cliente'}</CardTitle>
            <CardDescription>Dados do cliente (NUIT validado com módulo 11)</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>Código</Label>
                  <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} placeholder="C001" />
                </div>
                <div className="space-y-2">
                  <Label>NUIT <span className="text-xs text-muted-foreground">(9 dígitos)</span></Label>
                  <Input value={form.nuit} onChange={(e) => setForm({ ...form, nuit: e.target.value.replace(/\D/g, '').slice(0, 9) })} placeholder="123456789" />
                </div>
                <div className="space-y-2">
                  <Label>Tipo</Label>
                  <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })} className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm">
                    <option value="B2B">B2B — Empresa</option>
                    <option value="B2C">B2C — Particular</option>
                    <option value="GOVERNMENT">Governo</option>
                    <option value="EXPORT">Exportação</option>
                  </select>
                </div>
                <div className="space-y-2 md:col-span-3">
                  <Label>Nome *</Label>
                  <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <Label>Contacto</Label>
                  <Input value={form.contact} onChange={(e) => setForm({ ...form, contact: e.target.value })} placeholder="email@empresa.co.mz ou telefone" />
                </div>
                <div className="space-y-2">
                  <Label>Limite de Crédito (MT)</Label>
                  <Input type="number" min={0} value={form.creditLimit} onChange={(e) => setForm({ ...form, creditLimit: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="space-y-2 md:col-span-3">
                  <Label>Endereço</Label>
                  <Input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
                </div>
                <div className="space-y-2">
                  <Label>Desconto Padrão (%)</Label>
                  <Input type="number" min={0} max={100} step={0.1} value={form.defaultDiscount} onChange={(e) => setForm({ ...form, defaultDiscount: parseFloat(e.target.value) || 0 })} />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="outline" type="button" onClick={() => { setEditing(null); setCreating(false); setForm(empty) }}>
                  <X className="h-4 w-4" /> Cancelar
                </Button>
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
            <Input placeholder="Pesquisar por nome, NUIT ou código..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" />
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">A carregar...</div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">Sem clientes. Crie o primeiro!</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Código</TableHead>
                  <TableHead>Nome</TableHead>
                  <TableHead>NUIT</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Contacto</TableHead>
                  <TableHead className="text-right">Saldo</TableHead>
                  <TableHead className="text-right">Acções</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell className="font-mono">{c.code || '—'}</TableCell>
                    <TableCell className="font-medium">{c.name}</TableCell>
                    <TableCell className="text-sm">{formatNuit(c.nuit) || '—'}</TableCell>
                    <TableCell><Badge variant="secondary">{c.type || 'B2B'}</Badge></TableCell>
                    <TableCell className="text-sm text-muted-foreground">{c.contact || '—'}</TableCell>
                    <TableCell className="text-right">{formatMZN(c.balance)}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" onClick={() => startEdit(c)}><Edit className="h-4 w-4" /></Button>
                        <Button variant="ghost" size="icon" onClick={() => {
                          if (confirm(`Remover "${c.name}"?`)) deleteMutation.mutate(c.id!)
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