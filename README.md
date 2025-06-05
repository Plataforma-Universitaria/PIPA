# PIPA - Plataforma integrada personalizável para assistentes

O **PIPA** é um sistema modular e distribuído desenvolvido com **Spring Boot**
que fornece um meio pelo qual instituições parceiras pode integrar os seus sistemas
académicos ou bases de dados por meio de provedores com assistentes digitais.
---

## Arquitetura do Sistema

- O PIPA é um ecossistema modular distribuído, projetado para conectar instituições parceiras ao seu público-alvo de maneira personalizada e eficiente. Com foco inicial na **Universidade Estadual de Goiás (UEG-CET)**, a plataforma unifica soluções inteligentes e seguras através de microsserviços especializados.

- Cada projeto/módulo da PIPA possui responsabilidade bem definida e tecnologias específicas

---

## Visão Geral dos Módulos

| Projeto          | Responsabilidade                                                                 |
|------------------|----------------------------------------------------------------------------------|
| `API_IA`         | Comunicação com a OpenAI via Spring AI                                           |
| `AUTH_SERVER`    | Servidor de autorização e emissão de JWT                                         |
| `PIPA`           | Núcleo de domínio e orquestração das intenções do usuário                        |
| `PIPA_MIDDLEWARE`| Recebimento e tratamento de tokens JWT                                           |
| `PIPA_INTEGRATOR`| Contratos e interfaces para provedores                                           |
| `UEG_PROVIDER`   | Provedor de serviços reais usados no estudo de caso da UEG                       |

---

## Detalhamento dos Projetos

### API_IA

Projeto responsável pela comunicação entre a plataforma e o **OpenAI**, permitindo integração com modelos de linguagem como o ChatGPT.

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

Responsável pelo **domínio do sistema**, processando a intenção dos usuários e orquestrando os módulos da plataforma. Atua como cérebro da operação, onde reside a lógica de negócio.

**Tecnologias utilizadas:**
- `spring-boot-starter-data-jdbc`
- `spring-boot-starter-data-jpa`
- `mapstruct:1.5.5.Final`
- `springdoc-openapi-starter-webmvc-ui:2.0.4`
- `org.reflections:reflections:0.10.2`
- `org.postgresql:postgresql`

**Módulos internos:**
- `apiai:0.0.1-SNAPSHOT`
- `pipa_integrator:0.0.1-SNAPSHOT`
- `ueg_provider:0.0.1-SNAPSHOT`
- `pipa_middleware:0.0.1-SNAPSHOT`

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
- `pipa_integrator:0.0.1-SNAPSHOT`

---

### Como rodar o projeto

Clone os repositórios
- `https://github.com/Plataforma-Universitaria/API_IA`
- `https://github.com/Plataforma-Universitaria/PIPA`
- `https://github.com/Plataforma-Universitaria/PIPA_INTEGRATOR`
- `https://github.com/Plataforma-Universitaria/UEG_PROVIDER`
- `https://github.com/Plataforma-Universitaria/PIPA_MIDDLEWARE`
- `https://github.com/Plataforma-Universitaria/AUTH_SERVER`

## Configure as variáveis de ambiente
#### Para o uso da API_IA o módulo deve configurar:
`spring.ai.openai.api-key
spring.ai.openai.chat.options.model`

#### Para o uso do AUTH_SERVER
`server.port
platform.auth.url
platform.salutation.url
platform.institutions.url
jwt.private-key
jwt.public-key
jwt.expiration
jwt.issuer
bot.callback.url
`
#### Para a PIPA

`root.package
spring.ai.openai.api-key
spring.ai.openai.chat.options.model
spring.datasource.url
spring.datasource.username
spring.datasource.password
spring.datasource.driver-class-name
jwt.public-key`

## Rode o comando maven na seguinte ordem

* `API_AI`
* `PIPA_INTEGRATOR`
* `PIPA_MIDDLEWARE`
* `UEG_PROVIDER`
* `PIPA`
* `AUTH_SERVER`