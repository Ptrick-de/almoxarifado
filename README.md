# Sistema de Almoxarifado

Aplicação de controle de estoque multiempresa, com backend em **Java + Spring Boot** e frontend em **React**. Login por **usuário e senha** (sem e-mail), banco **MySQL**, e sem identidade visual fixa — cada empresa vê apenas seus próprios dados.

## Materiais, Movimentação e Relatórios (versão atual)

O menu lateral tem três áreas principais — **Materiais**, **Movimentação** e **Relatórios** — além do Painel e de Usuários (admin).

- **Materiais** (abas: Materiais · Fornecedores · Categorias): cadastro com código automático, marca, modelo, C.A., fornecedores com preço, botão **Consultar Material** e filtros por código, nome, marca, modelo, C.A., fornecedor e situação.
- **Movimentação** (abas: Visão geral · Criar entrada · Saída para consumo · Empréstimos · Perda ou avaria): cada operação tem tela própria, com histórico filtrável por material, responsável, tipo e período. Os quatro botões antigos (Entrada/Saída/Perda/Avaria) saíram da tela de histórico; o endpoint `POST /api/movimentacoes` continua sendo usado por essas telas.
- **Relatórios**: posição de estoque, movimentações e empréstimos, com filtros, exportação CSV e impressão.
- **Documentos**: *Ficha de Entrega de Material* (após cada saída para consumo) e *Termo de Empréstimo* (na tela de empréstimos). Abrem em página própria; o botão **Imprimir / Salvar em PDF** usa o diálogo de impressão do navegador (escolha "Salvar como PDF"). Não há geração de PDF no servidor.

### Decisões de regra de negócio

| Tema | Decisão |
|---|---|
| Código do material | Gerado pelo sistema (`MAT-001`, `MAT-002`...). O código identifica o **produto**. Se já existe material com o mesmo nome, o novo cadastro reaproveita o código; o botão de nova variação também reaproveita. O código nunca muda depois de criado. |
| Variações | Cada linha de `materiais` é uma **variação** (marca + modelo + C.A.) com estoque próprio. A unicidade é `código + marca + modelo + C.A. + empresa` (`uk_material_variacao`); a chave real é o `id`. Mesmo produto com marcas diferentes mantém o mesmo código. |
| Fornecedores e preços | Relação N:N na tabela `material_fornecedores` (um preço por fornecedor e variação). Uma entrada com fornecedor e preço atualiza esse preço e o preço de referência do material. |
| Saldo e empréstimo | `quantidade_atual` é o que está **disponível no almoxarifado**. Empréstimo subtrai do saldo uma única vez; devolução soma de volta. Total = disponível + emprestado. Não existe conceito de reserva (aparece sempre 0). |
| Situação | Indisponível (cadastro inativo) · Emprestado (saldo 0 e há itens emprestados) · Sem estoque · Estoque baixo (saldo ≤ mínimo) · Disponível. |
| Empréstimo | Data prevista obrigatória; devolução parcial permitida; "Atrasado" é calculado (ativo com data prevista vencida), não gravado. |
| Saldo inicial | Informado só no cadastro (gera uma entrada "saldo inicial" no histórico); depois o saldo só muda por movimentação. |
| Exclusão | Material ou fornecedor com vínculos/histórico não é excluído (mensagem amigável). Para tirar um material de uso, inative-o: o histórico é preservado. |
| Duplicidade | Cada formulário envia uma chave de operação; reenviar a mesma não lança de novo. Operações rodam em transação, com bloqueio do material para evitar saldo negativo em acessos simultâneos. |

### Mudanças no banco (aplicadas automaticamente)

- `materiais`: novas colunas `marca`, `modelo`, `ca`, `ativo`; a unicidade `(codigo, empresa_id)` foi trocada por `uk_material_variacao`.
- Novas tabelas: `material_fornecedores` e `emprestimos`.
- `movimentacoes`: novas colunas `destinatario`, `setor`, `fornecedor_id`, `preco_unitario`, `emprestimo_id`, `saldo_apos`, `chave_idempotencia`; tipos `EMPRESTIMO` e `DEVOLUCAO`; `tipo` vira texto e `observacao` passa a 1000 caracteres.
- Banco já existente: ao iniciar, `MigracaoBanco` remove a unicidade antiga, converte `tipo` de ENUM para texto e copia o fornecedor único de cada material para `material_fornecedores`, sem perder dados. Os exemplos novos do `data.sql` (variações do mesmo código) só entram depois dessa migração, isto é, a partir da segunda inicialização.

---

## Versão anterior: multiempresa e login

- **Multiempresa**: os dados (materiais, categorias, fornecedores, movimentações, usuários) agora pertencem a uma empresa específica. Uma empresa nunca vê ou acessa dados de outra — inclusive tentando adivinhar IDs na URL, a API responde "não encontrado".
- **Login por usuário, não e-mail**: o campo agora é um nome de usuário livre (ex: `joao.silva`), sem precisar ser um e-mail.
- **Sem nome nem logo fixos**: a barra lateral mostra o nome da própria empresa (cadastrado no banco), não uma marca do sistema.

---

## ⚠️ Este é um banco novo — apague o antigo antes de rodar

Como quase toda tabela ganhou uma coluna obrigatória (`empresa_id`), o banco anterior **não é compatível**. Como você só tinha dados de exemplo (não dados reais ainda), o caminho mais simples é recriar o banco do zero:

```sql
DROP DATABASE IF EXISTS almoxarifado;
CREATE DATABASE almoxarifado CHARACTER SET utf8mb4;
```

Rode isso no MySQL antes de subir a aplicação de novo. Ao iniciar, o Spring cria as tabelas novas e o `data.sql` popula com duas empresas de exemplo.

## 1. Primeiro acesso (duas empresas de demonstração)

O `data.sql` já cria duas empresas separadas, cada uma com seu próprio administrador, para você ver o isolamento funcionando na prática:

| Empresa | Usuário | Senha |
|---|---|---|
| Empresa Demonstração 1 | `admin1` | `admin123` |
| Empresa Demonstração 2 | `admin2` | `admin123` |

Faça login com `admin1` numa aba e `admin2` em outra (ou uma anônima) — você vai ver que os materiais, categorias, fornecedores e movimentações são completamente diferentes e independentes entre as duas.

**Renomeie as empresas** para os nomes reais assim que possível:
```sql
UPDATE empresas SET nome = 'Nome Real da Empresa 1' WHERE id = 1;
UPDATE empresas SET nome = 'Nome Real da Empresa 2' WHERE id = 2;
```
O nome mostrado na barra lateral do sistema é lido direto dessa tabela — não precisa mexer em código.

**Troque as senhas dos admins** assim que possível: crie um novo usuário ADMIN com senha definitiva pela tela **Usuários** (dentro de cada empresa) e depois desative ou exclua o `admin1`/`admin2` padrão.

## 2. Como funciona o isolamento entre empresas

- Cada usuário pertence a **exatamente uma empresa** (`empresa_id` na tabela `usuarios`).
- Um administrador só enxerga e gerencia usuários da própria empresa — não existe uma tela para "empresa A" mexer em dados da "empresa B".
- Toda consulta (materiais, categorias, fornecedores, movimentações) é automaticamente filtrada pela empresa do usuário logado — isso acontece no backend, então não depende do frontend "esconder" nada.
- **Adicionar uma terceira empresa**: por enquanto isso é feito direto no banco (não há tela de "criar nova empresa" — como você mencionou apenas duas empresas, não construí essa tela ainda). Para adicionar uma:
  ```sql
  INSERT INTO empresas (nome, ativa) VALUES ('Nome da Nova Empresa', true);
  ```
  Depois, crie o primeiro admin dela também via SQL (peça ajuda se precisar — envolve gerar um hash bcrypt para a senha). Se isso virar algo recorrente, vale a pena eu construir uma tela de "gestão de empresas" para um super-administrador — me avise se for o caso.

## 3. Segredo do JWT — importante antes de usar de verdade

No `application.properties`:
```properties
jwt.secret=${JWT_SECRET:troque-este-segredo-por-uma-chave-longa-e-aleatoria-antes-de-usar-em-producao}
```
Defina a variável de ambiente `JWT_SECRET` com uma chave forte antes de produção:
```bash
# Linux/Mac
export JWT_SECRET=$(openssl rand -base64 48)

# Windows (PowerShell)
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

## 4. Como rodar

**Backend** (Java 17+, Maven, MySQL rodando):
```bash
cd backend
mvn spring-boot:run
```

**Frontend** (Node.js 18+):
```bash
cd frontend
npm install
npm run dev
```

Acesse `http://localhost:5173`.

---

## Estrutura do projeto

```
almoxarifado/
├── backend/     → API REST em Spring Boot (Java 17, Maven, MySQL, Spring Security + JWT, multiempresa)
└── frontend/    → Aplicação React (Vite)
```

## Principais endpoints da API

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Login (`{ usuario, senha }`) → token JWT + dados da empresa |
| GET | `/api/usuarios/eu` | Autenticado | Dados do usuário logado (inclui a empresa) |
| GET/POST/PATCH/DELETE | `/api/usuarios` | **Somente ADMIN**, escopo da própria empresa | Gestão de usuários |
| GET/POST/PUT/DELETE | `/api/materiais` | Autenticado, escopo da própria empresa | CRUD de materiais |
| GET/POST/PUT/DELETE | `/api/categorias` | Autenticado, escopo da própria empresa | CRUD de categorias |
| GET/POST/PUT/DELETE | `/api/fornecedores` | Autenticado, escopo da própria empresa | CRUD de fornecedores |
| GET/POST | `/api/movimentacoes` | Autenticado, escopo da própria empresa | Entradas, saídas, perdas e avarias |
| GET | `/api/dashboard` | Autenticado, escopo da própria empresa | Resumo geral |

Todas as rotas (exceto `/api/auth/login`) exigem `Authorization: Bearer <token>` — o frontend já cuida disso automaticamente.

## Indo para produção

- **HTTPS obrigatório** (nunca rode login/JWT em produção sem TLS).
- **JWT_SECRET forte** via variável de ambiente.
- **Backup do MySQL** agendado.
- Trocar `ddl-auto=update` por `validate` + migrations (Flyway/Liquibase) quando o schema estabilizar.
- Se o número de empresas crescer além de duas com frequência, vale construir uma tela de administração de empresas (hoje é só via SQL).

## Recuperação de senha

O sistema não usa e-mail, então a recuperação funciona assim:

- **Usuário esqueceu a senha:** o administrador da empresa abre **Usuários → Redefinir senha**, define uma nova senha e informa ao usuário.
- **Qualquer usuário logado:** pode trocar a própria senha em **Alterar senha** (menu lateral), informando a senha atual.
- **Administrador esqueceu a própria senha** (e não há outro admin): gere um hash bcrypt de uma nova senha e atualize direto no banco:
  `UPDATE usuarios SET senha = '<hash bcrypt>' WHERE usuario = 'admin1';`
  Dica: mantenha sempre pelo menos dois administradores por empresa.
