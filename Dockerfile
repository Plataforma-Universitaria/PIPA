FROM maven:3.9.6-amazoncorretto-21 as stage1
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
WORKDIR /app/build

RUN yum install git -y

# API_IA
ADD "https://github.com/Plataforma-Universitaria/API_IA/commits?per_page=1&sha=main" latest_commit
RUN git clone https://github.com/Plataforma-Universitaria/API_IA.git && \
    cd API_IA && \
    git checkout main && \
    mvn clean install -U -Dmaven.test.skip=true && \
    cd ..

# PIPA_INTEGRATOR
ADD "https://github.com/Plataforma-Universitaria/PIPA_INTEGRATOR/commits?per_page=1&sha=main" latest_commit
RUN git clone https://github.com/Plataforma-Universitaria/PIPA_INTEGRATOR.git && \
    cd PIPA_INTEGRATOR && \
    git checkout main && \
    mvn clean install -U -Dmaven.test.skip=true && \
    cd ..

# PIPA_MIDDLEWARE
ADD "https://github.com/Plataforma-Universitaria/PIPA_MIDDLEWARE/commits?per_page=1&sha=main" latest_commit
RUN git clone https://github.com/Plataforma-Universitaria/PIPA_MIDDLEWARE.git && \
    cd PIPA_MIDDLEWARE && \
    git checkout main && \
    mvn clean install -U -Dmaven.test.skip=true && \
    cd ..

## PIPA_EMAIL
#ADD "https://github.com/Plataforma-Universitaria/PIPA_EMAIL/commits?per_page=1&sha=main" latest_commit
#RUN git clone https://github.com/Plataforma-Universitaria/PIPA_EMAIL.git && \
#    cd PIPA_EMAIL && \
#    git checkout main && \
#    mvn clean install -U -Dmaven.test.skip=true && \
#    cd ..

# UEG_PROVIDER
ADD "https://github.com/Plataforma-Universitaria/UEG_PROVIDER/commits?per_page=1&sha=main" latest_commit
RUN git clone https://github.com/Plataforma-Universitaria/UEG_PROVIDER.git && \
    cd UEG_PROVIDER && \
    git checkout main && \
    mvn clean install -U -Dmaven.test.skip=true && \
    cd ..

# Copia e instala o projeto principal
COPY pom.xml .
COPY ./src ./src
RUN mvn clean install -U -Dmaven.test.skip=true

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=stage1 /app/build/target/*.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]
