import { Link } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { User, ShieldCheck, ArrowLeft, Edit, Trash2 } from 'lucide-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { useState } from 'react'

type Role = { id?: number; name: string; description?: string; permissions: string[] }

export function ProfilesPage() {
  const qc = useQueryClient()
  const { data: roles, isLoading } = useQuery<Role[]>({
    queryKey: ['roles'],
    queryFn: async () => (await api.get('/roles')).data,
  })

  const { data: catalog } = useQuery<string[]>({
    queryKey: ['permissions-catalog'],
    queryFn: async () => (await api.get<string[]>('/permissions/catalog')).data,
  })

  const saveRole = useMutation({
    mutationFn: async (r: Role) => {
      return r.id ? (await api.put(`/roles/${r.id}`, r)).data : (await api.post('/roles', r)).data
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['roles'] })
  })

  const deleteRole = useMutation({
    mutationFn: async (id: number) => { await api.delete(`/roles/${id}`) },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['roles'] })
  })

  const [editing, setEditing] = useState<Role | null>(null)
  const [form, setForm] = useState<Role | null>(null)

  function openEdit(r?: Role) {
    setEditing(r ? r : { name: '', permissions: [] })
    setForm(r ? { ...r, permissions: [...(r.permissions || [])] } : { name: '', permissions: [] })
  }

  function save() {
    if (!form) return
    saveRole.mutate(form)
    setEditing(null)
  }

  if (isLoading) return <div>A carregar...</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <User className="h-8 w-8" />
            Perfis de Utilizador
          </h1>
          <p className="text-muted-foreground">Gerencie as funções e permissões da sua equipa.</p>
        </div>
        <Link to="/settings" className="flex items-center gap-2 px-3 py-2 rounded bg-gray-100"> <ArrowLeft className="h-4 w-4" /> Voltar às Configurações</Link>
      </div>

      <div className="overflow-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr>
              <th className="sticky left-0 bg-white z-10 border p-2 text-left">Permissão</th>
              {roles?.map(r => <th key={r.id} className="border p-2 text-left">{r.name}</th>)}
            </tr>
          </thead>
          <tbody>
            {(catalog || []).map((perm) => (
              <tr key={perm} className="align-top">
                <td className="border p-2 font-mono text-xs w-64">{perm}</td>
                {roles?.map((r) => {
                  const checked = (r.permissions || []).includes(perm)
                  return (
                    <td key={r.id} className="border p-2 text-center">
                              <input type="checkbox" checked={checked} onChange={() => {
                              const updated: Role = { ...r, permissions: [...(r.permissions || [])] }
                              if (checked) {
                                updated.permissions = updated.permissions.filter((p) => p !== perm)
                              } else {
                                updated.permissions.push(perm)
                              }
                              saveRole.mutate(updated)
                            }} />
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div>
        <Button onClick={() => openEdit()} className="mt-4">Adicionar perfil</Button>
      </div>

      {editing && form && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white p-6 rounded-lg w-full max-w-xl">
            <h3 className="text-lg font-semibold mb-4">{editing.id ? 'Editar Perfil' : 'Novo Perfil'}</h3>
            <div className="space-y-3">
              <div>
                <label className="text-sm">Nome</label>
                <input className="w-full border p-2 rounded" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </div>
              <div>
                <label className="text-sm">Descrição</label>
                <input className="w-full border p-2 rounded" value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })} />
              </div>
              <div>
                <label className="text-sm">Permissões (uma por linha, ex: VENDAS:CREATE)</label>
                <textarea className="w-full border p-2 rounded h-32" value={(form.permissions || []).join('\n')} onChange={e => setForm({ ...form, permissions: e.target.value.split('\n').map(s => s.trim()).filter(Boolean) })} />
              </div>
              <div className="flex gap-2 justify-end">
                <Button variant="secondary" onClick={() => setEditing(null)}>Cancelar</Button>
                <Button onClick={save}>Guardar</Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
