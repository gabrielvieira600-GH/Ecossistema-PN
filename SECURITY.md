# Acesso e dados

Somente administradores autenticados podem editar plantas, preços e usuários. Cadastros de expositores exigem aprovação. A conta inicial é criada pelas variáveis de ambiente, sem senha fixa no código.

Senhas são armazenadas com bcrypt. Sessões usam tokens aleatórios com hash no banco, cookie HttpOnly, SameSite=Strict e Secure no ambiente HTTPS. A API verifica o usuário ativo a cada requisição. Alterar acesso, perfil ou senha revoga suas sessões. Alterações exigem token CSRF; o navegador consulta um token atualizado antes de enviar cada operação. Há limites de tentativas de acesso e cadastro registrados no banco.

Use HTTPS e mantenha segredos no ambiente do Render ou no arquivo `.env` local ignorado pelo Git. Faça cópias completas do PostgreSQL em local privado. A exportação JSON do painel não substitui uma cópia do banco.

A plataforma registra aceite comercial e histórico, mas não processa pagamento ou assinatura eletrônica externa. As condições cadastradas devem definir o procedimento comercial real. Leia o manual antes de liberar expositores.
