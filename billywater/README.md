# BILLY WATER — SGF (módulo)

Sistema de Gestão de Facturação de Água (SGF) — aplicação desktop em **JavaFX**, com
**skin azul claro** (linhagem SGV). Módulo Maven independente dentro do repositório SGV.

> ⚠️ Este módulo **verifica** licenças com a chave pública. A **geração** de licenças é um
> **sistema separado** — veja `/home/user/gerador-licencas` (fora daqui, contém a chave privada).

---

## Stack

- JavaFX 21 (controls / graphics / base)
- H2 (perfil **TESTE**, cifrada AES) e PostgreSQL (perfil **REAL**)
- OpenPDF (relatórios PDF/CSV e talões REC)
- Ikonli + Atlantafx (ícones/skin)
- Maven (fat-jar `com.billywater.BillyWaterApp`) + perfil `dist` (instalador Windows)

## Estrutura

```
com.billywater
├── BillyWaterApp           # arranque (JavaFX)
├── config/                 # Config, ConfigBanco, Licenca, MigracaoBanco
├── domain/                 # POJOs (Cliente, Contrato, Factura, Produto, ...)
├── dao/                    # BaseDAO + DAOs específicos (CRUD e SQL)
├── servico/                # ServicoFacturacao, ServicoCaixa, ServicoLoja,
│                           # ServicoArmazens, ServicoRelatorios, GeradorPDF, ...
├── ui/                     # Ui, FormDialog, Sessao, Permissoes, ComboItem, ...
└── views/                  # BaseView + todas as telas (§3: Painel, Água, Loja, Rede,
                            # Relatórios, Configurações, Logs)
```

## Menu (estrutura §3)

`⌂ Painel` · `Água ▾` (Clientes, Contadores, Zonas, Leituras, Facturação, Tarifário, Dívidas, Caixa, Recibos) ·
`Loja ▾` (Loja, Stock, Armazéns, Transferências, Serviços, Fornecedores, Compras) ·
`Rede ▾` (Rede/NRW) · `Relatórios ▾` (15 relatórios) · `Configurações ▾` · `🛡️ Logs`.

## Compilar / executar (desenvolvimento)

```bash
cd billywater
mvn clean javafx:run          # correr localmente (perfil TESTE → H2)
mvn clean package             # gera o fat-jar billywater-5.0.0.jar
java -jar target/billywater-5.0.0.jar
```

## Instalador Windows (GitHub Actions)

No CI (`.github/workflows/build.yml`):

```bash
mvn -Pdist clean package
```

Gera o instalador **`.msi`** e o pacote portátil **`.zip`** com JRE incluído (via
`javapackager-maven-plugin`), publicados como artefactos.

## Licença

- **Verificação** (SGF): `src/main/resources/licenca/publica.pem` — RSA-SHA256, embebida aqui.
- **Geração** (sistema separado): `gerador-licencas` (fora deste módulo) — chave privada.

O SGF só consegue **verificar**; não tem como assinar. Isto garante que alterações aos campos da
licença invalidam a assinatura.
