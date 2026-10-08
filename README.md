# Portal Navalshore e NN Logística 2027 · v1.1.0

Aplicação Java 17 / Spring Boot 3.5 + React 19 + PostgreSQL, com implantação Docker em um único serviço.

**Comece pelo manual:** `docs/MANUAL_IMPLANTACAO.md` e `docs/MANUAL_IMPLANTACAO.pdf`.

Inclui as quatro plantas originais, 313 polígonos de estandes, editor administrativo, login com aprovação de cadastros, orçamento, reserva com prazo, união de vizinhos, fila FIFO, contratação com aceite e comprovante, notificações internas, histórico e exportação.

O PDF original é a referência visual integral. A visão “Situação atual” mostra a camada operacional editável. Dados provisórios estão em `docs/ESTANDES_PARA_CONFERIR.csv`; o segundo andar da Navalshore exige conferência administrativa antes de publicar. Preços reais devem ser configurados no painel.

## Rodar localmente

1. Instale Docker Desktop.
2. Copie `.env.example` para `.env` e preencha senha do banco, e-mail e senha do administrador.
3. Execute `docker compose up --build -d`.
4. Abra `http://localhost:8080`.

## Implantar

Crie um novo repositório, coloque os arquivos desta pasta na raiz e use o Blueprint `render.yaml`. O Blueprint contém planos pagos persistentes; consulte os valores na tela do Render. Use um banco novo para esta aplicação. As tabelas e os dados iniciais são criados via Flyway e carga inicial idempotente.

## Desenvolvimento e testes

- Backend: `cd backend && mvn verify` (Java 17, Maven 3.9).
- Frontend: `cd frontend && npm ci && npm test && npm run build` (Node 22).
- PostgreSQL: defina `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD` antes de executar `mvn verify`.
- Frontend local: `npm run dev`, com o backend em `localhost:8080`.

Ações administrativas são verificadas no servidor. Concorrência usa transações e bloqueio por feira; alterações usam versões. Orçamentos aceitos são armazenados como snapshots imutáveis.

Pagamento, assinatura externa, e-mail/WhatsApp e integração com Google Planilhas não estão conectados nesta entrega. “Contratar” registra o orçamento e o aceite na plataforma, sem cobrar cartão. As regras e o procedimento de cancelamento devem constar das condições reais cadastradas.

O sistema anterior foi consultado por meio do pacote `NN-Reservas-Estandes-2027-v3-editor-completo.zip`. O repositório NN não ficou disponível no acesso GitHub conectado e o link do Claude retornou 403.

## Revisão de 8 de outubro de 2026

Comercialização fechada bloqueia reservas, união, fila e contratação. Filas existentes aguardam a reabertura sem perder sua prioridade. Alterações estruturais de estandes ocupados são recusadas; nome e fonte permanecem editáveis. Navegação e atualização descartam respostas de outra planta ou de uma conta anterior. A visão operacional é a inicial. Consulte o relatório da validação realizada nesta entrega.
