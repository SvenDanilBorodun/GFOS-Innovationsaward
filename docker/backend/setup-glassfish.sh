#!/bin/bash
set -e

echo "=== Starting GlassFish Setup ==="

# Wait for database to be ready
echo "Waiting for database at ${DB_HOST}:${DB_PORT}..."
until nc -z ${DB_HOST:-db} ${DB_PORT:-5432}; do
  echo "Database not ready, waiting..."
  sleep 2
done
echo "Database is ready!"

# Start GlassFish domain
echo "Starting GlassFish domain..."
asadmin start-domain ${DOMAIN_NAME:-domain1}

# Wait for GlassFish to be fully started
echo "Waiting for GlassFish to be ready..."
sleep 10

# Configure JDBC Connection Pool (idempotent)
if ! asadmin list-jdbc-connection-pools | grep -q "IdeaBoardPool"; then
  echo "Creating JDBC connection pool..."
  asadmin create-jdbc-connection-pool \
    --restype javax.sql.DataSource \
    --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
    --property "user=${DB_USER:-postgres}:password=${DB_PASSWORD:-postgres}:serverName=${DB_HOST:-db}:portNumber=${DB_PORT:-5432}:databaseName=${DB_NAME:-ideaboard}" \
    IdeaBoardPool
  echo "JDBC connection pool created."
else
  echo "JDBC connection pool already exists."
fi

# Configure JDBC Resource (idempotent)
if ! asadmin list-jdbc-resources | grep -q "jdbc/ideaboard"; then
  echo "Creating JDBC resource..."
  asadmin create-jdbc-resource \
    --connectionpoolid IdeaBoardPool \
    jdbc/ideaboard
  echo "JDBC resource created."
else
  echo "JDBC resource already exists."
fi

# Ping the connection pool to verify
echo "Testing database connection..."
if asadmin ping-connection-pool IdeaBoardPool; then
  echo "Database connection successful!"
else
  echo "WARNING: Database connection test failed. Check configuration."
fi

# Deploy the application manually (after JDBC is configured)
echo "Deploying application..."
if ! asadmin list-applications | grep -q "ideaboard"; then
  asadmin deploy --contextroot ideaboard --name ideaboard /opt/ideaboard.war
  echo "Application deployed successfully!"
else
  echo "Application already deployed."
fi

echo "=== Application deployed successfully! ==="
echo "Backend API: http://localhost:8080/ideaboard/api"
echo "GlassFish Admin: http://localhost:4848"

# Keep container running by tailing GlassFish log
exec tail -f ${GLASSFISH_HOME}/glassfish/domains/${DOMAIN_NAME:-domain1}/logs/server.log
