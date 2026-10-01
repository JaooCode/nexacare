# ---------- 1) Compila o projeto ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- 2) Imagem final, só com o necessário para rodar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Fuso de Brasília (a agenda e as consultas de demonstração dependem da data de "hoje")
ENV TZ=America/Sao_Paulo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo -XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
