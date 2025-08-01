# Microservices Integration Guide

This guide explains how to integrate your existing Fastify and Nest microservices with the OAuth2 Authorization Server.

## Overview

Your microservices architecture now includes:
- **Fastify App** (Bank Accounts) - Port 3000
- **Nest App** (Transactions) - Port 3001  
- **Spring Auth Server** (OAuth2) - Port 9000

## 1. Fastify App Integration

### Install Required Dependencies

```bash
npm install jsonwebtoken jwks-client
```

### Create JWT Middleware

Create `middleware/jwt-auth.js`:

```javascript
const jwt = require('jsonwebtoken');
const jwksClient = require('jwks-client');

const client = jwksClient({
  jwksUri: 'http://localhost:9000/oauth2/jwks'
});

function getKey(header, callback) {
  client.getSigningKey(header.kid, function(err, key) {
    const signingKey = key.publicKey || key.rsaPublicKey;
    callback(null, signingKey);
  });
}

const jwtAuthMiddleware = async (request, reply) => {
  try {
    const authHeader = request.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return reply.code(401).send({ 
        error: 'Missing or invalid authorization header' 
      });
    }
    
    const token = authHeader.substring(7);
    
    jwt.verify(token, getKey, { 
      algorithms: ['RS256'],
      issuer: 'http://localhost:9000',
      audience: 'oidc-client'
    }, (err, decoded) => {
      if (err) {
        return reply.code(401).send({ 
          error: 'Invalid token',
          details: err.message 
        });
      }
      
      // Add user info to request
      request.user = decoded;
      request.userId = decoded.sub;
    });
  } catch (error) {
    return reply.code(500).send({ 
      error: 'Authentication error',
      details: error.message 
    });
  }
};

module.exports = jwtAuthMiddleware;
```

### Apply Middleware to Routes

```javascript
const fastify = require('fastify')();
const jwtAuth = require('./middleware/jwt-auth');

// Apply to specific routes
fastify.get('/api/bank-accounts', { preHandler: jwtAuth }, async (request, reply) => {
  // request.user contains the decoded JWT
  const userId = request.userId;
  
  // Your bank account logic here
  return { 
    message: 'Bank accounts for user: ' + userId,
    accounts: [] 
  };
});

// Apply to all routes under /api
fastify.addHook('preHandler', async (request, reply) => {
  if (request.url.startsWith('/api/')) {
    return jwtAuth(request, reply);
  }
});
```

### Environment Configuration

Create `.env` file:

```env
AUTH_SERVER_URL=http://localhost:9000
JWKS_URI=http://localhost:9000/oauth2/jwks
```

## 2. Nest App Integration

### Install Required Dependencies

```bash
npm install @nestjs/jwt @nestjs/passport passport passport-jwt jwks-client
npm install --save-dev @types/passport-jwt
```

### Create JWT Strategy

Create `auth/jwt.strategy.ts`:

```typescript
import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import * as jwksClient from 'jwks-client';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor() {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKeyProvider: jwksClient.passportJwtSecret({
        cache: true,
        rateLimit: true,
        jwksRequestsPerMinute: 5,
        jwksUri: 'http://localhost:9000/oauth2/jwks',
      }),
      issuer: 'http://localhost:9000',
      audience: 'oidc-client',
    });
  }

  async validate(payload: any) {
    return {
      userId: payload.sub,
      username: payload.sub,
      roles: payload.roles || [],
    };
  }
}
```

### Create JWT Auth Guard

Create `auth/jwt-auth.guard.ts`:

```typescript
import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {}
```

### Update App Module

```typescript
import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { JwtStrategy } from './auth/jwt.strategy';
import { TransactionController } from './transaction.controller';
import { TransactionService } from './transaction.service';

@Module({
  imports: [
    PassportModule,
    JwtModule.register({
      secret: 'your-secret-key', // Not used for verification, just for signing
      signOptions: { expiresIn: '1h' },
    }),
  ],
  controllers: [TransactionController],
  providers: [TransactionService, JwtStrategy],
})
export class AppModule {}
```

### Protect Routes

```typescript
import { Controller, Get, UseGuards, Request } from '@nestjs/common';
import { JwtAuthGuard } from './auth/jwt-auth.guard';

@Controller('transactions')
@UseGuards(JwtAuthGuard)
export class TransactionController {
  
  @Get()
  async getTransactions(@Request() req) {
    const userId = req.user.userId;
    
    // Your transaction logic here
    return {
      message: 'Transactions for user: ' + userId,
      transactions: []
    };
  }
}
```

## 3. Testing the Integration

### 1. Start All Services

```bash
# Terminal 1: Start PostgreSQL
docker-compose up -d

# Terminal 2: Start Auth Server
cd balance-auth
mvn spring-boot:run

# Terminal 3: Start Fastify App
cd fastify-bank-service
npm start

# Terminal 4: Start Nest App  
cd nest-transaction-service
npm run start:dev
```

### 2. Test Authentication Flow

```bash
# 1. Register a user
curl -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","email":"test@example.com"}'

# 2. Login to get token
TOKEN=$(curl -s -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' | \
  jq -r '.access_token')

# 3. Test Fastify endpoint
curl -X GET http://localhost:3000/api/bank-accounts \
  -H "Authorization: Bearer $TOKEN"

# 4. Test Nest endpoint
curl -X GET http://localhost:3001/transactions \
  -H "Authorization: Bearer $TOKEN"
```

## 4. Production Considerations

### Environment Variables

Create environment-specific configurations:

```env
# Development
AUTH_SERVER_URL=http://localhost:9000
JWKS_URI=http://localhost:9000/oauth2/jwks

# Production
AUTH_SERVER_URL=https://auth.yourdomain.com
JWKS_URI=https://auth.yourdomain.com/oauth2/jwks
```

### Error Handling

Implement proper error handling for authentication failures:

```javascript
// Fastify
fastify.setErrorHandler((error, request, reply) => {
  if (error.statusCode === 401) {
    return reply.code(401).send({
      error: 'Unauthorized',
      message: 'Invalid or missing authentication token'
    });
  }
  // Handle other errors
});

// Nest
@Catch(UnauthorizedException)
export class UnauthorizedExceptionFilter implements ExceptionFilter {
  catch(exception: UnauthorizedException, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse();
    
    response.status(401).json({
      error: 'Unauthorized',
      message: 'Invalid or missing authentication token',
      timestamp: new Date().toISOString(),
    });
  }
}
```

### CORS Configuration

Configure CORS for cross-origin requests:

```javascript
// Fastify
fastify.register(require('@fastify/cors'), {
  origin: ['http://localhost:3000', 'http://localhost:3001'],
  credentials: true
});

// Nest
app.enableCors({
  origin: ['http://localhost:3000', 'http://localhost:3001'],
  credentials: true,
});
```

### Health Checks

Add health check endpoints to verify service connectivity:

```javascript
// Fastify
fastify.get('/health', async (request, reply) => {
  return { status: 'ok', timestamp: new Date().toISOString() };
});

// Nest
@Get('health')
getHealth() {
  return { status: 'ok', timestamp: new Date().toISOString() };
}
```

## 5. Monitoring and Logging

### Add Request Logging

```javascript
// Fastify
fastify.addHook('onRequest', (request, reply, done) => {
  console.log(`${new Date().toISOString()} - ${request.method} ${request.url}`);
  done();
});

// Nest
@Injectable()
export class LoggingInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest();
    console.log(`${new Date().toISOString()} - ${request.method} ${request.url}`);
    return next.handle();
  }
}
```

### Token Validation Logging

```javascript
// Add logging to JWT validation
console.log(`Validating token for user: ${decoded.sub}`);
console.log(`Token scopes: ${decoded.scope}`);
```

## 6. Troubleshooting

### Common Issues

1. **CORS Errors**: Ensure CORS is properly configured
2. **Token Expiration**: Implement token refresh logic
3. **JWKS Fetch Failures**: Check network connectivity to auth server
4. **Invalid Token Format**: Verify token structure and signature

### Debug Commands

```bash
# Check auth server health
curl http://localhost:9000/oauth2/jwks

# Validate token manually
curl -X GET http://localhost:9000/api/auth/validate \
  -H "Authorization: Bearer YOUR_TOKEN"

# Check service connectivity
curl http://localhost:3000/health
curl http://localhost:3001/health
```

This integration guide provides a complete setup for securing your microservices with OAuth2 JWT tokens. The authentication flow ensures that only authenticated users can access your bank account and transaction data.