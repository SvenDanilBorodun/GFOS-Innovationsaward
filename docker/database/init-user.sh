set -e

# This script runs after init.sql to create the application user
echo "Creating application database user..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create application user if not exists
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ideaboard_user') THEN
            CREATE USER ideaboard_user WITH PASSWORD 'ideaboard123';
        END IF;
    END
    \$\$;

    -- Grant privileges
    GRANT ALL PRIVILEGES ON DATABASE ${POSTGRES_DB} TO ideaboard_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ideaboard_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ideaboard_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO ideaboard_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO ideaboard_user;
EOSQL

echo "Application user created and configured successfully!"
