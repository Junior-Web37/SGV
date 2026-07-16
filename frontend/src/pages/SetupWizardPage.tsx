import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Receipt, CheckCircle2, ChevronRight, ChevronLeft, Building2, ShieldCheck, Settings2, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'

const steps = [
  { id: 'company', title: 'A sua Empresa', icon: Building2, description: 'Dados básicos para a facturação' },
  { id: 'at', title: 'Certificação AT', icon: ShieldCheck, description: 'Número de certificado e licença' },
  { id: 'options', title: 'Preferências', icon: Settings2, description: 'Série inicial e comportamento' },
]

export function SetupWizardPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({
    companyName: '',
    companyNuit: '',
    companyAddress: '',
    companyPhone: '',
    companyEmail: '',
    softwareCertNumber: '',
    licenseNumber: '',
    defaultSeries: 'A',
    initialDocumentNumber: 1,
  })

  function update(field: string, value: string | number) {
    setForm({ ...form, [field]: value })
  }

  function canProceed(): boolean {
    if (step === 0) return form.companyName.length > 0 && /^\d{9}$/.test(form.companyNuit)
    if (step === 1) return true // AT é opcional mas recomendado
    return true
  }

  async function submit() {
    setLoading(true)
    try {
      await api.post('/setup/wizard', form)
      toast.success('Configuração guardada! Pode agora iniciar sessão.')
      navigate('/login')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Erro ao guardar configuração')
    } finally {
      setLoading(false)
    }
  }

  const Icon = steps[step].icon

  return (
    <div className="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-emerald-50 p-4 py-8">
      <div className="max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 text-primary font-bold text-2xl mb-2">
            <Receipt className="h-7 w-7" />
            SGV
          </div>
          <p className="text-muted-foreground">Configuração inicial — Passo {step + 1} de {steps.length}</p>
        </div>

        {/* Progress */}
        <div className="flex items-center justify-center mb-8">
          {steps.map((s, i) => (
            <div key={s.id} className="flex items-center">
              <div className={`flex items-center justify-center w-10 h-10 rounded-full border-2 ${
                i < step ? 'bg-primary border-primary text-primary-foreground' :
                i === step ? 'border-primary text-primary' : 'border-muted text-muted-foreground'
              }`}>
                {i < step ? <CheckCircle2 className="h-5 w-5" /> : <s.icon className="h-4 w-4" />}
              </div>
              {i < steps.length - 1 && (
                <div className={`h-0.5 w-16 mx-2 ${i < step ? 'bg-primary' : 'bg-muted'}`} />
              )}
            </div>
          ))}
        </div>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Icon className="h-6 w-6 text-primary" />
              <div>
                <CardTitle>{steps[step].title}</CardTitle>
                <CardDescription>{steps[step].description}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {step === 0 && (
              <>
                <div className="space-y-2">
                  <Label>Nome da empresa *</Label>
                  <Input value={form.companyName} onChange={(e) => update('companyName', e.target.value)} placeholder="Ex: Mercearia Sol Nascente, Lda." />
                </div>
                <div className="space-y-2">
                  <Label>NUIT * <span className="text-xs text-muted-foreground">(9 dígitos)</span></Label>
                  <Input value={form.companyNuit} onChange={(e) => update('companyNuit', e.target.value.replace(/\D/g, ''))} maxLength={9} placeholder="123456789" />
                </div>
                <div className="space-y-2">
                  <Label>Endereço</Label>
                  <Input value={form.companyAddress} onChange={(e) => update('companyAddress', e.target.value)} placeholder="Av. 25 de Setembro, Maputo" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Telefone</Label>
                    <Input value={form.companyPhone} onChange={(e) => update('companyPhone', e.target.value)} placeholder="+258 84 123 4567" />
                  </div>
                  <div className="space-y-2">
                    <Label>Email</Label>
                    <Input type="email" value={form.companyEmail} onChange={(e) => update('companyEmail', e.target.value)} placeholder="geral@empresa.co.mz" />
                  </div>
                </div>
              </>
            )}

            {step === 1 && (
              <>
                <p className="text-sm text-muted-foreground">
                  Estes dados são fornecidos pela Autoridade Tributária quando o seu software é certificado.
                  Se ainda não os possui, pode preencher mais tarde em Configurações.
                </p>
                <div className="space-y-2">
                  <Label>Número do Certificado AT</Label>
                  <Input value={form.softwareCertNumber} onChange={(e) => update('softwareCertNumber', e.target.value)} placeholder="Ex: 123/AGT/2024" />
                </div>
                <div className="space-y-2">
                  <Label>Número da Licença</Label>
                  <Input value={form.licenseNumber} onChange={(e) => update('licenseNumber', e.target.value)} placeholder="Ex: LIC-2024-001" />
                </div>
              </>
            )}

            {step === 2 && (
              <>
                <p className="text-sm text-muted-foreground">
                  A série inicial identifica a sequência dos seus documentos fiscais.
                </p>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Série inicial</Label>
                    <Input value={form.defaultSeries} onChange={(e) => update('defaultSeries', e.target.value.toUpperCase())} maxLength={2} />
                  </div>
                  <div className="space-y-2">
                    <Label>Número inicial</Label>
                    <Input type="number" min={1} value={form.initialDocumentNumber} onChange={(e) => update('initialDocumentNumber', parseInt(e.target.value) || 1)} />
                  </div>
                </div>
              </>
            )}

            <div className="flex justify-between pt-4">
              <Button variant="outline" onClick={() => setStep(Math.max(0, step - 1))} disabled={step === 0}>
                <ChevronLeft className="h-4 w-4" /> Anterior
              </Button>
              {step < steps.length - 1 ? (
                <Button onClick={() => setStep(step + 1)} disabled={!canProceed()}>
                  Próximo <ChevronRight className="h-4 w-4" />
                </Button>
              ) : (
                <Button onClick={submit} disabled={loading}>
                  {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> A guardar...</> : 'Concluir'}
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}