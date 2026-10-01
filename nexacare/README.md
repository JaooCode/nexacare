# NexaCare — Agendamento e gestão de consultas

**Fábrica:** NexaTech Solutions · **Produto:** NexaCare · **Área:** Saúde
Protótipo funcional para a disciplina de Laboratório de Empreendimentos Inovadores / Projeto de Fábrica de Software.

> Todos os dados do sistema são **fictícios**. Lembretes e notificações são **simulados** (nenhum e-mail, SMS ou WhatsApp real é enviado).

---

## 1. Objetivo e problema resolvido

Clínicas e consultórios ainda organizam consultas por WhatsApp, telefone e planilhas, o que causa conflitos de horário, agenda difícil de visualizar, pacientes que esquecem consultas, dificuldade para remarcar/cancelar, horários vagos por cancelamentos e risco de acesso indevido a dados de pacientes.

O NexaCare centraliza tudo em um sistema web responsivo: agenda visual, agendamento sem conflitos, confirmação, remarcação, cancelamento, lembretes e controle de acesso por perfil, com coleta mínima de dados (LGPD).

## 2. Tecnologias (stack obrigatória — nada além dela)

| Camada | Tecnologia |
|---|---|
| Front-end | HTML5, CSS3, JavaScript (puro, sem framework), Bootstrap 5 + Bootstrap Icons |
| Back-end | Java 17+ (testado com JDK 21), Spring Boot 3.3, API REST |
| Banco de dados | MySQL 8+ |
| Versionamento | Git / GitHub |

Detalhes sobre bibliotecas auxiliares (todas pequenas e sem substituir a stack):

* `spring-security-crypto`: **somente** o BCrypt para hash de senha (o Spring Security completo não é usado).
* Bootstrap e Bootstrap Icons vêm empacotados via Maven (WebJars): o front funciona **offline**, sem CDN.
* `H2`: **apenas no escopo de teste** (testes automatizados). A aplicação executa somente em MySQL.
* Não há React, Vue, Angular, Node.js, PHP, Python, Firebase, MongoDB, Tailwind etc.

## 3. Requisitos para executar

* **JDK 17 ou superior** (recomendado 21)
* **Maven 3.9+** (o IntelliJ IDEA já traz um Maven embutido)
* **MySQL 8+** em execução na porta 3306
* IntelliJ IDEA (opcional, recomendado) e um navegador atual

## 4. Como executar (passo a passo)

### 4.1 Configurar o MySQL

O banco `nexacare` é **criado automaticamente** pela aplicação (`createDatabaseIfNotExist=true`), e as tabelas/dados de teste são criados pelos arquivos `schema.sql` e `data.sql` ao iniciar. Você só precisa informar usuário e senha do **seu** MySQL:

**Opção A — variáveis de ambiente (recomendado, não altera arquivos):**

```bash
# Windows (PowerShell)
$env:DB_USER="root"; $env:DB_PASSWORD="SUA_SENHA_DO_MYSQL"
# Linux/macOS
export DB_USER=root DB_PASSWORD=SUA_SENHA_DO_MYSQL
```

**Opção B — editar** [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.username=root
spring.datasource.password=SUA_SENHA_DO_MYSQL
```

(O padrão, se nada for informado, é usuário `root` e senha `root`.)

**Criar o banco manualmente (opcional):** o script [`banco/nexacare_completo.sql`](banco/nexacare_completo.sql) cria o banco, as tabelas, os relacionamentos e os dados fictícios:

```bash
mysql -u root -p < banco/nexacare_completo.sql
```

### 4.2 Como o Spring Boot se conecta ao MySQL

1. O driver `mysql-connector-j` está no [`pom.xml`](pom.xml).
2. A URL JDBC em `application.properties` (`jdbc:mysql://localhost:3306/nexacare?...`) aponta para o servidor, a porta e o banco.
3. Usuário e senha vêm de `spring.datasource.username/password` (ou das variáveis `DB_USER`/`DB_PASSWORD`).
4. O Spring Data JPA (Hibernate) mapeia as classes de `model/` para as tabelas; `spring.jpa.hibernate.ddl-auto=none` porque a estrutura vem do `schema.sql`.
5. Se o MySQL estiver em outra porta/máquina, altere apenas a URL.

### 4.3 Executar pelo IntelliJ IDEA

1. **File → Open** e selecione a pasta `nexacare` (onde está o `pom.xml`). Aguarde o Maven baixar as dependências.
2. Confirme o JDK: **File → Project Structure → Project SDK** = 17 ou superior.
3. Defina a senha do MySQL: **Run → Edit Configurations → NexaCareApplication → Environment variables** → `DB_USER=root;DB_PASSWORD=sua_senha` (ou edite o `application.properties`).
4. Abra [`NexaCareApplication.java`](src/main/java/com/nexatech/nexacare/NexaCareApplication.java) e clique em ▶ (**Run**).
5. Quando aparecer `Started NexaCareApplication`, abra **http://localhost:8080**.

### 4.4 Executar pelo terminal

```bash
mvn spring-boot:run
```

Para gerar o `.jar`: `mvn clean package` e depois `java -jar target/nexacare-1.0.0.jar`.

### 4.5 Principais URLs

| URL | O que é |
|---|---|
| http://localhost:8080/ | Landing page |
| http://localhost:8080/login.html | Login, criar conta de paciente, recuperar senha (simulada) |
| http://localhost:8080/app.html | Sistema (exige login) |
| http://localhost:8080/privacidade.html | Política de privacidade / LGPD |
| http://localhost:8080/api/... | API REST (exige `Authorization: Bearer <token>`) |

## 5. Usuários de demonstração

A tela de login tem botões que preenchem estes acessos automaticamente.

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `administrador@nexacare.com` | `Admin@123` |
| Recepcionista | `recepcao@nexacare.com` | `Recep@123` |
| Profissional (Dra. Helena — Clínica Geral) | `medico@nexacare.com` | `Medico@123` |
| Profissional (Dr. Rafael — Cardiologia) | `cardiologista@nexacare.com` | `Medico@123` |
| Paciente (Ana Beatriz Lima) | `paciente@nexacare.com` | `Paciente@123` |

Senhas ficam no banco somente como hash **BCrypt**. As consultas e notificações de exemplo são geradas na primeira execução, sempre em dias úteis próximos da data atual. Para "zerar" a demonstração, apague o banco (`DROP DATABASE nexacare;`) e inicie de novo.

## 6. Roteiro sugerido de demonstração

1. Abra a landing → **Entrar** → botão **Recepcionista** → Entrar (dashboard com números reais do banco).
2. **Agenda** → escolha a data/profissional → clique num horário **Livre** → escolha o paciente → **Confirmar agendamento** → veja o comprovante e a mensagem de confirmação (simulada).
3. Tente agendar de novo o mesmo horário (ou o mesmo paciente no mesmo horário) → mensagem "Este horário já está ocupado."
4. Abra a consulta (**Ver detalhes**) → **Remarcar / editar** (o horário antigo fica livre) → **Cancelar** (com confirmação) → a consulta some das ativas e o horário volta a ficar livre.
5. **Notificações** → **Gerar lembretes** (simulação) → veja confirmação, lembrete, alteração e cancelamento.
6. Saia e entre como **Profissional**: só a própria agenda; abra uma consulta de hoje e **Marcar como realizada**.
7. Entre como **Paciente**: agende para si, confirme presença, **Solicitar remarcação** e cancele. Depois, como recepção, veja o aviso "Remarcação solicitada".
8. Entre como **Administrador**: Usuários e permissões, Profissionais, Configurações (horários de atendimento).
9. Tente abrir `#/pacientes` como paciente: bloqueado no front e na API (HTTP 403).

## 7. Funcionalidades implementadas

* **Landing page** com benefícios, funcionalidades, seção de segurança/LGPD e footer.
* **Login** com validação, mostrar/ocultar senha, mensagens de erro, bloqueio temporário após 5 tentativas, recuperação de senha simulada e redirecionamento (dashboard e menu conforme o perfil). **Autocadastro de paciente** com aceite da política de privacidade.
* **Dashboard por perfil**: consultas de hoje, próximos 7 dias, horários livres, canceladas, realizadas, pacientes, profissionais, remarcações solicitadas, taxa de cancelamento, agenda de hoje (com horários disponíveis) e próximas consultas.
* **Agenda**: grade por profissional e data (livre / ocupado / encerrado), filtros, navegação por dia, clique no horário livre abre o agendamento.
* **Consultas** (lista): pesquisa, filtros de período/status/profissional, paginação, remarcar e cancelar.
* **Novo agendamento / remarcação** com horários livres carregados da API e comprovante com mensagem simulada.
* **Detalhes da consulta**: status (Agendada, Confirmada, Realizada, Cancelada), ações por perfil e histórico de mensagens.
* **Pacientes**: cadastro (nome, CPF, nascimento, telefone, e-mail, observação administrativa), pesquisa, visualização com histórico, edição, desativação/reativação. **Profissionais**: cadastro (com criação opcional de login), pesquisa, filtro, edição, ativação/desativação.
* **Notificações e lembretes** (simulados): confirmação, lembrete, alteração, cancelamento e pedido de remarcação; gerar lembretes em lote; marcar como lidas; badge no menu.
* **Usuários e permissões** (admin) com matriz de permissões; **Configurações**: dados do usuário, preferências (tema claro/escuro, notificações), troca de senha e, para o admin, horário de atendimento e especialidades.
* Interface responsiva (computador, notebook, tablet e celular), com menu lateral recolhível.

## 8. Regras de negócio

1. Um profissional não pode ter dois agendamentos no mesmo horário.
2. Um paciente não pode ter dois agendamentos no mesmo horário.
3. Paciente, profissional, data e horário são obrigatórios.
4. Horários ocupados não podem ser reservados; a checagem é feita no servidor com trava de linha do profissional (evita duas reservas simultâneas).
5. Consultas **canceladas** não aparecem como ativas (as listas as ocultam por padrão) e **liberam o horário**.
6. Datas no passado, fins de semana, horários fora da grade (ex.: 10:10, fora do expediente ou na pausa) e horários que já passaram são rejeitados.
7. O profissional enxerga **somente a própria agenda**; o paciente, somente as próprias consultas (e vê horários alheios apenas como "Ocupado").
8. Permissões por perfil validadas no servidor a cada requisição.
9. CPF (dígitos verificadores), e-mail, telefone e senha (mín. 8 caracteres, letras e números) são validados.
10. Paciente/profissional com consultas futuras não podem ser desativados.
11. Mensagens claras de sucesso e erro; ações destrutivas pedem confirmação.
12. Remarcar uma consulta volta o status para **Agendada** (nova confirmação) e notifica o paciente.

**Quem pode o quê**

| Ação | Admin | Recepção | Profissional | Paciente |
|---|:-:|:-:|:-:|:-:|
| Dashboard / indicadores | ✔ | ✔ | ✔ (próprios) | ✔ (próprios) |
| Ver agenda | ✔ | ✔ | só a sua | — |
| Criar / remarcar agendamento | — | ✔ | — | só para si (criar) |
| Cancelar consulta | — | ✔ | as suas | as suas |
| Confirmar consulta | — | ✔ | ✔ | ✔ (presença) |
| Marcar como realizada | — | — | ✔ (hoje ou passada) | — |
| Cadastrar/editar pacientes | — | ✔ | — | — |
| Ver pacientes | — | completo | mínimo, só os seus | — |
| Cadastrar/ativar profissionais | ✔ | ✔ | — | — |
| Usuários, permissões, configurações | ✔ | — | — | — |

## 9. Segurança e LGPD

* **Coleta mínima**: nenhum dado clínico é armazenado; o campo de observação é administrativo e orienta a não registrar informações clínicas.
* **Autenticação** por token aleatório (8 h), com senhas em **BCrypt** e bloqueio temporário após tentativas inválidas.
* **Controle de acesso** no servidor (interceptador de autenticação + verificação de perfil em cada serviço) e também no front (rotas/menus por perfil).
* **Minimização por perfil**: CPF e e-mail de pacientes só para a recepção; profissionais veem dados mínimos dos seus pacientes; contato de profissionais não é exposto ao paciente.
* Mensagens de erro **sem detalhes técnicos** (stack traces só no log do servidor); login e recuperação de senha não revelam se um e-mail existe.
* Política de privacidade em [`privacidade.html`](src/main/resources/static/privacidade.html) e aceite no autocadastro.

## 10. Arquitetura

```
Navegador (HTML + CSS + JS + Bootstrap)
      │  fetch JSON  (Authorization: Bearer <token>)
      ▼
controller/  ── recebe HTTP, valida DTOs (@Valid), delega ao service
      ▼
service/     ── regras de negócio, permissões por perfil, transações
      ▼
repository/  ── Spring Data JPA (consultas JPQL)
      ▼
model/       ── entidades JPA  ⇄  MySQL (schema.sql / data.sql)
```

* `dto/` — objetos de entrada/saída da API (não expõem entidades nem hash de senha).
* `security/` — `AuthInterceptor` (protege `/api/**`), `TokenStore` (sessões em memória), `UsuarioLogado`.
* `exception/` — exceções de negócio e `TratadorDeErros` (mensagens amigáveis + status HTTP corretos).
* `config/` — `WebConfig` (interceptor, BCrypt), resolver do usuário logado e carga das consultas demo.
* O front é estático (servido pelo próprio Spring Boot em `/`), portanto **não há CORS** nem servidor separado.

## 11. Estrutura de pastas

```
nexacare/
├── pom.xml
├── README.md
├── banco/
│   └── nexacare_completo.sql          # criação do banco + tabelas + dados fictícios
└── src/
    ├── main/
    │   ├── java/com/nexatech/nexacare/
    │   │   ├── NexaCareApplication.java
    │   │   ├── config/        WebConfig, UsuarioLogadoResolver, DadosDemoInicializador
    │   │   ├── controller/    Auth, Paciente, Profissional, Agendamento, Notificacao, Painel
    │   │   ├── dto/           AuthDtos, CadastroDtos, AgendaDtos, ErroResponse, MensagemResponse
    │   │   ├── exception/     ExcecoesNegocio, TratadorDeErros
    │   │   ├── model/         Usuario, Paciente, Profissional, Especialidade, Agendamento,
    │   │   │                  Notificacao, Configuracao + enums (Perfil, StatusAgendamento, TipoNotificacao)
    │   │   ├── repository/    um repositório por entidade
    │   │   ├── security/      AuthInterceptor, TokenStore, UsuarioLogado
    │   │   └── service/       Autenticacao, Paciente, Profissional, Agendamento, Agenda,
    │   │                      Dashboard, Notificacao, Usuario, Configuracao, Validacoes, Mapeador
    │   └── resources/
    │       ├── application.properties
    │       ├── schema.sql     # tabelas (idempotente)
    │       ├── data.sql       # dados fictícios (INSERT IGNORE)
    │       └── static/
    │           ├── index.html, login.html, app.html, privacidade.html
    │           ├── css/nexacare.css
    │           ├── img/logo.svg
    │           └── js/
    │               ├── api.js, ui.js, validacao.js, agendamento-form.js, app.js, login.js
    │               └── views/  dashboard, agenda, consultas, consulta, pacientes,
    │                           profissionais, notificacoes, usuarios, configuracoes
    └── test/                  testes de integração da API (MockMvc)
```

## 12. Banco de dados (MySQL)

Tabelas: `usuarios`, `pacientes`, `profissionais`, `especialidades`, `agendamentos`, `notificacoes` e `configuracoes`.

```
especialidades 1───N profissionais 1───N agendamentos N───1 pacientes
                          │ 0..1                │ 1
                          ▼                     ▼ N
                       usuarios ◄── 0..1 ── pacientes      notificacoes
```

* PK `id` (AUTO_INCREMENT) em todas (exceto `configuracoes`, cuja PK é `chave`); FKs entre agendamentos↔paciente/profissional, profissional↔especialidade, usuário↔profissional/paciente e notificação↔agendamento.
* `status` da consulta: `AGENDADA`, `CONFIRMADA`, `REALIZADA`, `CANCELADA`. Timestamps `criado_em` / `atualizado_em`. Índices por profissional+data+hora e paciente+data+hora.
* Exclusão **lógica** (`ativo`/status) para preservar histórico.

## 13. API REST

Todas as rotas exigem `Authorization: Bearer <token>`, exceto as três públicas de autenticação. Erros retornam `{ "mensagem": "...", "campos": {...} }`.

| Método | Endpoint | Descrição | Perfis |
|---|---|---|---|
| POST | `/api/auth/login` | Login (retorna token e usuário) | público |
| POST | `/api/auth/registrar-paciente` | Autocadastro de paciente | público |
| POST | `/api/auth/recuperar-senha` | Recuperação simulada | público |
| POST | `/api/auth/logout` | Encerra a sessão | todos |
| GET | `/api/auth/me` | Usuário logado | todos |
| PUT | `/api/auth/senha` | Alterar senha | todos |
| PUT | `/api/auth/preferencias` | Nome, tema, notificações | todos |
| GET | `/api/dashboard` | Indicadores conforme o perfil | todos |
| GET | `/api/pacientes?busca=&incluirInativos=` | Listar/pesquisar | recepção, profissional* |
| GET | `/api/pacientes/{id}` | Detalhe | recepção, profissional* |
| POST | `/api/pacientes` | Cadastrar | recepção |
| PUT | `/api/pacientes/{id}` | Editar | recepção |
| PATCH | `/api/pacientes/{id}/status?ativo=` | Ativar/desativar | recepção |
| DELETE | `/api/pacientes/{id}` | Desativar (exclusão lógica) | recepção |
| GET | `/api/profissionais?busca=&especialidadeId=&incluirInativos=` | Listar | todos |
| GET | `/api/profissionais/{id}` | Detalhe | todos |
| POST | `/api/profissionais` | Cadastrar (opcional: senhaInicial cria login) | admin, recepção |
| PUT | `/api/profissionais/{id}` | Editar | admin, recepção |
| PATCH | `/api/profissionais/{id}/status?ativo=` | Ativar/desativar | admin, recepção |
| DELETE | `/api/profissionais/{id}` | Desativar | admin, recepção |
| GET / POST | `/api/especialidades` | Listar / criar | todos / admin |
| GET | `/api/agendamentos?inicio=&fim=&profissionalId=&pacienteId=&status=&busca=` | Listar (sem `status`: oculta canceladas; `TODAS` inclui) | todos (com escopo) |
| GET | `/api/agendamentos/{id}` | Detalhe | todos (com escopo) |
| POST | `/api/agendamentos` | Criar | recepção, paciente (para si) |
| PUT | `/api/agendamentos/{id}` | Editar/remarcar | recepção |
| PATCH | `/api/agendamentos/{id}/status` | `{ "status": "CONFIRMADA" \| "REALIZADA" \| "CANCELADA" }` | conforme regra |
| DELETE | `/api/agendamentos/{id}` | Cancelar (libera o horário) | recepção, profissional, paciente |
| POST | `/api/agendamentos/{id}/solicitar-remarcacao` | Pedido de remarcação | paciente |
| GET | `/api/agendamentos/horarios-livres?profissionalId=&data=` | Horários livres | todos |
| GET | `/api/agenda/grade?data=&profissionalId=` | Grade do dia (livre/ocupado) | todos (com escopo) |
| GET | `/api/notificacoes` | Notificações do usuário | todos (com escopo) |
| PATCH | `/api/notificacoes/{id}/lida` · POST `/api/notificacoes/lidas` | Marcar como lida(s) | todos |
| POST | `/api/notificacoes/lembretes?dias=1` | Gerar lembretes (simulação) | admin, recepção |
| GET / PUT | `/api/configuracoes` | Horário de atendimento / alterar | todos / admin |
| GET / POST | `/api/usuarios` | Listar / criar (admin ou recepção) | admin |
| PATCH | `/api/usuarios/{id}/ativo?valor=` · `/perfil` | Ativar/desativar · alterar perfil | admin |

\* Profissional vê apenas pacientes que já têm consulta com ele e sem CPF/e-mail.

Exemplo:

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"email":"recepcao@nexacare.com","senha":"Recep@123"}'
```

## 14. Testes

`src/test/java/.../NexaCareApiTest.java` executa 12 cenários de integração (login e perfis, permissões, CRUD de paciente e validações, fluxo de agendamento, conflitos, cancelamento liberando horário, escopo do profissional/paciente, remarcação, lembretes, dashboard, autocadastro e logout). Usam H2 em modo MySQL **carregando os mesmos `schema.sql` e `data.sql`** com `ddl-auto=validate`, garantindo que entidades e SQL estão coerentes.

```bash
mvn test
```

## 15. Solução de problemas

| Sintoma | Solução |
|---|---|
| `Access denied for user 'root'` | Senha do MySQL incorreta: defina `DB_USER`/`DB_PASSWORD` (item 4.1). |
| `Communications link failure` | MySQL desligado ou porta diferente de 3306. |
| Porta 8080 ocupada | Adicione `server.port=8081` ao `application.properties`. |
| `Unable to establish loopback connection` (Windows) | Já tratado automaticamente no `main` (o caminho temporário no formato 8.3, ex. `NOME~1`, quebra o Tomcat no JDK 21). Se ocorrer em outra forma de execução, use `-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp`. |
| Tela em branco / 401 | A sessão expira ao reiniciar o servidor: faça login novamente. |
| Dados de demonstração "velhos" | `DROP DATABASE nexacare;` e reinicie para recriar consultas em datas atuais. |

## 16. Publicar no GitHub

```bash
git init
git add .
git commit -m "NexaCare: protótipo funcional"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/nexacare.git
git push -u origin main
```

## 17. Limitações conhecidas (escopo de protótipo/MVP)

* Notificações e lembretes são simulados (sem integração real com WhatsApp/SMS/e-mail); o lembrete é gerado por botão, não por agendador automático.
* Recuperação de senha é apenas simulada (não há redefinição por e-mail).
* Sessões ficam em memória: reiniciar o servidor encerra os logins (e não escala para várias instâncias).
* A agenda funciona em dias úteis, com grade única (horário, duração e pausa configuráveis para toda a clínica, não por profissional).
* Não há prontuário nem qualquer dado clínico, por decisão de minimização de dados.
* A grade da agenda mostra um dia por vez (sem visão semanal/mensal).
* Cada notificação tem um único estado "lida" (compartilhado entre quem a enxerga).
