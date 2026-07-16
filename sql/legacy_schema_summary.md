# SGV Legacy Database Schema — Análise

Este documento resume a estrutura do banco de dados legado em `docs/sistema_vendas.db` e o mapeamento para o novo modelo 3FN.

## Tabelas principais no legado

- `usuarios`: contas de acesso com campos de nível, grupo e sucursal.
- `grupos_usuarios`: perfis/roles com `role_name` e `permissions_json`.
- `sucursais`: matriz de filiais com NUIT e contactos.
- `clientes`: cadastro de clientes com limite de crédito, saldo e dados de contato.
- `produtos`: catálogo com stock, preços, IVA, códigos de barras e controle de produção.
- `produtos_codigos_barras`: códigos de barras adicionais.
- `categorias`: classificação de produtos.
- `fornecedores`: cadastro de fornecedores.
- `compras`, `itens_compra`: entrada de estoque com documento de compra.
- `vendas`, `itens_venda`: documentos de venda com imposto, desconto, pagamento e relações de anulação.
- `pagamentos`: pagamentos feitos, incluindo vendas e clientes.
- `sessoes_caixa`, `movimentos_caixa`: abertura/fecho de caixa e movimentos.
- `estoque_sucursal`, `movimentos_estoque`, `movimentos_stock`: controle de estoque por filial e movimentos.
- `audit_logs`: registro de ações do sistema.
- `configuracoes`: parâmetros gerais.
- `despesas`: despesas operacionais.
- `fichas_tecnicas`, `ordens_producao`, `producao_realidades`, `notificacoes_producao`: funcionalidade de produção / restaurante.
- `mesas`, `notificacoes_producao`: módulos de restaurante/mesa que devem ser excluídos do escopo PME.
- `sys_custom_scripts`: scripts personalizados do sistema legado.

## Observações de normalização

- O legado mistura dados de cliente e vendas no mesmo registro (`cliente_nome`, `cliente_nuit`, `cliente_endereco`). No modelo 3FN, `customers` é uma entidade independente, e o documento de venda mantém cópias imutáveis dos dados.
- `usuarios` no legado usa `nivel` e `grupo_id`. No modelo 3FN, separei em `roles` e `user_roles` para apoio a múltiplos perfis por utilizador.
- `produtos` no legado contém atributos de produção e cozinha; o novo modelo simplifica em `products`, `product_barcodes`, `categories` e `suppliers`, removendo campos específicos de restaurante que não são prioridade PME.
- `sucursal_id` em tabelas como `vendas`, `compras`, `sessoes_caixa` é texto no legado; no novo modelo uso `branches.id` como chave estrangeira numérica para integridade referencial.
- `role_permissions` no legado armazena `permissions_json` por role; o novo modelo deixa espaço para migrar esta lógica a partir de `Role` + `user_roles` e políticas `@PreAuthorize` no backend.

## Mapeamento chave para 3FN

- `usuarios` → `users`
- `grupos_usuarios` + `role_permissions` → `roles` + `user_roles`
- `clientes` → `customers`
- `produtos` / `produtos_codigos_barras` → `products` / `product_barcodes`
- `categorias` → `categories`
- `fornecedores` → `suppliers`
- `compras` / `itens_compra` → `purchases` / `purchase_items`
- `vendas` / `itens_venda` → `sales` / `sale_items`
- `pagamentos` → `payments`
- `sessoes_caixa` / `movimentos_caixa` → `sessions_cash` / `movements_cash`
- `estoque_sucursal` / `movimentos_estoque` → `stock_branch` / `stock_movements`
- `audit_logs` e `configuracoes` mantidos como entidades separadas.

## Regras importantes para o novo modelo

- `CLIENTE` deve ser um role padrão e também uma entidade `customers` separada para cadastros comerciais.
- `ADMIN`, `GESTOR`, `CAIXA` são roles de acesso do sistema, com permissões granulares implementadas no backend.
- A normalização exige que detalhes imutáveis do documento fiscal sejam persistidos no cabeçalho da venda, não apenas vinculados por chave estrangeira.
- As tabelas de produção e restaurante são consideradas fora do escopo inicial PME, mas a estrutura de base pode ser preservada para expansão futura.
