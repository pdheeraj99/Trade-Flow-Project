---
trigger: always_on
description: 
globs: 
---

# Trade-Flow Project Rules

## Architecture
- Multi-module Maven project with Spring Boot microservices
- React TypeScript frontend
- Services: api-gateway, auth-service, audit-service, market-data-service, matching-engine, oms-service, wallet-service
- All REST APIs must follow OpenAPI 3.0 specification

## Backend Standards
- Use Spring Boot 3.x with Java 17+
- All DTOs must have proper validation annotations (@Valid, @NotNull, etc.)
- Generate OpenAPI specs using springdoc-openapi
- Follow RESTful conventions for endpoints
- Use ResponseEntity for all controller methods

## Frontend Standards  
- TypeScript strict mode enabled
- Generate types from OpenAPI specs using orval or openapi-typescript
- Use React Query for API calls
- All API responses must be validated with Zod schemas

## Type Safety Rules
- NEVER manually write API types - always generate from OpenAPI
- DTOs on backend must match TypeScript interfaces exactly
- Run type generation before every frontend build
- Date fields: Use ISO 8601 strings (LocalDateTime → string in TypeScript)
- Enums: Must be identical between Java and TypeScript

## Don't Modify
- pom.xml structure without approval
- docker-compose.yml service names
- Common module shared DTOs without updating all services
