# Flowable Wrapper API

A generic workflow wrapper API built on top of Flowable engine.

## Project Structure

```
custom-flowable/
├── wrapper-api/          # Spring Boot application
├── definitions/          # BPMN workflow definitions  
├── docker/              # Docker compose for dependencies
└── README.md
```

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Flowable 7.1.0
- PostgreSQL
- Keycloak for authentication
- OpenAPI/Swagger documentation

## Setup

1. Start dependencies:
```bash
cd docker
docker-compose up -d
```

2. Build and run the application:
```bash
cd wrapper-api
./mvnw spring-boot:run
```

3. Access:
- API: http://localhost:8090
- Swagger UI: http://localhost:8090/swagger-ui.html
- Keycloak: http://localhost:8180

## What's Implemented So Far

- ✅ Basic project structure
- ✅ Maven dependencies  
- ✅ Core entities (QueueTask, ProcessMetadata)
- ✅ Repository layer
- ✅ Docker compose setup
- ⏳ Controllers
- ⏳ Services
- ⏳ Event listeners
- ⏳ Security configuration
- ⏳ Exception handling