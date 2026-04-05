FROM eclipse-temurin:17-jdk

# Install Maven
RUN apt-get update && apt-get install -y maven

# Set working directory
WORKDIR /app

# Copy project
COPY . .

# Build project
RUN mvn clean package -DskipTests

# Run jar
CMD ["java", "-jar", "target/finance-core-system-0.0.1-SNAPSHOT.jar"]
