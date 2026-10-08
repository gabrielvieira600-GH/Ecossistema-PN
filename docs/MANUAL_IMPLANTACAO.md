# Manual de implantação e operação

## Portal Navalshore e NN Logística · edição 2027

Entrega de 8 de outubro de 2026. Versão 1.1.0. Código Java 17/Spring Boot, React e PostgreSQL. O ZIP contém o projeto completo; não depende de um projeto no Claude para funcionar.

## 1. O que está pronto e o que configurar

O portal oferece login, cadastros sujeitos à aprovação, duas feiras com duas plantas cada, navegação com zoom, busca, orçamento, reserva, contratação com aceite, liberação, união de estandes adjacentes, fila de preferência e notificações internas. Administradores podem editar código, nome exibido, metragem, dimensões, posição, contorno e fonte, adicionar/excluir estandes, aprovar usuários, configurar preços e consultar histórico.

As quatro imagens de referência foram renderizadas dos PDFs enviados. Os PDFs originais também estão no pacote. A camada interativa tem 313 espaços:

- NN Logística / Vitória Régia: 49 estandes.
- NN Logística / Iara: 65 estandes.
- Navalshore / 1º andar: 103 estandes.
- Navalshore / 2º andar: 96 estandes.

O PDF do segundo andar tem menor resolução. Seus 96 registros e cinco registros do primeiro andar estão marcados para conferência; consulte `ESTANDES_PARA_CONFERIR.csv`. Os contornos são uma camada derivada editável, cuja correspondência e metragens devem ser homologadas pela organização. A imagem original permanece como referência fiel. Não se afirma que os dados provisórios são uma planta técnica certificada.

Preços, taxas e condições reais não foram fornecidos. Reservas, novas entradas na fila e contratações começam desabilitadas até sua configuração. Todas as plantas começam em conferência. Não há preço fictício publicado nem senha padrão pronta para uso.

“Contratar” armazena o orçamento aceito, a empresa, os estandes, as condições, data e IP. Não realiza cobrança, assinatura eletrônica externa, emissão fiscal ou integração com e-mail/WhatsApp. O comprovante pode ser impresso ou salvo como PDF pelo navegador. Notificações são vistas dentro do portal.

Este é um novo sistema e deve começar em um novo banco. Não reutilize o banco do sistema anterior. O pacote anterior da NN foi consultado como inspiração, mas contas, reservas e contratos antigos não foram migrados automaticamente. As ocupações impressas nos PDFs foram preservadas como ocupações existentes sem inventar titulares cadastrados.

## 2. Conhecer o pacote

- `backend/`: servidor, regras, segurança, migração e testes.
- `frontend/`: interface React e testes de seleção/transformação.
- `backend/src/main/resources/static/maps/`: imagens de referência, base operacional e PDFs.
- `backend/src/main/resources/data/maps.json`: carga inicial das quatro plantas.
- `Dockerfile` e `docker-entrypoint.sh`: compilação e execução conjunta da aplicação.
- `render.yaml`: criação do serviço e banco no Render.
- `compose.yaml` e `.env.example`: execução local com Docker.
- `.github/workflows/ci.yml`: verificação no GitHub, incluindo PostgreSQL 17.
- `docs/`: este manual, pendências, relatório de validação e imagens da interface.

Arquivos `.env`, senhas, `node_modules`, `target`, `dist` e cópias do banco não devem ser enviados ao GitHub. A aplicação compilada será gerada no Render a partir do código. Os arquivos de nomes iniciados por ponto incluídos no ZIP são necessários e devem ser mantidos.

## 3. Subir para um novo repositório GitHub

### Caminho com GitHub Desktop

1. Extraia o ZIP. Abra a pasta `feira-suite`; dentro dela devem aparecer diretamente `Dockerfile`, `render.yaml`, `backend` e `frontend`.
2. Instale o GitHub Desktop e entre na conta proprietária do novo repositório.
3. Use File > New repository. Crie um repositório chamado, por exemplo, `navalshore-nn-portal` em uma pasta vazia do computador.
4. Copie todo o conteúdo de `feira-suite` para a raiz dessa pasta nova, incluindo `.github`, `.gitignore`, `.dockerignore` e `.env.example`. Não crie outra pasta `feira-suite` dentro do repositório.
5. No GitHub Desktop, revise a lista de arquivos. Faça o primeiro commit com a mensagem `Portal Navalshore e NN Logística`.
6. Clique Publish repository. Prefira repositório privado e escolha corretamente a conta/organização proprietária.
7. Abra o repositório no GitHub. Confirme que `Dockerfile` e `render.yaml` aparecem na raiz. A aba Actions deve iniciar a verificação; aguarde o resultado.

### Alternativa com terminal

Execute dentro da pasta extraída, substituindo a URL pelo novo repositório vazio criado no GitHub. Use o gerenciador de credenciais ou SSH; não coloque token no comando nem nos arquivos.

```bash
git init
git add .
git commit -m "Portal Navalshore e NN Logistica"
git branch -M main
git remote add origin https://github.com/SUA_CONTA/SEU_REPOSITORIO.git
git push -u origin main
```

Se preferir envio pelo site do GitHub, faça upload de toda a estrutura e confira também os arquivos ocultos. O GitHub Desktop reduz o risco de perder pastas e o workflow. Não substitua o repositório antigo em funcionamento para esta primeira implantação.

## 4. Implantar no Render com o Blueprint

O `render.yaml` solicita um serviço Docker com 1 CPU/2 GB (`1c-2g`) e PostgreSQL com 0,1 CPU/256 MB (`0.1c-256mb`), ambos em Ohio. São recursos pagos. Confira os valores, armazenamento, backups e condições exibidos na sua conta antes de criar. Aumente a capacidade do banco conforme usuários e carga reais. O pacote não foi implantado nem criou cobrança em suas contas.

1. Entre no Render e abra New > Blueprint.
2. Conecte o GitHub e autorize o acesso ao novo repositório. Se privado, a instalação do Render precisa ter acesso a ele.
3. Selecione o repositório e a branch `main`. Use o arquivo `render.yaml` da raiz.
4. Revise os dois recursos apresentados: `navalshore-nn-portal` e `navalshore-nn-db`.
5. Preencha `ADMIN_EMAIL` com o e-mail do administrador responsável. Preencha `ADMIN_PASSWORD` com uma senha exclusiva de pelo menos 12 caracteres, até 72 bytes em UTF-8. Não use senhas de exemplo deste manual.
6. Confirme a criação e acompanhe os logs de build e execução.
7. O build executa `npm ci`, compila a interface e executa `mvn verify`. Em seguida gera a imagem de execução. Não há comando de build ou start extra a preencher no caminho Blueprint.
8. O banco é conectado pela rede interna. O Blueprint preenche automaticamente host, nome do banco, usuário e senha. O entrypoint monta a URL JDBC; nenhuma senha fica no código.
9. Aguarde o serviço ficar Live. Abra a URL HTTPS mostrada pelo Render e faça login com os dados do passo 5.
10. A URL `/api/health` deve devolver `{"status":"UP"}`. Essa verificação consulta o banco.

Na primeira execução, Flyway cria as tabelas e o sistema importa as plantas. Reinícios e atualizações não sobrescrevem as edições existentes. O administrador inicial só é criado se ainda não houver administrador ativo; mudar a variável `ADMIN_PASSWORD` depois não redefine sua senha.

As variáveis do Blueprint são:

- `PORT=10000`: porta do servidor no Render.
- `SECURE_COOKIE=true`: cookie da sessão enviado por HTTPS.
- `REGISTRATION_OPEN=true`: permite solicitação de novos cadastros.
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`: criação inicial do administrador.
- `DATABASE_HOST`, `DATABASE_NAME`: referências ao PostgreSQL.
- `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`: credenciais do PostgreSQL.

Guarde o acesso administrativo em um gerenciador de senhas. Crie um segundo administrador pela interface. Você pode remover `ADMIN_PASSWORD` do ambiente após a criação inicial; se iniciar futuramente um banco vazio, precisará fornecê-la novamente. Não remova as credenciais do banco.

### Alternativa: criar os recursos manualmente

1. Crie um Render Postgres 17 persistente na região Ohio, com banco `feiras` e usuário `feiras`.
2. Crie um Web Service conectado ao novo repositório, ambiente Docker, mesma região, Dockerfile `./Dockerfile`, contexto/raiz na raiz do projeto.
3. Use um plano com memória suficiente para Java, como o previsto no Blueprint. Configure health check `/api/health`.
4. Defina `SPRING_DATASOURCE_URL` com o formato abaixo, usando o hostname interno real do seu banco. Configure usuário e senha em variáveis separadas.

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST_INTERNO:5432/feiras
SPRING_DATASOURCE_USERNAME=USUARIO_REAL
SPRING_DATASOURCE_PASSWORD=SENHA_REAL_DO_BANCO
PORT=10000
SECURE_COOKIE=true
REGISTRATION_OPEN=true
ADMIN_EMAIL=SEU_EMAIL
ADMIN_PASSWORD=SUA_SENHA_EXCLUSIVA
```

A URL `postgresql://...` fornecida pelo Render não é uma URL JDBC. Não a cole inteira em `SPRING_DATASOURCE_URL`; acrescente o formato `jdbc:postgresql://` e mantenha usuário/senha separados. No caminho Blueprint, essa conversão já está automatizada. Não configure ao mesmo tempo dois bancos por variáveis conflitantes.

### Usar um PostgreSQL novo na Neon

Se preferir a Neon, crie um projeto e banco novos para este portal. Use a conexão pooled com TLS. No Web Service Docker do Render, configure a URL JDBC sem usuário ou senha dentro dela:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST-POOLER.neon.tech:5432/NOME_DO_BANCO?sslmode=require
SPRING_DATASOURCE_USERNAME=USUARIO_DA_NEON
SPRING_DATASOURCE_PASSWORD=SENHA_DA_NEON
```

Mantenha as variáveis do administrador, PORT e SECURE_COOKIE. Não configure DATABASE_HOST nesse caminho. A URI da Neon começa com postgresql://; adapte somente o protocolo para jdbc:postgresql://, mantenha o host/banco e informe o usuário e a senha originais separadamente. Use a tela Connect da Neon como fonte desses valores. Não faça o Blueprint criar um segundo banco se escolher essa implantação manual.

### Domínio próprio

Após validar a URL do Render, use Settings > Custom Domains, adicione o domínio desejado e siga os registros DNS indicados pelo Render. Aguarde emissão do HTTPS antes de divulgar. A interface e a API usam o mesmo domínio; não é necessário configurar dois serviços ou CORS para esta entrega.

## 5. Preparar a operação antes de convidar expositores

### Configurar condições comerciais

Abra Administração > Condições comerciais. Cada feira possui sua própria configuração:

- Preço do espaço por metro quadrado.
- Montagem por metro quadrado, opcional no orçamento.
- Taxas fixas por contratação, cobradas uma vez no conjunto selecionado.
- Tributos adicionais em porcentagem do subtotal.
- Prazo de reserva em horas: inicialmente 48.
- Preferência da fila em horas: inicialmente 24.
- Condições de pagamento, itens incluídos, exclusões, cancelamento e validade.

Use valores reais e descreva tudo que constitui a contratação. Se o preço já inclui tributos, deixe tributos adicionais em zero. Se houver cobrança que não se enquadre nesses componentes, adapte a regra no código antes de publicar; não esconda uma cobrança nas condições sem refletir no total.

Marque Publicar preços e abrir comercialização e salve. A publicação exige preço de espaço positivo e condições preenchidas. Isso não substitui a revisão da planta. Sem publicação, o orçamento aparece como preliminar e reservas, novas entradas na fila e contratações são recusadas. É possível liberar uma reserva ou sair de uma fila já existente.

Exemplo apenas para entender a fórmula: 20 m² a R$ 100/m², montagem de R$ 20/m² e taxa de R$ 50 resultam em subtotal de R$ 2.450. Com tributos adicionais de 5%, o total é R$ 2.572,50. Esses valores não são os preços das feiras e não estão cadastrados.

### Conferir as quatro plantas

1. Escolha a feira e o pavilhão/andar.
2. Compare a opção PDF original com a visão Situação atual. O botão PDF original no topo abre o arquivo original.
3. Clique Editar planta. Selecione um estande e use Editar estande; duplo clique também abre o editor no modo de edição.
4. Confira código, área comercial, dimensões, contorno e nome. Ajuste quando necessário.
5. Marque Conferi código, metragem e contorno com o PDF e salve.
6. Repita para as pendências do CSV. No segundo andar da Navalshore, revise os 96 registros; uma ocupação sem código legível está identificada como `RESERVADO-SN` e precisa receber o código correto.
7. Ao concluir a revisão do pavilhão, clique Publicar planta conferida. O servidor impede a publicação enquanto houver estandes ativos sem área ou sem conferência.
8. Faça uma última inspeção dos estandes já marcados conferidos, incluindo áreas e nomes, e teste uma operação antes de divulgar.

Os códigos agrupados com barra representam espaços já unidos no desenho de origem. A revisão técnica deve verificar se devem continuar como uma única unidade comercial. A largura/altura visual do editor não converte automaticamente pixels em metros. Informe a área comercial real separadamente.

### Vincular ocupações do PDF

Os espaços com empresas ou reservas já impressas aparecem como Ocupação existente, para não disponibilizar indevidamente uma área ocupada. O nome original é conservado no histórico da origem.

Peça ao expositor que se cadastre; aprove a conta. Depois, abra o editor do estande e use Atribuir reserva a uma empresa. Essa ação cria uma reserva temporária em nome do cadastro escolhido; não cria um contrato antigo. A empresa poderá contratar pelas condições publicadas.

Se a ocupação impressa já não existir, o administrador pode alterar Ocupação existente para Disponível no editor. Se houver fila, a preferência será oferecida antes de disponibilizar publicamente. Para contratos antigos que precisam constar formalmente, será necessário um trabalho de migração específico; este pacote não inventa valores ou aceites anteriores.

### Aprovar usuários e administradores

Em Administração > Usuários, aprove os cadastros após conferir empresa e contato. Novos cadastros começam sem acesso. O perfil Expositor não recebe comandos de edição. Para uma pessoa da organização, selecione Administrador. Mantenha pelo menos um administrador ativo; o sistema protege o último administrador.

Desativar, alterar perfil ou redefinir senha encerra as sessões do usuário. A recuperação de senha é feita pela organização nesse painel, pois o envio de e-mail não foi integrado. Para suspender novas solicitações de cadastro, altere `REGISTRATION_OPEN=false` no Render e reimplante.

## 6. Jornada do expositor e regras da fila

1. Na tela de acesso, solicite o cadastro da empresa e aguarde aprovação.
2. Entre com e-mail e senha. Escolha a feira e um dos dois pavilhões/andares.
3. Use busca por código ou empresa, filtro de ocupação, zoom e Arrastar planta.
4. Clique no espaço desejado. O painel informa área, ocupação e orçamento. Selecione montagem quando quiser incluí-la.
5. Reservar confirma uma reserva temporária. O prazo aparece na sua área. Reserva, união e contratação só ficam disponíveis com dados e preços publicados.
6. Contratar apresenta o orçamento final e as condições. Marque o aceite e confirme. O comprovante fica em Meus estandes e pode ser salvo em PDF.
7. Liberar permite ao expositor liberar suas reservas. Contratações são canceladas pela organização, com motivo registrado.
8. Para unir espaços, mantenha Ctrl, Command ou Shift pressionado ao clicar nos vizinhos e use Reservar e unir estandes. A união exige contato de borda; não permite atravessar corredores. No celular ou no computador, ative Selecionar vários estandes e toque nos polígonos vizinhos para adicionar ou retirar componentes.
9. Para um espaço ocupado, use Entrar na fila. Sua posição é informada; outros nomes da fila não são expostos ao público.
10. Ao liberar ou vencer uma reserva, a primeira empresa elegível da fila recebe uma reserva de preferência com o prazo configurado e uma notificação interna. Se não contratar dentro do prazo, a preferência passa à próxima empresa.

A fila é por estande. Em um conjunto liberado, cada componente segue sua própria fila; a preferência não garante automaticamente o conjunto inteiro. Sair da fila está disponível em Meus estandes. Ao fechar a comercialização, novas reservas e entradas na fila param. Reservas já existentes mantêm o prazo original. Quando um espaço é liberado com a feira fechada, a fila é preservada sem iniciar nova preferência; na reabertura, a primeira empresa elegível recebe a oferta antes de novas reservas públicas. A atribuição administrativa não pode pular uma fila existente. O sistema não envia e-mail ou WhatsApp nessa etapa, portanto a organização deve orientar os expositores a acompanhar o portal.

Uma união mantém os polígonos e a identificação dos componentes. Seus integrantes devem ser operados juntos enquanto a reserva estiver agrupada. Unir uma reserva existente não reinicia nem prolonga seu prazo. Seleções são limitadas a 20 estandes no mesmo pavilhão.

Duas empresas não conseguem confirmar a mesma reserva simultaneamente. Se outra pessoa alterar o espaço ou a tabela de preços, o sistema exige atualização antes da confirmação. O orçamento já contratado permanece armazenado com os valores aceitos, mesmo quando preços ou plantas mudam depois.

## 7. Editor administrativo

A opção Editar planta habilita Adicionar estande, Editar estande e Textos/ruas. O controle de autorização também é feito na API, não apenas na tela.

No editor, ajuste código, metragem, dimensões, nome e fonte. A prévia acompanha as mudanças. A fonte de 2 a 30 unidades é aplicada ao nome na visão Situação atual; o texto impresso na imagem original do PDF permanece inalterado.

Para mover/redimensionar, informe posição X/Y e largura/altura visual ou arraste os vértices laranja. Os polígonos podem ter de 3 a 16 vértices, devem ficar dentro da planta e não podem se cruzar nem sobrepor outros estandes. O campo de vértices aceita pares numéricos como `[[10,10],[30,10],[30,25],[10,25]]`.

Adicionar cria um espaço disponível. Preencha um código único no pavilhão, área comercial, contorno, nome e conferência. Confira a posição em relação à circulação e às exigências físicas da organização; o editor não é um sistema de projeto de segurança do pavilhão.

Excluir retira o espaço da planta operacional, mantendo a identidade no histórico. Só é permitido quando disponível e sem fila. Um código de registro arquivado continua reservado no banco para preservar a rastreabilidade; use outro código ao criar um novo registro.

A visão Situação atual é exibida por padrão e é ativada automaticamente ao entrar em edição. Nome e fonte salvos aparecem nessa visão; PDF original permanece disponível como referência.

Em reservas e contratações ativas, o servidor mantém código, área, contorno e conferência. Nome, fonte e observações podem ser editados. Para alterar a estrutura, libere a reserva ou cancele a contratação primeiro.

A imagem operacional é uma base derivada do desenho inicial: os interiores originais dos estandes foram limpos para receber as novas cores e nomes. Grandes mudanças de arquitetura, paredes ou circulação requerem atualizar a base da planta também. Alterar um polígono não redesenha automaticamente paredes ou textos estáticos do PDF.

Textos/ruas permite adicionar, editar e excluir anotações sobre a planta com posição, fonte e rotação. A imagem original não é reescrita. Alterações administrativas, atribuições, preços, reservas, aceites e cancelamentos ficam registrados no histórico.

Em Administração > Contratações, o cancelamento exige um motivo e libera todos os componentes para suas filas. Combine com o expositor o procedimento financeiro previsto nas condições; o cancelamento no portal não executa estorno bancário.

## 8. Rodar no computador e desenvolver

### Docker local

Instale Docker Desktop. Na raiz do projeto, copie `.env.example` para `.env` e substitua os três campos por credenciais próprias. Mantenha `.env` somente no seu computador.

```bash
docker compose up --build -d
docker compose logs -f app
```

Abra `http://localhost:8080`. O Compose usa cookies sem atributo Secure exclusivamente para esse teste HTTP local. No Render, mantenha `SECURE_COOKIE=true`.

Para parar sem apagar o banco:

```bash
docker compose down
```

O banco local fica no volume `database`. Não execute `docker compose down -v` com dados que precise conservar: a opção `-v` apaga o volume.

### Desenvolvimento sem Docker

Use Java 17, Maven 3.9, Node 22 e PostgreSQL 17. Crie um banco vazio e configure `SPRING_DATASOURCE_URL`, usuário e senha conforme a seção 4. Defina os dados de administrador inicial. No backend execute `mvn spring-boot:run`; em outro terminal, no frontend, `npm ci` e `npm run dev`. O Vite redireciona `/api` e `/maps` para o backend em 8080.

### Verificações

```bash
# Dentro de backend
mvn verify
# Dentro de frontend
npm ci
npm test
npm run build
```

Os testes locais usam H2 em modo de compatibilidade PostgreSQL por padrão. Para testar em um PostgreSQL real de homologação, defina `TEST_DATABASE_URL` em formato JDBC, `TEST_DATABASE_USERNAME` e `TEST_DATABASE_PASSWORD` antes de `mvn verify`. Use um banco descartável de testes, nunca o banco de produção: os testes criam usuários e registros de teste.

O workflow do GitHub cria um PostgreSQL 17 temporário e executa essas verificações. O build Docker também verifica o backend. O relatório incluído informa o que foi efetivamente executado nesta entrega; implantação no Render e teste de carga em produção devem ser feitos na sua conta.

## 9. Atualizações, cópias e restauração

Código e plantas iniciais ficam no GitHub; dados em uso ficam no PostgreSQL. Fazer um commit não salva o banco. Exportar JSON no painel é útil para análise, mas não substitui uma cópia completa: a exportação não contém contas, hashes de senha nem todas as tabelas de autenticação.

Antes de atualizações relevantes, faça uma cópia do PostgreSQL e verifique a disponibilidade das restaurações no plano contratado. Use as ferramentas de backup do Render ou `pg_dump` com uma conexão autorizada. Para uma conexão externa, permita apenas o IP necessário no controle de acesso do banco e remova a exceção ao concluir. O Blueprint começa sem acesso externo liberado.

Exemplo usando a URL PostgreSQL completa em uma variável de ambiente local chamada `DATABASE_URL`, sem gravá-la em arquivo de código:

```bash
pg_dump "$DATABASE_URL" --format=custom --file=backup-feiras.dump
```

Guarde a cópia em local privado e protegido, fora do repositório. Teste a restauração primeiro em um banco vazio de homologação. O comando abaixo é um exemplo para esse destino vazio; a variável deve apontar somente para o banco de restauração:

```bash
pg_restore --dbname="$RESTORE_DATABASE_URL" --no-owner backup-feiras.dump
```

Depois de restaurar, conecte uma instância de homologação ao banco e confirme usuários, plantas, contratos e filas antes de substituir o ambiente ativo. Uma cópia contém dados pessoais e comerciais e deve ter acesso restrito à organização.

Para atualizar o código, faça commit/push no repositório conectado. Se auto deploy estiver habilitado, Render gerará outra imagem. Mantenha o mesmo banco e as mesmas variáveis. Não edite `V1__schema.sql` depois que foi aplicada em produção; crie novas migrações `V2__...sql`, `V3__...sql` para mudanças de schema. A carga inicial só é importada quando ainda não há feiras.

Editar `maps.json` ou trocar PDFs no código de uma aplicação já inicializada não substitui automaticamente os estandes editados no banco. Alterações de geometria devem ser feitas pelo painel ou por migração cuidadosamente preparada. Preserve a consistência entre imagens, dimensões da planta e polígonos.

## 10. Verificação de lançamento

Antes de divulgar o endereço, faça uma operação de homologação com contas de teste e uma seleção deliberadamente escolhida pela organização. Não use estandes ocupados de clientes para teste.

- Confirme as quatro plantas, códigos, áreas e ocupações e publique cada pavilhão.
- Configure as duas tabelas de preços e condições completas.
- Cadastre/aprove um expositor e mantenha outro como expositor, sem perfil administrativo.
- Verifique que apenas administradores editam; teste reserva, união de vizinhos e tentativa de seleção desconectada.
- Com uma segunda conta, entre na fila. Libere a reserva da primeira e confirme a oferta à segunda.
- Contrate um espaço de teste, confira total, aceite e comprovante; cancele com motivo no painel ao concluir o teste.
- Reinicie ou reimplante e confirme persistência dos dados.
- Confirme HTTPS, domínio, acesso administrativo alternativo e uma cópia restaurável do banco.

Não há garantia de desempenho de carga sem medir o volume real. O bloqueio transacional por feira privilegia a integridade das reservas; acompanhe latência e memória e dimensione os planos quando necessário.

## 11. Resolver problemas comuns

**Login não funciona logo após cadastro.** A organização precisa aprovar o cadastro. Administradores iniciais usam as credenciais cadastradas no Render, sem uma senha fixa no código.

**Alterar ADMIN_PASSWORD não mudou a senha.** Essa variável serve somente ao primeiro cadastro em um banco vazio. Use Administração > Usuários para redefinir a senha existente.

**Erro 403 ao salvar.** Pode ser sessão/token de proteção expirado, operação sem perfil administrativo ou acesso por HTTP com cookies seguros. Atualize a página, entre novamente e use a URL HTTPS no Render. Não desative a proteção CSRF.

**Reserva/contratação desabilitada.** Confira publicação da planta, verificação e área dos estandes, titularidade, publicação dos preços e condições. Contratar exige orçamento atualizado e aceite marcado.

**Nome/fonte aparentemente não mudou.** Selecione Situação atual. O modo PDF original preserva o texto impresso no documento.

**Erro de sobreposição ao editar.** Ajuste os vértices para que o contorno não invada o vizinho. Não reduza o controle de integridade apenas para aceitar um desenho incorreto.

**Este registro foi alterado.** Outra sessão ou a expiração da reserva mudou o registro. Recarregue e avalie a situação atual antes de repetir.

**Build não encontra Dockerfile.** Verifique a raiz do repositório. Os arquivos não devem ficar dentro de uma pasta adicional inadvertida. Confirme que o shell `docker-entrypoint.sh` usa quebra de linha LF.

**Build Maven falhou.** Abra o trecho de erro no log. Não ignore os testes; corrija dependência, rede ou falha indicada. `target` não precisa ser enviado ao GitHub.

**Banco indisponível.** Confira host interno, mesma região, usuário, senha e formato JDBC. `/api/health` não ficará UP sem conexão ao banco.

**Feiras apareceram vazias ou outro administrador foi criado.** Confirme se o serviço está conectado ao banco certo. Não recrie o banco para corrigir um problema de credenciais.

**Dados sumiram após recriar recursos.** Dados comerciais pertencem ao PostgreSQL, não ao serviço Docker. Recriá-lo sem restaurar uma cópia inicia uma base nova.

## 12. Referências e limites da entrega

- Documentação Render / Blueprint: https://render.com/docs/blueprint-spec
- Render / Docker: https://render.com/docs/docker
- Render / planos: https://render.com/docs/compute-plans
- Render / PostgreSQL: https://render.com/docs/postgresql-creating-connecting
- GitHub Desktop: https://docs.github.com/en/desktop
- PostgreSQL / backup: https://www.postgresql.org/docs/17/backup-dump.html

A aplicação foi preparada para implantação manual; nenhum recurso foi publicado nas suas contas. O sistema anterior foi consultado pelo ZIP disponível. O repositório GitHub específico da NN não estava disponível no acesso conectado, e o projeto embrionário do Claude retornou HTTP 403. A solução usa os PDFs recebidos e código próprio, sem depender desses acessos.

A validação automatizada e visual está descrita em `RELATORIO_VALIDACAO.md`. A conferência dos dados provisórios é uma etapa necessária para produção, particularmente no segundo andar da Navalshore. Integrações externas e migração formal de contratos anteriores precisam de escopo e credenciais próprios.


## 13. Validação desta versão

Em 8 de outubro de 2026, foram executados 14 testes do backend, incluindo integridade dos mapas, concorrência de reservas, fila e expiração, união, orçamento, contratos, edição, autorização e as correções desta revisão. Todos passaram. Os dois testes do frontend e a compilação de produção também passaram. A versão 1.1.0 foi empacotada pelo Maven.

O navegador foi exercitado com servidor Java real e banco H2 descartável: login, duas contas de expositor, reserva, fila, liberação, contratação com aceite, comprovante, cancelamento, nome e fonte persistidos, união de vizinhos, criação e exclusão de estandes, acesso administrativo recusado ao expositor e troca rápida de pavilhão com uma resposta atrasada. A tela de celular de 390 por 844 pixels foi conferida. As capturas deste manual usam somente dados fictícios de homologação.

Os PDFs incluídos foram comparados por SHA-256 com os quatro anexos desta solicitação e são idênticos. No Iara, os 65 códigos extraídos do texto do PDF coincidem com os 65 códigos iniciais. Essa comparação não certifica todas as áreas ou contornos. Há 101 registros sinalizados para conferência; o segundo andar da Navalshore exige revisão antes de comercializar.

PostgreSQL real, imagem Docker, HTTPS e implantação na sua conta Render não foram executados nesta entrega. O workflow do GitHub executará os testes em PostgreSQL 17, e o manual fornece os passos de homologação no destino. Consulte também RELATORIO_VALIDACAO.md no pacote.
