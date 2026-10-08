# Relatório de validação · 08/10/2026 · v1.1.0

## Verificações concluídas nesta entrega

- Backend: `mvn verify` passou com **14 testes**, sem falhas, erros ou testes ignorados. Banco H2 2.3 em modo de compatibilidade PostgreSQL; Java 17 e Maven 3.9.9.
- Frontend: os **2 testes** de transformação/seleção passaram. `npm ci` e `npm run build` concluíram; React 19 e Vite 7. A compilação de produção foi verificada depois das alterações de navegação.
- Empacotamento Java: `mvn package -DskipTests` criou o artefato da versão 1.1.0 após a verificação funcional. O Dockerfile referencia esse mesmo nome e versão.
- Fontes gráficas: os quatro PDFs do pacote são idênticos aos anexos desta solicitação por comparação SHA-256. Os 65 códigos do Iara extraídos do texto do PDF coincidem com os códigos iniciais. O teste de integridade dos mapas verifica códigos únicos, polígonos dentro da planta, área geométrica e ausência de sobreposição significativa.
- Navegador: Chromium 133 com servidor Java real e banco H2 descartável. Foram exercitados login de administrador e dois expositores, visualização das feiras, orçamento, reserva, fila, liberação com promoção automática, contratação com aceite, comprovante, cancelamento, edição de nome e fonte com persistência no SVG, união de vizinhos e liberação do conjunto, criação e exclusão administrativas, restrição administrativa e restrição do comprovante ao titular. Troca de pavilhão com resposta atrasada foi conferida. O celular de 390 × 844 pixels foi conferido sem transbordamento horizontal. O registro das verificações está em `VALIDACAO_BROWSER.json`.
- PDF: manual criado a partir das instruções atualizadas, renderizado e inspecionado visualmente. As imagens da interface usam dados fictícios de homologação.

## Regras corrigidas e verificadas

1. Reservas, união, novas filas e contratações exigem feira aberta para comercialização.
2. Liberação e saída de fila continuam possíveis quando a feira está fechada.
3. A fila de um espaço liberado durante fechamento fica preservada, sem começar um novo prazo de preferência. Na reabertura, ela é processada antes de permitir a reserva pública.
4. Atribuição administrativa não pode ultrapassar uma fila já existente.
5. Código, área, geometria e conferência de um estande reservado/contratado não podem ser alterados sem tratar antes a ocupação. Nome, fonte e observações continuam editáveis.
6. A visão operacional é exibida inicialmente e ao entrar no editor, para mostrar as informações salvas.
7. Atualizações atrasadas de outra planta ou de uma conta anterior são descartadas; seleção e dados privados são limpos na mudança de usuário.

## Limites efetivos

Os testes desta entrega não foram executados contra PostgreSQL real. A instalação de um servidor PostgreSQL neste ambiente não ficou disponível. O workflow incluído cria PostgreSQL 17 descartável no GitHub Actions e executa o backend contra esse banco; o proprietário deve conferir o resultado antes da abertura ao público. Não use credenciais de produção nos testes.

Não foi executado build Docker, deploy no Render, domínio, HTTPS real, restauração de backup ou teste de carga nesta entrega. O código foi compilado e os fluxos foram testados em servidor HTTP local. A implantação e a homologação no destino estão descritas no manual.

A carga inicial tem 313 objetos: 49 no Vitória Régia, 65 no Iara, 103 no primeiro andar da Navalshore e 96 no segundo. Há 101 registros sinalizados para conferência: 5 no primeiro andar e os 96 do segundo. Os contornos são editáveis e derivados das referências; os PDFs da Navalshore são imagens. A reprodução visual original é preservada, mas a correspondência exata de todos os contornos e metragens precisa ser conferida pela organização, especialmente no segundo andar.

Preços e condições reais não foram fornecidos; feiras e plantas iniciam fechadas para comercialização/conferência. Ocupações impressas iniciam bloqueadas sem inventar contas ou contratos anteriores. Dados fictícios dos testes estão apenas nas capturas, nunca na carga inicial.

O pacote anterior da NN Logística foi consultado. O repositório específico NN não ficou acessível no GitHub conectado. O link do protótipo Claude não retornou conteúdo acessível. Não houve alteração nem migração automática do sistema antigo.

Contratação registra orçamento e aceite, com comprovante imprimível; não integra pagamento, emissão fiscal, assinatura externa, e-mail/WhatsApp ou Google Planilhas.
