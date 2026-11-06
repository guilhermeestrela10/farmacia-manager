FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home farmacia
COPY --from=build /app/target/*.jar app.jar
USER farmacia
EXPOSE 10000
ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-XX:MaxRAMPercentage=60.0", "-jar", "app.jar"]
