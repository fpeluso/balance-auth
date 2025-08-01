# Balance Auth - OAuth2 Authorization Server

This is a Spring Boot-based OAuth2 Authorization Server that provides JWT-based authentication and authorization for your microservices architecture.

## Features

- ✅ OAuth2 Authorization Server with JWT tokens
- ✅ User registration and authentication
- ✅ Role-based access control (RBAC)
- ✅ JWT token validation and refresh
- ✅ PostgreSQL database for user storage
- ✅ Support for multiple OAuth2 clients (microservices)
- ✅ Admin endpoints for user management

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Fastify App   │    │   Nest App      │    │   Web Client    │
│  (Bank Accounts)│    │ (Transactions)  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │  Balance Auth   │
                    │ (OAuth2 Server) │
                    └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │   PostgreSQL    │
                    │   (Users DB)    │
                    └─────────────────┘
```

## Prerequisites

- Java 21
- Maven
- PostgreSQL
- Docker (optional)

## Setup

### 1. Database Setup

Create a PostgreSQL database:

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
mvn spring-boot:run
```

The application will start on port 9000.

## API Endpoints

### Authentication Endpoints

#### Register a new user
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

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "openid profile read write"
}
```

#### Refresh Token
```http
POST /api/auth/refresh?refresh_token=your_refresh_token
```

#### Validate Token
```http
GET /api/auth/validate
Authorization: Bearer your_access_token
```

### User Management Endpoints

#### Get User Profile
```http
GET /api/users/profile
Authorization: Bearer your_access_token
```

#### Admin: Get All Users (requires ADMIN role)
```http
GET /api/users/admin/all
Authorization: Bearer your_access_token
```

#### Admin: Delete User (requires ADMIN role)
```http
DELETE /api/users/admin/{userId}
Authorization: Bearer your_access_token
```

#### Admin: Update User Roles (requires ADMIN role)
```http
PUT /api/users/admin/{userId}/roles
Authorization: Bearer your_access_token
Content-Type: application/json

{
  "roles": ["USER", "ADMIN"]
}
```

### OAuth2 Endpoints

#### Authorization Endpoint
```
GET /oauth2/authorize?response_type=code&client_id=your_client_id&redirect_uri=your_redirect_uri&scope=openid profile&state=random_state
```

#### Token Endpoint
```http
POST /oauth2/token
Authorization: Basic base64(client_id:client_secret)
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=authorization_code&redirect_uri=your_redirect_uri
```

#### JWK Set Endpoint
```
GET /oauth2/jwks
```

## Integrating with Microservices

### 1. Fastify App Integration

Add JWT validation middleware to your Fastify app:

```javascript
const fastify = require('fastify')();
const jwt = require('jsonwebtoken');

// JWT validation middleware
fastify.addHook('preHandler', async (request, reply) => {
  const authHeader = request.headers.authorization;
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return reply.code(401).send({ error: 'Missing or invalid authorization header' });
  }
  
  const token = authHeader.substring(7);
  
  try {
    // Verify JWT token using the public key from /oauth2/jwks
    const decoded = jwt.verify(token, publicKey, { algorithms: ['RS256'] });
    request.user = decoded;
  } catch (error) {
    return reply.code(401).send({ error: 'Invalid token' });
  }
});
```

### 2. Nest App Integration

Add JWT validation to your Nest app:

```typescript
import { JwtAuthGuard } from './guards/jwt-auth.guard';

@Controller('transactions')
@UseGuards(JwtAuthGuard)
export class TransactionController {
  // Your protected endpoints
}
```

## Security Features

### JWT Token Structure

The JWT tokens contain:
- **Subject (sub)**: Username
- **Issued At (iat)**: Token creation time
- **Expiration (exp)**: Token expiration time
- **Issuer (iss)**: Authorization server URL
- **Audience (aud)**: Client ID
- **Scopes**: Granted permissions

### Token Expiration

- **Access Token**: 1 hour
- **Refresh Token**: 30 days

### Roles and Permissions

- **USER**: Basic user permissions
- **ADMIN**: Administrative permissions

## Development

### Adding New OAuth2 Clients

Use the `ClientService` to register new microservices:

```java
@Autowired
private ClientService clientService;

// Register a new microservice
clientService.registerMicroserviceClient(
    "new-service",
    "service-secret",
    "http://localhost:3002/callback"
);
```

### Customizing Token Claims

Modify the `AuthenticationService` to add custom claims to JWT tokens:

```java
// Add custom claims
Map<String, Object> claims = new HashMap<>();
claims.put("user_id", user.getId());
claims.put("email", user.getEmail());
```

## Testing

### Using curl

```bash
# Register a user
curl -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","email":"test@example.com"}'

# Login
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Use the token
curl -X GET http://localhost:9000/api/users/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Production Considerations

1. **Database**: Use a production-ready database with proper backups
2. **Secrets**: Store client secrets securely (use environment variables)
3. **HTTPS**: Always use HTTPS in production
4. **Token Storage**: Store refresh tokens securely
5. **Monitoring**: Add logging and monitoring
6. **Rate Limiting**: Implement rate limiting for auth endpoints
7. **CORS**: Configure CORS properly for your domains

## Troubleshooting

### Common Issues

1. **Database Connection**: Ensure PostgreSQL is running and accessible
2. **Port Conflicts**: Check if port 9000 is available
3. **JWT Validation**: Verify the public key is correctly configured
4. **CORS Issues**: Configure CORS for your frontend domains

### Logs

Enable debug logging by adding to `application.properties`:

```properties
logging.level.org.springframework.security=DEBUG
logging.level.it.peluso.balanceauth=DEBUG
```