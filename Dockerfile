FROM maven:3.9.6-amazoncorretto-21 AS builder

RUN yum install -y git && yum clean all
WORKDIR /app

# Clonar os repositórios
RUN git clone https://github.com/Plataforma-Universitaria/API_IA.git && \
    git clone https://github.com/Plataforma-Universitaria/PIPA_INTEGRATOR.git && \
    git clone https://github.com/Plataforma-Universitaria/PIPA_MIDDLEWARE.git && \
    git clone https://github.com/Plataforma-Universitaria/UEG_PROVIDER.git && \
    git clone https://github.com/Plataforma-Universitaria/PIPA.git

# Build dos projetos para instalar as dependências locais
RUN cd API_IA && mvn clean install -Dmaven.test.skip=true
RUN cd PIPA_INTEGRATOR && mvn clean install -Dmaven.test.skip=true
RUN cd PIPA_MIDDLEWARE && mvn clean install -Dmaven.test.skip=true
RUN cd UEG_PROVIDER && mvn clean install -Dmaven.test.skip=true

# Agora build do projeto principal (PIPA)
RUN cd PIPA && mvn clean install -Dmaven.test.skip=true

# Copiar o jar final para a imagem runtime
FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=builder /app/PIPA/target/*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
