FROM node:24-alpine AS web-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:17-jdk-alpine AS api-build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src/ src/
COPY --from=web-build /workspace/frontend/dist/ frontend/dist/
RUN ./mvnw -B -ntp clean verify

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S finaxis && adduser -S -G finaxis -u 10001 finaxis
COPY --from=api-build /workspace/target/finance-core-system-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=35s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:${PORT:-8080}/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
