# Security Integration Guide: OAuth2 with Spring Authorization Server

This guide explains how to integrate the OAuth2 authorization server with your existing Fastify and NestJS microservices for your expense tracking application.

## Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │  Authorization   │    │   Microservices │
│  (React/Vue/    │◄──►│     Server       │◄──►│   (Fastify/     │
│   Angular)      │    │  (Spring Boot)   │    │    NestJS)      │
│                 │    │   Port: 9000     │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │   PostgreSQL     │
                       │   (Auth DB)      │
                       │   Port: 5432     │
                       └──────────────────┘
```

## Features

- **OAuth2 Authorization Code Flow**: For user authentication from frontend
- **Client Credentials Flow**: For service-to-service communication
- **JWT Tokens**: Stateless authentication with custom claims
- **Scope-based Authorization**: Granular permissions (bank:read, bank:write, transaction:read, transaction:write)
- **Role-based Authorization**: User roles (USER, ADMIN)
- **CORS Support**: Cross-origin requests from frontend
- **Token Introspection**: For microservices to validate tokens

## Quick Start

### 1. Database Setup

Create a PostgreSQL database for the authorization server:

```sql
CREATE DATABASE authdb;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE authdb TO postgres;
```

### 2. Start Authorization Server

```bash
# Build and run the Spring Boot application
./mvnw spring-boot:run
```

The authorization server will be available at: `http://localhost:9000`

### 3. Register a User

```bash
curl -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'
```

## OAuth2 Clients Configuration

The authorization server is pre-configured with three OAuth2 clients:

### 1. Bank Accounts Service (Fastify)
- **Client ID**: `bank-accounts-service`
- **Client Secret**: configured via environment variable (do not hardcode)
- **Scopes**: `bank:read`, `bank:write`, `profile`
- **Port**: 3000

### 2. Transactions Service (NestJS)
- **Client ID**: `transactions-service`
- **Client Secret**: configured via environment variable (do not hardcode)
- **Scopes**: `transaction:read`, `transaction:write`, `profile`
- **Port**: 3001

### 3. Frontend Application
- **Client ID**: `expense-tracker-frontend`
- **Client Secret**: configured via environment variable (do not hardcode)
- **Scopes**: `openid`, `profile`, `email`, `bank:read`, `transaction:read`, `transaction:write`

## Fastify Integration

### Installation

```bash
npm install @fastify/jwt @fastify/cors jwks-client
```

### Implementation

Copy the configuration from `microservices-integration/fastify-bank-accounts-config.js` to your Fastify service.

### Usage Example

```javascript
// Protected route with scope checking
fastify.get('/api/bank-accounts', {
  preHandler: [fastify.authenticate, fastify.requireScope('bank:read')]
}, async (request, reply) => {
  const userId = request.user.user_id;
  // Your existing logic here
  return { accounts: [], userId };
});
```

## NestJS Integration

### Installation

```bash
npm install @nestjs/jwt @nestjs/passport passport passport-jwt jwks-rsa
```

### Implementation

Copy the configuration from `microservices-integration/nestjs-transactions-config.ts` to your NestJS service.

### Usage Example

```typescript
@Controller('api/transactions')
@UseGuards(JwtAuthGuard, ScopesGuard)
export class TransactionsController {
  
  @Get()
  @RequireScopes('transaction:read')
  async getTransactions(@Request() req) {
    const userId = req.user.userId;
    // Your existing logic here
    return { transactions: [] };
  }
}
```

## Authentication Flows

### 1. Authorization Code Flow (Frontend)

```javascript
// Step 1: Redirect user to authorization server
const authUrl = 'http://localhost:9000/oauth2/authorize?' +
  'response_type=code&' +
  'client_id=expense-tracker-frontend&' +
  'redirect_uri=http://localhost:4200/login/oauth2/code/expense-tracker&' +
  'scope=openid profile email bank:read transaction:read transaction:write';

window.location.href = authUrl;

// Step 2: Exchange code for token (in your callback handler)
const tokenResponse = await fetch('http://localhost:9000/oauth2/token', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
    'Authorization': 'Basic ' + btoa(`${CLIENT_ID}:${CLIENT_SECRET}`)
  },
  body: new URLSearchParams({
    'grant_type': 'authorization_code',
    'code': authorizationCode,
    'redirect_uri': 'http://localhost:4200/login/oauth2/code/expense-tracker'
  })
});
```

### 2. Client Credentials Flow (Service-to-Service)

```javascript
// Get service token for API calls
async function getServiceToken() {
  const response = await fetch('http://localhost:9000/oauth2/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Basic ' + btoa('bank-accounts-service:bank-secret')
    },
    body: new URLSearchParams({
      'grant_type': 'client_credentials',
      'scope': 'bank:read bank:write'
    })
  });
  
  const data = await response.json();
  return data.access_token;
}
```

## API Endpoints

### Authorization Server Endpoints

- **Authorization**: `GET /oauth2/authorize`
- **Token**: `POST /oauth2/token`
- **Token Introspection**: `POST /api/auth/token/introspect`
- **User Info**: `GET /api/auth/user-info`
- **User Registration**: `POST /api/auth/register`
- **JWKS**: `GET /.well-known/jwks.json`

### Protected Microservice Endpoints

All microservice endpoints require a valid JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

## JWT Token Structure

```json
{
  "sub": "testuser",
  "user_id": "testuser",
  "roles": ["ROLE_USER"],
  "scopes": ["bank:read", "transaction:write"],
  "iss": "http://localhost:9000",
  "exp": 1234567890,
  "iat": 1234567890
}
```

## Kafka Integration

Your microservices can continue using Kafka for inter-service communication. When publishing messages to Kafka, include the user context:

```javascript
// In your Fastify service
await kafka.producer.send({
  topic: 'bank-account-events',
  messages: [{
    value: JSON.stringify({
      userId: request.user.user_id,
      userRoles: request.user.roles,
      event: 'account_created',
      data: accountData
    })
  }]
});
```

## Security Best Practices

1. **HTTPS in Production**: Always use HTTPS in production environments
2. **Secure Secrets**: Store client secrets securely (environment variables, secret management)
3. **Token Expiration**: Configure appropriate token expiration times
4. **Scope Validation**: Always validate scopes in your microservices
5. **Rate Limiting**: Implement rate limiting on your endpoints
6. **Input Validation**: Validate all inputs in your microservices

## Testing

### Test User Registration

```bash
curl -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'
```

### Test Token Generation

```bash
# Get authorization code (browser-based)
# Then exchange for token:
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "expense-tracker-frontend:frontend-secret" \
  -d "grant_type=authorization_code&code=YOUR_CODE&redirect_uri=http://localhost:4200/login/oauth2/code/expense-tracker"
```

### Test Service Token

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "bank-accounts-service:bank-secret" \
  -d "grant_type=client_credentials&scope=bank:read bank:write"
```

## Troubleshooting

### Common Issues

1. **Token Invalid**: Check token expiration and signature
2. **Insufficient Scope**: Verify the token has required scopes
3. **CORS Errors**: Check CORS configuration in both authorization server and microservices
4. **Database Connection**: Verify PostgreSQL connection settings

### Debug Endpoints

- Check server health: `GET http://localhost:9000/actuator/health`
- View token info: `GET http://localhost:9000/api/auth/user-info` (with valid token)
- JWKS endpoint: `GET http://localhost:9000/.well-known/jwks.json`

## Next Steps

1. **Frontend Integration**: Create a frontend application that uses the authorization code flow
2. **API Gateway**: Consider adding an API gateway for routing and additional security
3. **Monitoring**: Add monitoring and logging for security events
4. **Admin Panel**: Create an admin interface for managing users and clients