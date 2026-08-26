# PIPA - Plataforma integrada personalizável para assistentes

**PIPA** é uma plataforma digital desenvolvida com **Spring Boot**
que fornece um meio pelo qual instituições parceiras pode integrar os seus sistemas
académicos ou bases de dados por meio de provedores com assistentes digitais.
---

## Arquitetura do Sistema
- Cada projeto/módulo da PIPA possui responsabilidade bem definida e tecnologias específicas

### Visão Geral dos Módulos

| Projeto           | Responsabilidade                                           |
|-------------------|------------------------------------------------------------|
| `API_AI`          | Módulo JAR de comunicação com a OpenAI via Spring AI       |
| `AUTH_SERVER`     | Servidor de autorização e emissão de JWT                   |
| `PIPA`            | Núcleo de domínio e orquestração das intenções do usuário  |
| `PIPA_MIDDLEWARE` | Configuração de filtros JWT                                |
| `PIPA_INTEGRATOR` | Contratos e interfaces para provedores                     |
| `UEG_PROVIDER`    | Provedor de serviços reais usados no estudo de caso da UEG |
| `PIPA_EMAIL`      | Envio de emails                                            |


---

## Detalhamento dos Projetos

### API_AI

Módulo JAR responsável pela comunicação entre a plataforma e a **OpenAI**, permitindo integração com modelos de linguagem como o ChatGPT. Embora possua dependências Spring Web e WebFlux, o código atual não contém controller nem endpoint REST; suas classes são consumidas diretamente pelos demais módulos Java.

**Tecnologias utilizadas:**
- `spring-ai-starter-model-openai:1.0.0-SNAPSHOT`
- `spring-ai-bom:1.0.0-SNAPSHOT`
- `jackson-databind`

---

### AUTH_SERVER

Servidor de autorização central, encarregado da autenticação dos usuários e emissão de **tokens JWT**.

**Tecnologias utilizadas:**
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-validation`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (`0.12.5`)

---

### PIPA

Responsável pelo **domínio do sistema** e pela orquestração dos módulos da plataforma. O fluxo standalone em `/api/intent` processa a intenção com `RequestExecutorService` e `AiService`. Na integração atual com o Guará, o PIPA descobre e executa ferramentas de forma determinística pelos endpoints `/api/guara/**`, enquanto a seleção conversacional da ferramenta permanece no LangChain do Guará.

O núcleo também contém a base de observabilidade: `UserSession`, `ToolExecutionLog`, `ObservabilityService`, `ProviderFailureResolver` e `GET /api/observability/logs`. O endpoint retorna `ObservabilityLogDTO` sem usuário, fingerprint, mensagem ou stack trace. Nas invocações alcançadas, o Core mede `durationMs`, percorre a cadeia de causas e persiste código, categoria, etapa e retry do contrato `ProviderFailure`; falhas desconhecidas recebem classificação segura de fallback. Falhas anteriores a `method.invoke()` ainda não são registradas. Encerramento automático conforme o TTL do contexto do Guará, consultas acumulativas por `Specification`, Dashboard, exportação e proteção administrativa específica ainda não estão implementados.

**Tecnologias utilizadas:**
- `spring-boot-starter-data-jdbc`
- `spring-boot-starter-data-jpa`
- `mapstruct:1.5.5.Final`
- `springdoc-openapi-starter-webmvc-ui:2.0.4`
- `org.reflections:reflections:0.10.2`
- `org.postgresql:postgresql`

**Módulos internos:**
- `apiai:0.0.1-SNAPSHOT`
- `PIPA_INTEGRATOR:0.0.1-SNAPSHOT`
- `UEG_PROVIDER:0.0.1-SNAPSHOT`
- `PIPA_MIDDLEWARE:0.0.1-SNAPSHOT`
- `PIPA_EMAIL:0.0.1-SNAPSHOT`

---

### PIPA_MIDDLEWARE

Responsável por **receber e validar tokens JWT**, servindo como camada de proteção entre os consumidores externos e os serviços da plataforma.

**Tecnologias utilizadas:**
- `spring-boot-starter-oauth2-client`
- `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-security`
- `spring-security-oauth2-jose`

---

### PIPA_INTEGRATOR

Define os **contratos e interfaces** obrigatórios para que qualquer provedor de serviço (como UEG_PROVIDER) possa ser conectado à plataforma.

**Tecnologias utilizadas:**
- `gson:2.10.1`
- `lombok:1.18.30`

**Módulo interno:**
- `apiai:0.0.1-SNAPSHOT`

---

### UEG_PROVIDER

Módulo do estudo de caso com a **Universidade Estadual de Goiás (UEG-CET)**. Fornece os serviços institucionais integrados à PIPA, consulta de aulas e notas.

**Tecnologias utilizadas:**
- `joda-time:2.12.7`
- `reflections:0.10.2`
- `flying-saucer-pdf:9.9.4`
- `apache-httpclient:5.1.3`
- `jtiddy:r938`
- `simple-java-mail:8.12.2`

**Módulos internos:**
- `apiai:0.0.1-SNAPSHOT`
- `PIPA_INTEGRATOR:0.0.1-SNAPSHOT`
- `PIPA_EMAIL:0.0.1-SNAPSHOT`


---

### Como rodar o projeto

Clone os repositórios
* `https://github.com/Plataforma-Universitaria/API_IA`
* `https://github.com/Plataforma-Universitaria/PIPA`
* `https://github.com/Plataforma-Universitaria/PIPA_INTEGRATOR`
* `https://github.com/Plataforma-Universitaria/UEG_PROVIDER`
* `https://github.com/Plataforma-Universitaria/PIPA_MIDDLEWARE`
* `https://github.com/Plataforma-Universitaria/AUTH_SERVER`
* `https://github.com/Plataforma-Universitaria/PIPA_EMAIL`

---

## Configure as variáveis de ambiente
#### Para o uso da API_AI

O `AIClient` fornecido pelo PIPA_INTEGRATOR, consumidor do módulo, lê estas propriedades:

* `spring.ai.openai.api-key`
* `spring.ai.openai.chat.options.model`

#### Para o uso do AUTH_SERVER

O `application.properties` do Auth Server fixa `server.port=9090` e lê:

* `ROOT_URL_AUTH`
* `ROOT_URL_LOGOUT`
* `ROOT_URL_SALUTATION`
* `ROOT_URL_INSTITUTIONS`
* `PRIVATE_KEY`
* `EXP_TIME`
* `ISSUER`
* `CALLBACK`

#### Para a PIPA

O `application.properties` atual lê as seguintes variáveis de ambiente:

* `OPENAI_API_KEY`
* `OPENAI_API_MODEL`
* `DB_ADDRESS`
* `DB_USER`
* `DB_PASSWORD`
* `PUBLIC_KEY`
* `EMAIL_HOST`
* `EMAIL_PORT`
* `EMAIL_APP_PW`
* `EMAIL`
* `USER_ADMIN`
* `USER_PASS`

Além disso, `root.package` está definido como `br.ueg.tc.` e `guara.api-key` está configurada diretamente no `application.properties` atual.


## Rode o comando maven na seguinte ordem

* `API_AI`
* `PIPA_INTEGRATOR`
* `PIPA_EMAIL`
* `PIPA_MIDDLEWARE`
* `UEG_PROVIDER`
* `PIPA`
* `AUTH_SERVER`
