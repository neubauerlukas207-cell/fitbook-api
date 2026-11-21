# --- build: Maven uvnitř kontejneru, není potřeba mít nic nainstalované ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# --- runtime: jen JRE a hotový jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system app
COPY --from=build /src/target/fitbook-api-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
