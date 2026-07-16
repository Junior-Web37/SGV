import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatMZN(value: number | undefined | null): string {
  if (value == null) return "0,00 MT"
  return new Intl.NumberFormat('pt-MZ', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value) + " MT"
}

export function formatNuit(nuit: string | undefined | null): string {
  if (!nuit || nuit.length < 9) return nuit || ""
  return `${nuit.substring(0, 3)} ${nuit.substring(3, 6)} ${nuit.substring(6)}`
}

export function formatDate(date: string | Date | undefined | null): string {
  if (!date) return ""
  const d = typeof date === "string" ? new Date(date) : date
  return d.toLocaleDateString('pt-PT', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function formatDateTime(date: string | Date | undefined | null): string {
  if (!date) return ""
  const d = typeof date === "string" ? new Date(date) : date
  return d.toLocaleString('pt-PT', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}