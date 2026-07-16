import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Plus, Edit, Trash2, Save, X } from 'lucide-react'
import { toast } from 'sonner'

type Role = { id?: number; name: string }
type User = { id?: number; username: string; fullName?: string; email?: string; roleIds?: number[] }

export function UsersPage() {
  const qc = useQueryClient()
  const { data: users } = useQuery<User[]>({ queryKey: ['users'], queryFn: async () => (await api.get('/users')).data })
  const { data: roles } = useQuery<Role[]>({ queryKey: ['roles'], queryFn: async () => (await api.get('/roles')).data })
  const { data: permissionsCatalog } = useQuery<string[]>({ queryKey: ['permissionsCatalog'], queryFn: async () => (await api.get('/permissions/catalog')).data })

  const createUser = useMutation({
    mutationFn: async (u: any) => (await api.post('/users', u)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] })
  })

  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<User & { permissions?: string[] }>({ username: '', fullName: '', email: '', roleIds: [], permissions: [] })

  function openNew() { setForm({ username: '', fullName: '', email: '', roleIds: [] }); setCreating(true) }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.username || !form.roleIds || form.roleIds.length === 0) { toast.error('Username e pelo menos um perfil são obrigatórios'); return }
    try {
      await createUser.mutateAsync({ username: form.username, password: 'changeme', fullName: form.fullName, roleIds: form.roleIds || [], permissions: form.permissions || [] })
      toast.success('Utilizador criado')
      setCreating(false)
    } catch (err: any) { toast.error(err.response?.data?.error || 'Erro') }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2"><Plus className="h-8 w-8" /> Utilizadores</h1>
          <p className="text-muted-foreground">Crie e atribua perfis no momento da criação.</p>
        </div>
        <Button onClick={openNew}><Plus className="h-4 w-4" /> Novo Utilizador</Button>
      </div>

      <Card>
        <CardHeader><CardTitle>Utilizadores</CardTitle></CardHeader>
        <CardContent>
          <table className="w-full">
            <thead>
              <tr><th>Username</th><th>Nome</th><th>Email</th><th>Perfis</th></tr>
            </thead>
            <tbody>
              {(users || []).map(u => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{(u.roleIds || []).map(id => roles?.find(r=>r.id===id)?.name).join(', ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>

      {creating && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white p-6 rounded-lg w-full max-w-md">
            <h3 className="text-lg font-semibold mb-4">Novo Utilizador</h3>
            <form onSubmit={submit} className="space-y-3">
              <div>
                <Label>Username</Label>
                <Input value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} />
              </div>
              <div>
                <Label>Nome completo</Label>
                <Input value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} />
              </div>
              <div>
                <Label>Email</Label>
                <Input value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
              </div>
              <div>
                <Label>Perfis (seleccione no mínimo 1)</Label>
                <div className="flex flex-col gap-1 max-h-40 overflow-auto border p-2">
                  {(roles || []).map(r => (
                    <label key={r.id} className="flex items-center gap-2">
                      <input type="checkbox" checked={(form.roleIds||[]).includes(r.id!)} onChange={e => {
                        const set = new Set(form.roleIds || [])
                        if (e.target.checked) set.add(r.id!) ; else set.delete(r.id!)
                        setForm({ ...form, roleIds: Array.from(set) })
                      }} /> {r.name}
                    </label>
                  ))}
                </div>
              </div>
              <div>
                <Label>Permissões (opcional)</Label>
                <div className="flex flex-col gap-2 max-h-40 overflow-auto border p-2">
                  {(() => {
                    const groups: Record<string, string[]> = {};
                    (permissionsCatalog || []).forEach(p => {
                      const parts = p.split(':');
                      const page = parts[0] || 'GENERAL';
                      groups[page] = groups[page] || [];
                      groups[page].push(p);
                    });
                    return Object.entries(groups).map(([page, perms]) => (
                      <div key={page} className="mb-1">
                        <div className="text-sm font-semibold">{page}</div>
                        <div className="flex flex-wrap gap-2 mt-1">
                          {perms.map(p => (
                            <label key={p} className="flex items-center gap-2 bg-gray-50 px-2 py-1 rounded">
                              <input type="checkbox" checked={(form.permissions||[]).includes(p)} onChange={e => {
                                const set = new Set(form.permissions || [])
                                if (e.target.checked) set.add(p) ; else set.delete(p)
                                setForm({ ...form, permissions: Array.from(set) })
                              }} />
                              <span className="text-xs">{p.split(':')[1] || p}</span>
                            </label>
                          ))}
                        </div>
                      </div>
                    ));
                  })()}
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="secondary" onClick={() => setCreating(false)}>Cancelar</Button>
                <Button type="submit">Criar</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
