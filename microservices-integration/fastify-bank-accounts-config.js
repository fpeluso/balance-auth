// Fastify JWT Authentication Plugin Configuration
// Install required dependencies: npm install @fastify/jwt @fastify/cors jwks-client

const jwksClient = require('jwks-client');
const fp = require('fastify-plugin');

// JWKS client for retrieving public keys
const client = jwksClient({
  jwksUri: 'http://localhost:9000/.well-known/jwks.json',
  cache: true,
  cacheMaxEntries: 5,
  cacheMaxAge: 600000, // 10 minutes
});

// Function to get signing key
function getKey(header, callback) {
  client.getSigningKey(header.kid, (err, key) => {
    if (err) {
      return callback(err);
    }
    const signingKey = key.publicKey || key.rsaPublicKey;
    callback(null, signingKey);
  });
}

// Fastify authentication plugin
async function authPlugin(fastify, options) {
  // Register JWT plugin
  await fastify.register(require('@fastify/jwt'), {
    secret: {
      public: getKey,
    },
    verify: {
      issuer: 'http://localhost:9000',
      audience: false, // We'll check scopes instead
    },
  });

  // Register CORS
  await fastify.register(require('@fastify/cors'), {
    origin: ['http://localhost:3000', 'http://localhost:4200'],
    credentials: true,
  });

  // Authentication decorator
  fastify.decorate('authenticate', async function (request, reply) {
    try {
      await request.jwtVerify();
      
      // Check if user has required scope for bank operations
      const scopes = request.user.scopes || [];
      if (!scopes.includes('bank:read') && !scopes.includes('bank:write')) {
        reply.code(403).send({ error: 'Insufficient scope' });
        return;
      }
    } catch (err) {
      reply.code(401).send({ error: 'Unauthorized' });
    }
  });

  // Scope checking decorator
  fastify.decorate('requireScope', (requiredScope) => {
    return async function (request, reply) {
      const scopes = request.user?.scopes || [];
      if (!scopes.includes(requiredScope)) {
        reply.code(403).send({ error: `Required scope: ${requiredScope}` });
        return;
      }
    };
  });
}

module.exports = fp(authPlugin);

// Example usage in your bank accounts routes:
/*
// app.js or your main file
const fastify = require('fastify')({ logger: true });

// Register authentication plugin
fastify.register(require('./auth-plugin'));

// Protected routes
fastify.register(async function (fastify) {
  // Prehandler for all routes in this context
  fastify.addHook('preHandler', fastify.authenticate);

  // Get bank accounts (requires bank:read scope)
  fastify.get('/api/bank-accounts', {
    preHandler: [fastify.requireScope('bank:read')]
  }, async (request, reply) => {
    // Access user info from request.user
    const userId = request.user.user_id;
    const userRoles = request.user.roles;
    
    // Your existing logic here
    return { accounts: [], userId, userRoles };
  });

  // Create bank account (requires bank:write scope)
  fastify.post('/api/bank-accounts', {
    preHandler: [fastify.requireScope('bank:write')]
  }, async (request, reply) => {
    const userId = request.user.user_id;
    // Your existing logic here
    return { message: 'Account created', userId };
  });
});

// Health check (no authentication required)
fastify.get('/health', async (request, reply) => {
  return { status: 'ok' };
});

const start = async () => {
  try {
    await fastify.listen({ port: 3000 });
    console.log('Fastify bank accounts service running on port 3000');
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
};
start();
*/

// Example client credentials flow for service-to-service communication:
/*
const axios = require('axios');

async function getServiceToken() {
  const response = await axios.post('http://localhost:9000/oauth2/token', 
    new URLSearchParams({
      'grant_type': 'client_credentials',
      'scope': 'bank:read bank:write'
    }), {
      auth: {
        username: 'bank-accounts-service',
        password: 'bank-secret'
      },
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    }
  );
  
  return response.data.access_token;
}
*/