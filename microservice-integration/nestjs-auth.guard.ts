import { Injectable, CanActivate, ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class AuthGuard implements CanActivate {
  private readonly authServerUrl = 'http://localhost:9000';
  private readonly clientId = 'nest-service';
  private readonly clientSecret = 'nest-secret';
  private serviceToken: string | null = null;
  private tokenExpiry: number = 0;

  constructor(private readonly httpService: HttpService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const authHeader = request.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing or invalid authorization header');
    }

    const userToken = authHeader.substring(7);

    try {
      const tokenInfo = await this.validateUserToken(userToken);

      if (!tokenInfo.active) {
        throw new UnauthorizedException('Invalid or expired token');
      }

      // Add user info to request
      request.user = {
        username: tokenInfo.username,
        authorities: tokenInfo.authorities || [],
        sub: tokenInfo.sub,
        scope: tokenInfo.scope
      };

      return true;
    } catch (error) {
      throw new UnauthorizedException('Token validation failed');
    }
  }

  private async getServiceToken(): Promise<string> {
    const now = Date.now();

    // Check if we have a valid service token
    if (this.serviceToken && now < this.tokenExpiry) {
      return this.serviceToken;
    }

    try {
      const response = await firstValueFrom(
        this.httpService.post(
          `${this.authServerUrl}/oauth2/token`,
          'grant_type=client_credentials&scope=read write',
          {
            headers: {
              'Authorization': `Basic ${Buffer.from(`${this.clientId}:${this.clientSecret}`).toString('base64')}`,
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }
        )
      );

      this.serviceToken = response.data.access_token;
      // Set expiry to 5 minutes before actual expiry
      this.tokenExpiry = now + (response.data.expires_in - 300) * 1000;

      return this.serviceToken;
    } catch (error) {
      throw new Error('Failed to get service token');
    }
  }

  private async validateUserToken(userToken: string): Promise<any> {
    try {
      const serviceToken = await this.getServiceToken();

      const response = await firstValueFrom(
        this.httpService.post(
          `${this.authServerUrl}/api/token/introspect`,
          null,
          {
            params: { token: userToken },
            headers: {
              'Authorization': `Bearer ${serviceToken}`
            }
          }
        )
      );

      return response.data;
    } catch (error) {
      return { active: false };
    }
  }
}

// Usage in NestJS controller:
/*
import { Controller, Get, UseGuards } from '@nestjs/common';
import { AuthGuard } from './auth.guard';

@Controller('transactions')
@UseGuards(AuthGuard)
export class TransactionsController {
  @Get()
  getTransactions() {
    return [
      { id: 1, amount: 100, description: 'Grocery shopping' },
      { id: 2, amount: -50, description: 'Salary deposit' }
    ];
  }
}
*/