import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { AppConfig, SeriesStatusResponse } from '@/lib/types'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertIcon } from '@/components/ui/alert'
import { Settings, Save, Database, AlertCircle, FileText, Power } from 'lucide-react'
import { toast } from 'sonner'

export function SettingsPage() {
  const qc = useQueryClient()

  const { data: config, isLoading } = useQuery({
    queryKey: ['app-config'],
    queryFn: async () => (await api.get<AppConfig>('/setup')).data,
  })

  const { data: series } = useQuery({
    queryKey: ['series-status'],
    queryFn: async () => (await api.get<SeriesStatusResponse>('/series/status')).data,
  })

  const [form, setForm] = useState<Partial<AppConfig>>({})

  function update(field: keyof AppConfig, value: unknown) {
    setForm({ ...form, [field]: value })
  }

  async function saveFlags() {
    try {
      await api.put('/setup/flags', form)
      toast.success('Configurações guardadas')
      qc.invalidateQueries({ queryKey: ['app-config'] })
      qc.invalidateQueries({ queryKey: ['compliance'] })
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Erro')
    }
  }

  async function backupNow() {
    try {
      const res = await api.post<{ path: string; sizeBytes: number }>('/backup/now')
      toast.success(`Backup criado: ${Math.round(res.data.sizeBytes / 1024)} KB`)
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Erro no backup')
    }
  }

  function downloadSnapshot() {
    window.open('/api/backup/snapshot', '_blank')
  }

  if (isLoading || !config) return <div className="text-center py-12">A carregar...</div>

  const current = { ...config, ...form }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <Settings className="h-8 w-8" />
          Configurações
        </h1>
        <p className="text-muted-foreground">Parâmetros da loja e do sistema</p>
      </div>

      {/* Empresa */}
      <Card>
        <CardHeader>
          <CardTitle>Dados da Empresa</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid grid-cols-2 gap-4">
            <div><Label>Nome</Label><Input value={current.companyName || ''} disabled /></div>
            <div><Label>NUIT</Label><Input value={current.companyNuit || ''} disabled /></div>
            <div><Label>Certificado AT</Label><Input value={current.softwareCertNumber || ''} disabled /></div>
            <div><Label>Licença</Label><Input value={current.licenseNumber || ''} disabled /></div>
          </div>
          <p className="text-xs text-muted-foreground">Para alterar estes dados, contacte o administrador (re-Setup Wizard).</p>
        </CardContent>
      </Card>

      {/* Flags operacionais */}
      <Card>
        <CardHeader>
          <CardTitle>Comportamento do Sistema</CardTitle>
          <CardDescription>Flags operacionais que afectam o dia-a-dia</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between p-3 border rounded-md">
            <div className="flex items-center gap-3">
              <Power className="h-5 w-5" />
              <div>
                <div className="font-medium">Modo Demonstração</div>
                <div className="text-xs text-muted-foreground">Documentos de teste (não contam para relatórios oficiais)</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={!!current.demoMode}
              onChange={(e) => update('demoMode', e.target.checked)}
              className="h-5 w-5"
            />
          </div>

          <div className="flex items-center justify-between p-3 border rounded-md">
            <div className="flex items-center gap-3">
              <Database className="h-5 w-5" />
              <div>
                <div className="font-medium">Backup Automático</div>
                <div className="text-xs text-muted-foreground">mysqldump diário às 03:00, retenção 7 dias</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={!!current.autoBackupEnabled}
              onChange={(e) => update('autoBackupEnabled', e.target.checked)}
              className="h-5 w-5"
            />
          </div>

          <div className="flex items-center justify-between p-3 border rounded-md">
            <div className="flex items-center gap-3">
              <AlertCircle className="h-5 w-5" />
              <div>
                <div className="font-medium">Detecção de Duplicação</div>
                <div className="text-xs text-muted-foreground">Janela: {current.duplicateWindowSeconds || 60}s</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={!!current.duplicateDetectionEnabled}
              onChange={(e) => update('duplicateDetectionEnabled', e.target.checked)}
              className="h-5 w-5"
            />
          </div>

          <div className="flex items-center justify-between p-3 border rounded-md">
            <div className="flex items-center gap-3">
              <FileText className="h-5 w-5" />
              <div>
                <div className="font-medium">Modo Offline</div>
                <div className="text-xs text-muted-foreground">Permite emitir sem validação AT em tempo real</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={!!current.offlineModeEnabled}
              onChange={(e) => update('offlineModeEnabled', e.target.checked)}
              className="h-5 w-5"
            />
          </div>

          <Button onClick={saveFlags}>
            <Save className="h-4 w-4" /> Guardar Alterações
          </Button>
        </CardContent>
      </Card>

      {/* Séries */}
      {series && (
        <Card>
          <CardHeader>
            <CardTitle>Séries Documentais</CardTitle>
            <CardDescription>{series.totalSeries} série(s) em uso</CardDescription>
          </CardHeader>
          <CardContent>
            {series.alerts.length > 0 && (
              <Alert variant="warning" className="mb-4">
                <AlertIcon variant="warning" />
                <AlertDescription>
                  <ul className="list-disc pl-4">
                    {series.alerts.map((a, i) => <li key={i}>{a.message}</li>)}
                  </ul>
                </AlertDescription>
              </Alert>
            )}
            <div className="space-y-2">
              {series.series.map((s) => (
                <div key={`${s.year}-${s.series}-${s.documentType}`} className="flex items-center justify-between p-3 border rounded-md">
                  <div>
                    <div className="font-medium">{s.series}/{s.documentType}/{s.year}</div>
                    <div className="text-xs text-muted-foreground">#{s.current} de {s.limit}</div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm">{s.usage}</span>
                    <Badge variant={s.status === 'exhausted' ? 'destructive' : s.status === 'warning' ? 'warning' : 'success'}>
                      {s.status === 'exhausted' ? 'Esgotada' : s.status === 'warning' ? 'A esgotar' : 'OK'}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Backup */}
      <Card>
        <CardHeader>
          <CardTitle>Backup</CardTitle>
          <CardDescription>Faça backup dos seus dados a qualquer momento</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex gap-2">
            <Button onClick={backupNow}>
              <Database className="h-4 w-4" /> Fazer Backup Agora
            </Button>
            <Button variant="outline" onClick={downloadSnapshot}>
              <FileText className="h-4 w-4" /> Descarregar Snapshot (ZIP)
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">Os backups ficam guardados em ~/Documents/SGV/backups/</p>
        </CardContent>
      </Card>
    </div>
  )
}