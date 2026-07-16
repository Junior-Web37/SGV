# SGV — Projeto Inicial

Scaffold inicial para o Sistema de Gestão de Vendas (SGV) destinado a PME em Moçambique.

Rápido para começar:

1. Instalar dependências:

```bash
npm install
```

2. Criar ficheiro de ambiente (opcional):

```bash
cp .env.example .env
```

3. Inicializar base de dados (executa o schema SQL):

```bash
# arranque o servidor e aceda a http://localhost:3000/init-db
npm start
```

4. Endpoints disponíveis (inicial):

- `GET /health` — verifica estado da API
- `POST /init-db` — inicializa/cria as tabelas no ficheiro de DB configurado

Notas:

- O esquema contido em `migrations/schema.sql` é um ponto de partida; ajuste conforme os requisitos fiscais e de negócio.
- Este scaffold usa SQLite por defeito para instalações locais; pode ser adaptado para PostgreSQL para deployments multi-sucursal.
## 5. Java scaffold

O directório `java-sgv` contém um scaffold Spring Boot com:

- autenticação JWT
- roles: `ADMIN`, `GESTOR`, `CAIXA`, `CLIENTE`
- entidades JPA normalizadas em 3FN
- endpoints de autenticação e gestão de utilizadores
- H2 em memória para desenvolvimento

Para usar, abra `java-sgv/README.md`.
