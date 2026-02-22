# ===========================================
# Stage 1: Build WAR with Maven
# ===========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first for dependency caching
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code
COPY backend/src ./src

# Build WAR (skip tests - run separately)
RUN mvn clean package -DskipTests -B

# ===========================================
# Stage 2: GlassFish Runtime
# ===========================================
FROM eclipse-temurin:17-jdk-jammy AS runtime

# Arguments
ARG GLASSFISH_VERSION=7.0.11
ARG POSTGRES_DRIVER_VERSION=42.7.1

# Environment variables
ENV GLASSFISH_HOME=/opt/glassfish7
ENV PATH="${GLASSFISH_HOME}/bin:${PATH}"
ENV DOMAIN_NAME=domain1

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

# Download and extract GlassFish
RUN curl -L -o /tmp/glassfish.zip \
    "https://github.com/eclipse-ee4j/glassfish/releases/download/${GLASSFISH_VERSION}/glassfish-${GLASSFISH_VERSION}.zip" \
    && unzip -q /tmp/glassfish.zip -d /opt \
    && rm /tmp/glassfish.zip

# Download PostgreSQL JDBC driver
RUN curl -L -o ${GLASSFISH_HOME}/glassfish/lib/postgresql-${POSTGRES_DRIVER_VERSION}.jar \
    "https://jdbc.postgresql.org/download/postgresql-${POSTGRES_DRIVER_VERSION}.jar"

# Copy setup script and fix line endings (Windows CRLF -> Unix LF)
COPY docker/backend/setup-glassfish.sh /opt/setup-glassfish.sh
RUN sed -i 's/\r$//' /opt/setup-glassfish.sh && chmod +x /opt/setup-glassfish.sh

# Copy WAR to staging location (NOT autodeploy - we deploy manually after JDBC setup)
COPY --from=builder /build/target/ideaboard.war /opt/ideaboard.war

# Create non-root user with home directory
RUN groupadd -r glassfish && useradd -r -g glassfish -m -d /home/glassfish glassfish \
    && chown -R glassfish:glassfish ${GLASSFISH_HOME} \
    && mkdir -p /home/glassfish/.gfclient \
    && chown -R glassfish:glassfish /home/glassfish

USER glassfish

# Expose ports
EXPOSE 8080 4848

# Startup script
CMD ["/opt/setup-glassfish.sh"]
