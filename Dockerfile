FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

COPY scenario-repositories/url-shortener/ scenario-repositories/url-shortener/
RUN chmod +x scenario-repositories/url-shortener/mvnw && \
    cd scenario-repositories/url-shortener && \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY scenario-repositories/greenfield-seed/ scenario-repositories/greenfield-seed/
RUN chmod +x scenario-repositories/greenfield-seed/mvnw && \
    cd scenario-repositories/greenfield-seed && \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

FROM eclipse-temurin:21-jdk-alpine AS runtime

RUN addgroup -S agent && \
    adduser -S -G agent -h /home/agent agent

WORKDIR /opt/app

COPY --from=build /workspace/target/agentic-software-engineer-*.jar application.jar
COPY --from=build --chown=agent:agent \
    /workspace/scenario-repositories scenario-repositories
COPY --from=build --chown=agent:agent /root/.m2 /home/agent/.m2

RUN mkdir -p /opt/app/agent-workspaces && \
    chown -R agent:agent /opt/app

USER agent:agent

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD wget -q -O - http://127.0.0.1:8080/actuator/health/readiness \
        | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/opt/app/application.jar"]
