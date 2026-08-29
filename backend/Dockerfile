# --- Etapa 1: compilar ---------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar só o pom primeiro faz o Docker guardar em cache as dependências.
# Assim, alterar código não obriga a descarregar tudo outra vez.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Etapa 2: correr -----------------------------------------------
# Imagem só com o runtime: muito mais pequena do que a de compilação.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage: o Render dá 512 MB. Sem isto a JVM assume
# limites do host e pode ser morta por falta de memória.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
