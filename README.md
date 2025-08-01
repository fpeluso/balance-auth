# Balance Auth - OAuth2 Authorization Server

A Spring Boot-based OAuth2 authorization server for securing microservices in the Balance application.

## Features

- ✅ OAuth2 Authorization Server with JWT tokens
- ✅ User registration and authentication
- ✅ Multiple OAuth2 client configurations
- ✅ Token introspection for microservices
- ✅ PostgreSQL database integration
- ✅ Role-based access control

## Architecture

This authorization server is designed to work with your existing microservices:

- **Fastify App** (Bank Accounts) - PostgreSQL
- **NestJS App** (Transactions) - MongoDB
- **Spring Auth Server** (Authentication) - PostgreSQL + JWT

## Quick Start

### 1. Database Setup

Create a PostgreSQL database named `authdb`:

```sql
CREATE DATABASE authdb;
```

### 2. Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/authdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The server will start on port 9000.

## OAuth2 Clients

The server is configured with three OAuth2 clients:

### 1. Web Client (Authorization Code Flow)
- **Client ID**: `web-client`
- **Client Secret**: `web-secret`
- **Grant Types**: Authorization Code, Refresh Token
- **Redirect URIs**: `http://localhost:3000/callback`
- **Scopes**: `openid`, `profile`, `read`, `write`

### 2. Fastify Service (Client Credentials Flow)
- **Client ID**: `fastify-service`
- **Client Secret**: `fastify-secret`
- **Grant Types**: Client Credentials
- **Scopes**: `read`, `write`

### 3. NestJS Service (Client Credentials Flow)
- **Client ID**: `nest-service`
- **Client Secret**: `nest-secret`
- **Grant Types**: Client Credentials
- **Scopes**: `read`, `write`

## API Endpoints

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123",
  "email": "john@example.com"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

#### Get User Profile
```http
GET /api/auth/profile
Authorization: Bearer <access_token>
```

#### Validate Token
```http
GET /api/auth/validate
Authorization: Bearer <access_token>
```

### OAuth2 Endpoints

#### Authorization Endpoint
```http
GET /oauth2/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:3000/callback&scope=openid profile read write&state=random_state
```

#### Token Endpoint
```http
POST /oauth2/token
Authorization: Basic <base64(client_id:client_secret)>
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=<authorization_code>&redirect_uri=http://localhost:3000/callback
```

#### Token Introspection
```http
POST /api/token/introspect?token=<access_token>
```

### OpenID Connect Endpoints

- **Discovery**: `/.well-known/openid_configuration`
- **JWKS**: `/.well-known/jwks.json`

## Microservice Integration

### For Fastify Service

1. **Get Client Credentials Token**:
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Authorization: Basic $(echo -n 'fastify-service:fastify-secret' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read write"
```

2. **Validate User Tokens**:
```bash
curl -X POST http://localhost:9000/api/token/introspect?token=<user_access_token>
```

### For NestJS Service

1. **Get Client Credentials Token**:
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Authorization: Basic $(echo -n 'nest-service:nest-secret' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read write"
```

2. **Validate User Tokens**:
```bash
curl -X POST http://localhost:9000/api/token/introspect?token=<user_access_token>
```

## Security Considerations

1. **Client Secrets**: In production, use proper secret management
2. **HTTPS**: Always use HTTPS in production
3. **Token Expiration**: Configure appropriate token lifetimes
4. **CORS**: Configure CORS for your frontend domains
5. **Rate Limiting**: Implement rate limiting for security endpoints

## Development

### Adding New OAuth2 Clients

Update `SecurityConfig.java` in the `registeredClientRepository()` method:

```java
RegisteredClient newClient = RegisteredClient.withId(UUID.randomUUID().toString())
    .clientId("new-service")
    .clientSecret("{noop}new-secret")
    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
    .scope("read")
    .scope("write")
    .clientSettings(ClientSettings.builder().build())
    .build();
```

### Custom Scopes

Add custom scopes in the client configuration:

```java
.scope("custom-scope")
```

## Testing

### Test User Registration
```bash
curl -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","email":"test@example.com"}'
```

### Test OAuth2 Flow
1. Open browser to: `http://localhost:9000/oauth2/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:3000/callback&scope=openid profile read write`
2. Login with test user
3. Get authorization code from redirect
4. Exchange code for token

## Troubleshooting

### Common Issues

1. **Database Connection**: Ensure PostgreSQL is running and credentials are correct
2. **Port Conflicts**: Change `server.port` in application.properties if needed
3. **CORS Issues**: Configure CORS for your frontend domains
4. **Token Validation**: Check token format and expiration

### Logs

Enable debug logging by adding to `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.it.peluso.balanceauth=DEBUG
```