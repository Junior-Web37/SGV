import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Warehouse, StockWarehouse, StockMovement } from '@/lib/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Package, ArrowRightLeft, History } from 'lucide-react'
import { formatMZN } from '@/lib/utils'

export function WarehousePage() {
  const { data: warehouses, isLoading } = useQuery({
    queryKey: ['warehouses'],
    queryFn: async () => (await api.get<Warehouse[]>('/warehouses')).data,
  })

  const selectedWarehouseId = warehouses?.[0]?.id

  const { data: stock = [] } = useQuery({
    queryKey: ['warehouse-stock', selectedWarehouseId],
    enabled: !!selectedWarehouseId,
    queryFn: async () => (await api.get<StockWarehouse[]>(`/warehouses/${selectedWarehouseId}/stock`)).data,
  })

  const { data: movements = [] } = useQuery({
    queryKey: ['warehouse-movements', selectedWarehouseId],
    enabled: !!selectedWarehouseId,
    queryFn: async () => (await api.get<StockMovement[]>(`/warehouses/${selectedWarehouseId}/movements`)).data,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <Package className="h-8 w-8" /> Armazém
        </h1>
        <p className="text-muted-foreground">Produtos recebidos no armazém e histórico de movimentações.</p>
      </div>

      {isLoading ? (
        <div className="text-muted-foreground">A carregar armazém...</div>
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardTitle>Armazéns disponíveis</CardTitle>
              <CardDescription>O fluxo atual é: entrada no armazém → transferência para a loja.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-2">
              {warehouses?.map((warehouse) => (
                <Badge key={warehouse.id} variant={warehouse.id === selectedWarehouseId ? 'default' : 'outline'}>
                  {warehouse.name}
                </Badge>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Package className="h-5 w-5" /> Stock no armazém
              </CardTitle>
              <CardDescription>Saldo disponível para transferir para a loja.</CardDescription>
            </CardHeader>
            <CardContent>
              {stock.length === 0 ? (
                <div className="text-muted-foreground">Ainda não há stock registado neste armazém.</div>
              ) : (
                <div className="space-y-3">
                  {stock.map((item) => (
                    <div key={item.id} className="border rounded-lg p-3 flex items-center justify-between">
                      <div>
                        <div className="font-semibold">{item.product?.name}</div>
                        <div className="text-sm text-muted-foreground">{item.product?.code} • {item.product?.supplier || 'Sem fornecedor'}</div>
                      </div>
                      <div className="text-right">
                        <div className="font-semibold">{item.stockCurrent ?? 0}</div>
                        <div className="text-xs text-muted-foreground">Saldo actual</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <History className="h-5 w-5" /> Histórico de movimentos
              </CardTitle>
              <CardDescription>Entradas, saídas e transferências associadas ao armazém.</CardDescription>
            </CardHeader>
            <CardContent>
              {movements.length === 0 ? (
                <div className="text-muted-foreground">Ainda não há movimentos registados.</div>
              ) : (
                <div className="space-y-3">
                  {movements.map((movement) => (
                    <div key={movement.id} className="border rounded-lg p-3">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <div className="font-semibold">{movement.product?.name}</div>
                          <div className="text-sm text-muted-foreground">{movement.type} • {movement.subtype || 'sem subtipo'}</div>
                        </div>
                        <Badge variant={movement.qty && movement.qty < 0 ? 'destructive' : 'secondary'}>
                          {movement.qty ?? 0}
                        </Badge>
                      </div>
                      <div className="mt-2 text-sm text-muted-foreground flex flex-wrap gap-4">
                        <span>Antes: {movement.stockBefore ?? 0}</span>
                        <span>Depois: {movement.stockAfter ?? 0}</span>
                        <span>Preço custo: {formatMZN(movement.unitCostPrice)}</span>
                      </div>
                      {movement.notes && <div className="mt-2 text-sm">{movement.notes}</div>}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}
