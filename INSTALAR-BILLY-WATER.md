# BILLY WATER — Instalar / Usar (2 cliques)

O BILLY WATER (SGF) está em `billywater/` e é uma aplicação JavaFX. Para **descarregar
e usar**, escolhe o caminho que preferes:

---

## 🚀 Opção A — Descarregar o instalador pronto (o mais simples)

Este é o caminho de **“dois cliques e usar”** de verdade:

1. Abre a página **Releases** do repositório e descarrega o **`billywater-5.0.0.msi`**
   (instalador) ou o **`billywater-5.0.0.zip`** (portátil).
2. Faz **duplo clique** no `.msi` para instalar (ou descompacta o `.zip`).
3. Duplo clique no atalho **BILLY WATER** que fica no Ambiente de Trabalho.

> ⚠️ Este instalador é gerado pelo **GitHub Actions** (fica pronto após correr o
> workflow `.github/workflows/build.yml`). Se ainda não houver Release, usa a Opção B.

---

## 🔧 Opção B — Clonar/descarregar o código e correr na tua máquina

**Pré-requisitos** (só uma vez): **JDK 17+** (Temurin: https://adoptium.net/) e **Apache Maven**
(https://maven.apache.org/).

Depois de teres a pasta do projecto:

1. Faz **duplo clique** em **`BILLY-WATER.bat`**.
   - Se não houver jar de produção, o script chama o Maven e **compila/abre o sistema**
     automaticamente (a primeira vez demora a descarregar dependências).
   - O sistema arranca no perfil **TESTE** (base de dados H2 local, em `billywater/data/`),
     sem precisar de servidor de base de dados.

2. Para gerar o **instalador Windows** na tua máquina, faz **duplo clique** em
   **`COMPILAR-BILLY-WATER.bat`** → produz `target\billywater-5.0.0.msi` e `.zip`.

3. Para criar um **atalho no Ambiente de Trabalho**, faz duplo clique em
   **`ATALHO-BILLY-WATER.bat`**.

## Login

- **admin / admin** (Administrador)
- (ou os utilizadores que criares em *Configurações → Utilizadores*)

## Licença

- A aplicação **verifica** a licença com a chave pública (`billywater/src/main/resources/licenca/publica.pem`).
- As licenças são **assinadas** pelo sistema separado **`gerador-licencas`** (contém a chave privada e **não faz parte do SGF**).
- Em *Configurações → Licença → Activar licença (.lic)* importas o `.lic` gerado.

## Base de dados

- **Perfil TESTE (predefinição):** H2 local e cifrada em `billywater/data/billywater.mv.db` — zero configuração.
- **Perfil REAL (produção):** PostgreSQL. Edita `billywater/src/main/resources/config.properties` com
  `billywater.perfil=REAL` e os dados `real.bd.*`.

## Nota sobre compilar localmente

Este ambiente de trabalho **não tem JDK/Maven** e não consegue descarregar dependências
(Maven Central bloqueado). A **compilação de verdade** acontece no **GitHub Actions** ou na
tua máquina com JDK 17 + Maven. O código está completo; a validação final é feita aí.
