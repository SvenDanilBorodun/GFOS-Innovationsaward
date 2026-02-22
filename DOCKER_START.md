# Docker Quick Start

copy .env.example .env

# Starten aller services
docker compose up --build

# Kann auch verwendet werden bei Problemen 
docker compose down -v && docker compose build --no-cache && docker compose up
```

Warten und bei `Application deployed successfully!`:

**http://localhost:3000**

