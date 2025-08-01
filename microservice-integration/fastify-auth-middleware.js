const fastify = require('fastify')();
const axios = require('axios');

// Configuration
const AUTH_SERVER_URL = 'http://localhost:9000';
const CLIENT_ID = 'fastify-service';
const CLIENT_SECRET = 'fastify-secret';

// Get service token (client credentials)
async function getServiceToken() {
    try {
        const response = await axios.post(`${AUTH_SERVER_URL}/oauth2/token`, 
            'grant_type=client_credentials&scope=read write',
            {
                headers: {
                    'Authorization': `Basic ${Buffer.from(`${CLIENT_ID}:${CLIENT_SECRET}`).toString('base64')}`,
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            }
        );
        return response.data.access_token;
    } catch (error) {
        console.error('Error getting service token:', error.message);
        throw error;
    }
}

// Validate user token
async function validateUserToken(userToken) {
    try {
        const serviceToken = await getServiceToken();
        
        const response = await axios.post(`${AUTH_SERVER_URL}/api/token/introspect`, null, {
            params: { token: userToken },
            headers: {
                'Authorization': `Bearer ${serviceToken}`
            }
        });
        
        return response.data;
    } catch (error) {
        console.error('Error validating user token:', error.message);
        return { active: false };
    }
}

// Authentication middleware
async function authMiddleware(request, reply) {
    const authHeader = request.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return reply.code(401).send({ error: 'Missing or invalid authorization header' });
    }
    
    const token = authHeader.substring(7);
    
    try {
        const tokenInfo = await validateUserToken(token);
        
        if (!tokenInfo.active) {
            return reply.code(401).send({ error: 'Invalid or expired token' });
        }
        
        // Add user info to request
        request.user = {
            username: tokenInfo.username,
            authorities: tokenInfo.authorities || [],
            sub: tokenInfo.sub
        };
        
    } catch (error) {
        return reply.code(401).send({ error: 'Token validation failed' });
    }
}

// Example usage in Fastify routes
fastify.addHook('preHandler', authMiddleware);

// Protected route example
fastify.get('/api/accounts', async (request, reply) => {
    return {
        message: `Hello ${request.user.username}!`,
        accounts: [
            { id: 1, name: 'Main Account', balance: 1000 },
            { id: 2, name: 'Savings', balance: 5000 }
        ]
    };
});

// Start server
fastify.listen({ port: 3001 }, (err) => {
    if (err) throw err;
    console.log('Fastify server running on port 3001');
});

module.exports = { authMiddleware, validateUserToken, getServiceToken };