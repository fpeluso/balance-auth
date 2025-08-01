// NestJS JWT Authentication Configuration
// Install required dependencies: npm install @nestjs/jwt @nestjs/passport passport passport-jwt jwks-rsa

// auth.module.ts
import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { JwtStrategy } from './jwt.strategy';
import { AuthService } from './auth.service';

@Module({
  imports: [
    PassportModule,
    JwtModule.register({
      // We'll verify tokens, not sign them
      verifyOptions: {
        issuer: 'http://localhost:9000',
      },
    }),
  ],
  providers: [JwtStrategy, AuthService],
  exports: [AuthService],
})
export class AuthModule {}

// jwt.strategy.ts
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy, ExtractJwt } from 'passport-jwt';
import { JwksClient } from 'jwks-client';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  private jwksClient: JwksClient;

  constructor() {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      issuer: 'http://localhost:9000',
      algorithms: ['RS256'],
      secretOrKeyProvider: (request, rawJwtToken, done) => {
        this.getSigningKey(rawJwtToken, done);
      },
    });

    this.jwksClient = new JwksClient({
      jwksUri: 'http://localhost:9000/.well-known/jwks.json',
      cache: true,
      cacheMaxEntries: 5,
      cacheMaxAge: 600000, // 10 minutes
    });
  }

  private getSigningKey(token: string, done: (err: any, key?: string) => void) {
    const header = JSON.parse(
      Buffer.from(token.split('.')[0], 'base64').toString()
    );

    this.jwksClient.getSigningKey(header.kid, (err, key) => {
      if (err) {
        return done(err);
      }
      const signingKey = key.getPublicKey();
      done(null, signingKey);
    });
  }

  async validate(payload: any) {
    // Validate scopes for transaction operations
    const scopes = payload.scopes || [];
    if (!scopes.includes('transaction:read') && !scopes.includes('transaction:write')) {
      throw new UnauthorizedException('Insufficient scope');
    }

    return {
      userId: payload.user_id,
      username: payload.sub,
      email: payload.email,
      roles: payload.roles,
      scopes: payload.scopes,
    };
  }
}

// auth.service.ts
import { Injectable, HttpService } from '@nestjs/common';
import { AxiosResponse } from 'axios';

@Injectable()
export class AuthService {
  constructor(private httpService: HttpService) {}

  // Get service token for service-to-service communication
  async getServiceToken(): Promise<string> {
    try {
      const response: AxiosResponse = await this.httpService
        .post(
          'http://localhost:9000/oauth2/token',
          new URLSearchParams({
            grant_type: 'client_credentials',
            scope: 'transaction:read transaction:write',
          }),
          {
            auth: {
              username: 'transactions-service',
              password: 'transaction-secret',
            },
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
            },
          }
        )
        .toPromise();

      return response.data.access_token;
    } catch (error) {
      throw new Error('Failed to get service token');
    }
  }
}

// Custom decorators for scope checking
// scopes.decorator.ts
import { SetMetadata } from '@nestjs/common';

export const SCOPES_KEY = 'scopes';
export const RequireScopes = (...scopes: string[]) =>
  SetMetadata(SCOPES_KEY, scopes);

// scopes.guard.ts
import { Injectable, CanActivate, ExecutionContext, ForbiddenException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { SCOPES_KEY } from './scopes.decorator';

@Injectable()
export class ScopesGuard implements CanActivate {
  constructor(private reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const requiredScopes = this.reflector.getAllAndOverride<string[]>(SCOPES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredScopes) {
      return true;
    }

    const { user } = context.switchToHttp().getRequest();
    const userScopes = user?.scopes || [];

    const hasScope = requiredScopes.some((scope) => userScopes.includes(scope));
    
    if (!hasScope) {
      throw new ForbiddenException(`Required scopes: ${requiredScopes.join(', ')}`);
    }

    return true;
  }
}

// Example controller usage
// transactions.controller.ts
import { Controller, Get, Post, Body, UseGuards, Request } from '@nestjs/common';
import { JwtAuthGuard } from '@nestjs/passport';
import { RequireScopes } from './scopes.decorator';
import { ScopesGuard } from './scopes.guard';

@Controller('api/transactions')
@UseGuards(JwtAuthGuard, ScopesGuard)
export class TransactionsController {
  
  @Get()
  @RequireScopes('transaction:read')
  async getTransactions(@Request() req) {
    // Access user info from req.user
    const userId = req.user.userId;
    const userRoles = req.user.roles;
    
    // Your existing logic here
    return { transactions: [], userId, userRoles };
  }

  @Post()
  @RequireScopes('transaction:write')
  async createTransaction(@Body() createTransactionDto: any, @Request() req) {
    const userId = req.user.userId;
    
    // Your existing logic here
    return { message: 'Transaction created', userId };
  }
}

// main.ts - Enable CORS
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  app.enableCors({
    origin: ['http://localhost:3000', 'http://localhost:4200'],
    credentials: true,
  });
  
  await app.listen(3001);
  console.log('NestJS transactions service running on port 3001');
}
bootstrap();

// app.module.ts - Register auth module
import { Module, HttpModule } from '@nestjs/common';
import { AuthModule } from './auth/auth.module';
import { TransactionsModule } from './transactions/transactions.module';

@Module({
  imports: [
    HttpModule,
    AuthModule,
    TransactionsModule,
    // Your other modules
  ],
  controllers: [],
  providers: [],
})
export class AppModule {}