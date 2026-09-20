# socialmediaapi
Educational project

## PostgreSQL

The database runs in Docker with the following default settings:

- database: `social_media`
- schema: `public`
- user: `postgres`
- password: `password`
- port: `5432`

Start PostgreSQL:
```shell
docker compose up -d
```

Check container status:
```shell
docker compose ps
```

View PostgreSQL logs (useful for diagnosing startup and connection errors):
```shell
docker compose logs postgres
```

Stop PostgreSQL and remove its container and network while preserving the
database volume:
```shell
docker compose down
```

Stop PostgreSQL and delete its volume with all database data. Use this command
only when the database needs to be initialized from scratch:
```shell
docker compose down -v
```

## Application

Run the tests on Linux or macOS:
```shell
./mvnw test
```

Start the application on Linux or macOS:
```shell
./mvnw spring-boot:run
```

Stop:
```text
Ctrl + C
```

