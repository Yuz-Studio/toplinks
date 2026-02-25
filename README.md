# toplinks

[![CI](https://github.com/Yuz-Studio/toplinks/actions/workflows/ci.yml/badge.svg)](https://github.com/Yuz-Studio/toplinks/actions/workflows/ci.yml)

A Spring Boot application for file upload and display.

## Running Tests Locally

Make sure you have a MySQL instance running with a database named `toplinks_test`, then:

```bash
mvn clean verify -Dspring.profiles.active=test
```

Or with a custom DB username/password:

```bash
DB_USERNAME=myuser DB_PASSWORD=mypass mvn clean verify -Dspring.profiles.active=test
```

## CI

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs automatically on every push and pull request targeting `main`. It:

- Starts a MySQL 8.0 service container with the `toplinks_test` database
- Sets up Java 17 (Temurin) with Maven dependency caching
- Runs `mvn -B -ntp clean verify -Dspring.profiles.active=test`