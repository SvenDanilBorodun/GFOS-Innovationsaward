FROM postgres:15-alpine

# Copy init scripts and fix line endings
COPY database/init.sql /docker-entrypoint-initdb.d/01-init.sql
COPY docker/database/init-user.sh /docker-entrypoint-initdb.d/02-init-user.sh

# Fix Windows line endings and set permissions
RUN sed -i 's/\r$//' /docker-entrypoint-initdb.d/01-init.sql \
    && sed -i 's/\r$//' /docker-entrypoint-initdb.d/02-init-user.sh \
    && chmod +x /docker-entrypoint-initdb.d/02-init-user.sh
