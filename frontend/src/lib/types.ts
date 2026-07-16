// Tipos do domínio SGV

export interface User {
  id: number
  username: string
  fullName?: string
  email?: string
  active: boolean
  roles: string[]
}

export interface AppConfig {
  id: number
  companyName?: string
  companyNuit?: string
  companyAddress?: string
  companyPhone?: string
  companyEmail?: string
  softwareCertNumber?: string
  licenseNumber?: string
  defaultSeries: string
  initialDocumentNumber: number
  defaultCurrency: string
  consumerFinalNuit: string
  demoMode: boolean
  autoBackupEnabled: boolean
  offlineModeEnabled: boolean
  duplicateDetectionEnabled: boolean
  duplicateWindowSeconds: number
  thermalPrinterName?: string
  thermalPrinterWidth: number
  setupCompleted: boolean
  setupCompletedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface SetupStatus {
  setupCompleted: boolean
  demoMode: boolean
  companyName?: string
  companyNuit?: string
}

export interface Branch {
  id: number
  name: string
  nuit?: string
  address?: string
  contact?: string
  isHead: boolean
  softwareCertNumber?: string
  licenseNumber?: string
}

export interface Customer {
  id: number
  code?: string
  name: string
  nuit?: string
  type?: string
  address?: string
  contact?: string
  creditLimit?: number
  balance?: number
  defaultDiscount?: number
  fidelityPoints?: number
}

export interface Product {
  id: number
  code?: string
  name: string
  unit?: string
  price?: number
  cost?: number
  priceCost?: number
  priceSale?: number
  priceSaleBulk?: number
  bulkQuantity?: number
  conversionFactor?: number
  profitMargin?: number
  taxRate?: number
  iceRate?: number
  categoryId?: number
  service?: boolean
  stockCurrent?: number
  description?: string
  supplier?: string
  supplierReference?: string
  entryDate?: string
  exitDate?: string
  location?: string
  lastMovementAt?: string
}

export interface Warehouse {
  id: number
  code?: string
  name: string
  address?: string
  contact?: string
  notes?: string
  active?: boolean
}

export interface StockWarehouse {
  id?: number
  warehouse?: Warehouse
  product?: Product
  stockCurrent?: number
  stockMin?: number
  stockMax?: number
}

export interface StockMovement {
  id?: number
  product?: Product
  branch?: Branch
  type?: string
  subtype?: string
  qty?: number
  stockBefore?: number
  stockAfter?: number
  unitCostPrice?: number
  unitSalePrice?: number
  notes?: string
  reference?: string
  createdAt?: string
}

export interface WarehouseTransferItem {
  id?: number
  product?: Product
  quantity?: number
  quantityReceived?: number
}

export interface WarehouseTransfer {
  id?: number
  documentNumber?: number
  series?: string
  status?: string
  warehouse?: Warehouse
  branch?: Branch
  notes?: string
  createdAt?: string
  processedAt?: string
  items?: WarehouseTransferItem[]
}

export interface SaleItem {
  productId?: number
  description?: string
  unit?: string
  qty: number
  unitPrice: number
  discount?: number
  iceRate?: number
  taxRate?: number
  taxType?: string
  motivoInexistTax?: string
}

export interface Payment {
  method: string
  amount: number
  terminalRef?: string
  cardType?: string
}

export interface SaleRequest {
  branchId: number
  customerId?: number
  customerName?: string
  customerNuit?: string
  customerAddress?: string
  paymentMethod?: string
  documentType?: string
  series?: string
  documentDateTime?: string
  originSaleId?: number
  offlineFlag?: boolean
  withholdingTaxRate?: number
  items: SaleItem[]
  payments?: Payment[]
}

export interface Sale {
  id: number
  documentType: string
  series: string
  documentNumber: number
  documentYear: number
  branch?: Branch
  customer?: Customer
  customerName: string
  customerNuit: string
  customerAddress?: string
  subtotal: number
  totalDiscount: number
  totalTax: number
  totalIce: number
  total: number
  currency: string
  exchangeRate: number
  paymentMethod?: string
  state: string
  signatureHash?: string
  hashHash?: string
  hashControl?: number
  qrCode?: string
  reprintCount: number
  pendingSync: boolean
  offlineFlag: boolean
  demoFlag?: boolean
  withholdingTax: number
  withholdingTaxRate: number
  paidAmount: number
  changeAmount: number
  annulReason?: string
  annulDate?: string
  createdAt: string
  items: SaleItemResponse[]
  payments: PaymentResponse[]
}

export interface SaleItemResponse {
  id?: number
  productCode?: string
  description?: string
  unit?: string
  qty: number
  unitPrice: number
  discount: number
  iceRate: number
  taxRate: number
  taxType?: string
  motivoInexistTax?: string
  lineBase: number
  lineDiscount: number
  lineIce: number
  lineTax: number
  lineTotal: number
}

export interface PaymentResponse {
  id?: number
  method: string
  amount: number
  terminalRef?: string
  cardType?: string
  createdAt: string
}

export interface ComplianceSnapshot {
  lastSale: {
    id?: number
    documentType?: string
    series?: string
    documentNumber?: number
    createdAt?: string
    total?: number
    minutesAgo: number
    hint?: string
  }
  pendingSyncCount: number
  today: { count: number; total: number; totalTax: number }
  alerts: Array<{
    level: 'critical' | 'warning' | 'info'
    code: string
    message: string
  }>
  alertCount: number
  branch?: Branch
  demoMode: boolean
}

export interface ReconciliationReport {
  date: string
  branchId?: number
  totalDocuments: number
  emittedCount: number
  annulledCount: number
  totalVendas: number
  totalIva: number
  totalIce: number
  totalDesconto: number
  totalRetencao: number
  porTipoDocumento: Record<string, { count: number; total: number }>
  porTaxaIva: Array<{ taxRate: number; base: number; tax: number; count: number }>
  series: Record<string, { first: number; last: number; count: number }>
  annulled: Array<{ id: number; document: string; reason: string; annulDate: string; total: number }>
  porFormaPagamento: Record<string, number>
  discrepancies: string[]
  hasDiscrepancies: boolean
}

export interface TodayReport {
  from: string
  to: string
  totalSales: number
  annulledCount: number
  totalAmount: number
  totalTax: number
  totalDiscount: number
  averageTicket: number
}

export interface TopProductsReport {
  top: Array<{ code: string; name: string; qtySold: number; revenue: number; transactions: number }>
  limit: number
}

export interface SeriesStatusItem {
  series: string
  documentType: string
  year: number
  current: number
  limit: number
  usage: string
  remaining: number
  status: 'ok' | 'warning' | 'exhausted'
}

export interface SeriesStatusResponse {
  series: SeriesStatusItem[]
  alerts: Array<{ level: string; code: string; message: string; recommendation?: string }>
  totalSeries: number
}

export interface HashHistoryItem {
  id: number
  document: string
  hashHash: string
  hashControl?: number
  signatureHash?: string
  createdAt: string
  total: number
  customerNuit: string
  state: string
}

export interface HashAuditResult {
  totalDocuments: number
  issues: Array<{ level: string; code: string; [k: string]: unknown }>
  issueCount: number
  status: 'clean' | 'issues_found'
}

export interface TaxType { [code: string]: string }
export interface TaxExemptionReason { [code: string]: string }