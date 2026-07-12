FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM amazoncorretto:21-alpine
# aws-cli + jq: only actually used in prod, to fetch DB credentials at container startup
# (command-override in the Helm chart, no CSI/sidecar -- no IRSA available on this
# self-managed cluster). Harmless/unused on local, which uses a plain k8s Secret instead.
RUN apk add --no-cache aws-cli jq
WORKDIR /app
COPY --from=build /build/target/membership.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
