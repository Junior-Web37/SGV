import { useEffect, useMemo, useRef, useState } from 'react'
import { Printer, X, FileDown, ExternalLink, Loader2, Receipt, FileText } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { cn, formatMZN } from '@/lib/utils'
import { api } from '@/lib/api'
import type { Sale } from '@/lib/types'

export type PrintFormat = 'thermal-80mm' | 'a4' | 'a5'

interface Props {
  sale: Sale | null
  open: boolean
  onClose: () => void
  /** Formato inicial. Se não for passado, o componente escolhe o padrão AT conforme o tipo de documento. */
  defaultFormat?: PrintFormat
  /** Título do cabeçalho (opcional). */
  title?: string
}

/**
 * Devolve o formato AT-padrão para um tipo de documento:
 *  - TV (Talão de Venda) / RC (Recibo) → 80mm térmica
 *  - FA (Factura) / NC (Nota Crédito) / ND (Nota Débito) / cotação → A4
 */
export function defaultFormatFor(sale: Sale | null | undefined): PrintFormat {
  if (!sale) return 'thermal-80mm'
  const t = (sale.documentType || '').toUpperCase()
  if (t === 'TV' || t === 'RC') return 'thermal-80mm'
  return 'a4'
}

const FORMATS: Array<{ key: PrintFormat; label: string; hint: string; atHint: string }> = [
  { key: 'thermal-80mm', label: 'Térmica 80mm', hint: 'Recibo / Talão', atHint: 'AT-padrão para TV e RC' },
  { key: 'a4',           label: 'A4',           hint: 'Factura / Cotação', atHint: 'AT-padrão para FA e Cotação' },
  { key: 'a5',           label: 'A5',           hint: 'Compacto',          atHint: 'Alternativa' },
]

/**
 * Modal de preview do recibo/factura. Mostra o documento dentro de um iframe
 * com altura limitada à viewport (sem espaço em branco extra) e expõe botões
 * visíveis para:
 *  - escolher o formato (80mm / A4 / A5)
 *  - imprimir (window.print dentro do iframe)
 *  - descarregar PDF
 *  - abrir numa nova aba
 */
export function ReceiptPreviewModal({ sale, open, onClose, defaultFormat, title }: Props) {
  const initialFormat = defaultFormat ?? defaultFormatFor(sale)
  const [format, setFormat] = useState<PrintFormat>(initialFormat)
  const [loading, setLoading] = useState(false)
  const [pdfLoading, setPdfLoading] = useState(false)
  const iframeRef = useRef<HTMLIFrameElement>(null)
  const wrapperRef = useRef<HTMLDivElement>(null)

  // Re-sincroniza o formato quando o sale muda
  useEffect(() => {
    if (sale) {
      setFormat(defaultFormat ?? defaultFormatFor(sale))
    }
  }, [sale, defaultFormat])

  const htmlUrl = useMemo(() => {
    if (!sale) return ''
    return `/api/print/html/${sale.id}?format=${format}&_t=${Date.now()}`
  }, [sale, format])

  // Recarrega o iframe quando o formato muda
  useEffect(() => {
    const f = iframeRef.current
    if (!f) return
    setLoading(true)
    f.src = htmlUrl
  }, [htmlUrl])

  // Faz o iframe "ouvir" window.print e ajustar altura (fit-to-viewport)
  useEffect(() => {
    const f = iframeRef.current
    if (!f) return
    const onLoad = () => {
      setLoading(false)
      try {
        // Ajusta altura do iframe ao conteúdo (até o limite da viewport)
        const doc = f.contentDocument
        if (!doc) return
        const body = doc.body
        const html = doc.documentElement
        const contentHeight = Math.max(body.scrollHeight, html.scrollHeight)
        // Limita à altura da viewport menos 280px (toolbar + header + botões)
        const maxH = Math.max(280, window.innerHeight - 280)
        const finalH = Math.min(contentHeight, maxH)
        f.style.height = finalH + 'px'
      } catch {
        // cross-origin, ignora
      }
    }
    f.addEventListener('load', onLoad)
    return () => f.removeEventListener('load', onLoad)
  }, [sale, format])

  if (!open || !sale) return null

  function doPrint() {
    const f = iframeRef.current
    if (!f || !f.contentWindow) return
    f.contentWindow.focus()
    f.contentWindow.print()
  }

  function openInNewTab() {
    if (!sale) return
    window.open(htmlUrl, '_blank')
  }

  async function downloadPdf() {
    if (!sale) return
    setPdfLoading(true)
    try {
      // PDF direto pelo backend
      const endpoint = format === 'thermal-80mm' ? '/api/print/thermal/' : '/api/print/a4/'
      const res = await api.get(endpoint + sale.id, { responseType: 'blob' })
      const blob = new Blob([res.data], { type: 'application/pdf' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      const dt = sale.documentType || 'DOC'
      a.href = url
      a.download = `${dt}_${sale.series || 'A'}_${sale.documentNumber || sale.id}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (err) {
      // fallback: abre a página HTML numa nova aba — o utilizador pode usar "Imprimir → Guardar como PDF"
      window.open(htmlUrl, '_blank')
    } finally {
      setPdfLoading(false)
    }
  }

  const docType = sale.documentType || 'DOC'
  const docTitle = title || `${docType} ${sale.series || ''}/${sale.documentNumber || sale.id}`

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-0 sm:p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="relative bg-white w-full max-w-4xl h-full sm:h-auto sm:max-h-[95vh] flex flex-col rounded-none sm:rounded-lg shadow-2xl overflow-hidden"
        onClick={(e) => e.stopPropagation()}
        ref={wrapperRef}
      >
        {/* Cabeçalho com info do documento */}
        <div className="flex items-center justify-between gap-3 px-4 py-3 border-b bg-slate-50">
          <div className="flex items-center gap-2 min-w-0">
            {format === 'thermal-80mm'
              ? <Receipt className="h-5 w-5 text-emerald-700 flex-shrink-0" />
              : <FileText className="h-5 w-5 text-emerald-700 flex-shrink-0" />}
            <div className="min-w-0">
              <div className="font-semibold text-sm truncate">{docTitle}</div>
              <div className="text-xs text-slate-500 truncate">
                {sale.customerName || 'Consumidor Final'} • {formatMZN(sale.total)}
                {sale.state === 'ANULADA' && (
                  <Badge variant="destructive" className="ml-2">ANULADA</Badge>
                )}
              </div>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} title="Fechar" className="flex-shrink-0">
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Barra de formatos — visível e destacada */}
        <div className="px-4 py-2.5 border-b bg-white flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-slate-500 uppercase tracking-wide mr-1">Formato:</span>
          {FORMATS.map((f) => {
            const isDefault = f.key === defaultFormatFor(sale)
            const active = format === f.key
            return (
              <button
                key={f.key}
                onClick={() => setFormat(f.key)}
                className={cn(
                  "px-3 py-1.5 text-sm rounded-md border transition-colors flex items-center gap-1.5",
                  active
                    ? "bg-emerald-600 text-white border-emerald-600 shadow-sm"
                    : "bg-white text-slate-700 border-slate-300 hover:bg-slate-50",
                )}
                title={`${f.label} — ${f.atHint}`}
              >
                <span className="font-medium">{f.label}</span>
                {isDefault && (
                  <span className={cn(
                    "text-[10px] px-1.5 py-0.5 rounded font-semibold uppercase",
                    active ? "bg-white/20 text-white" : "bg-emerald-100 text-emerald-700"
                  )}>AT</span>
                )}
              </button>
            )
          })}
          <div className="ml-auto flex items-center gap-2 flex-wrap">
            <Button variant="outline" size="sm" onClick={openInNewTab} title="Abrir numa nova aba">
              <ExternalLink className="h-3.5 w-3.5" /> Nova aba
            </Button>
            <Button variant="outline" size="sm" onClick={downloadPdf} disabled={pdfLoading} title="Descarregar PDF">
              {pdfLoading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <FileDown className="h-3.5 w-3.5" />}
              PDF
            </Button>
            <Button size="sm" onClick={doPrint} title="Imprimir">
              <Printer className="h-3.5 w-3.5" /> Imprimir
            </Button>
          </div>
        </div>

        {/* Hint do formato escolhido */}
        <div className="px-4 py-1.5 bg-slate-50 border-b text-xs text-slate-600 flex items-center gap-2">
          {(() => {
            const f = FORMATS.find((x) => x.key === format)!
            return (
              <>
                <span className="font-medium text-slate-700">{f.label}</span>
                <span>•</span>
                <span>{f.hint}</span>
                <span>•</span>
                <span className="text-slate-500">{f.atHint}</span>
              </>
            )
          })()}
        </div>

        {/* Área do preview — sem espaço em branco extra, sem scroll desnecessário */}
        <div className="flex-1 overflow-auto bg-slate-200 p-2 sm:p-4 flex items-start justify-center min-h-0">
          <div className="relative bg-white shadow-md w-full" style={{ maxWidth: format === 'thermal-80mm' ? '80mm' : (format === 'a4' ? '210mm' : '148mm') }}>
            {loading && (
              <div className="absolute inset-0 flex items-center justify-center bg-white/80 z-10">
                <Loader2 className="h-6 w-6 animate-spin text-emerald-600" />
              </div>
            )}
            <iframe
              ref={iframeRef}
              src={htmlUrl}
              title={docTitle}
              className="w-full border-0 block bg-white"
              style={{ height: '600px' }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
