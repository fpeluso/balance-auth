const axios = require('axios');

const AUTH_SERVER_URL = 'http://localhost:9000';

async function testAuthServer() {
    console.log('🧪 Testing Authorization Server...\n');

    try {
        // 1. Test user registration
        console.log('1. Testing user registration...');
        const registerResponse = await axios.post(`${AUTH_SERVER_URL}/api/auth/register`, {
            username: 'testuser',
            password: 'password123',
            email: 'test@example.com'
        });
        console.log('✅ User registered successfully\n');

        // 2. Test user login
        console.log('2. Testing user login...');
        const loginResponse = await axios.post(`${AUTH_SERVER_URL}/api/auth/login`, {
            username: 'testuser',
            password: 'password123'
        });
        console.log('✅ User login successful');
        console.log('Response:', loginResponse.data, '\n');

        // 3. Test OAuth2 authorization endpoint
        console.log('3. Testing OAuth2 authorization endpoint...');
        const authUrl = `${AUTH_SERVER_URL}/oauth2/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:3000/callback&scope=openid profile read write&state=test123`;
        console.log('Authorization URL:', authUrl);
        console.log('⚠️  Open this URL in browser to complete OAuth2 flow\n');

        // 4. Test service token (client credentials)
        console.log('4. Testing service token (client credentials)...');
        const serviceTokenResponse = await axios.post(`${AUTH_SERVER_URL}/oauth2/token`,
            'grant_type=client_credentials&scope=read write',
            {
                headers: {
                    'Authorization': 'Basic ' + Buffer.from('fastify-service:fastify-secret').toString('base64'),
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            }
        );
        console.log('✅ Service token obtained successfully');
        console.log('Token:', serviceTokenResponse.data.access_token.substring(0, 20) + '...\n');

        // 5. Test OpenID Connect discovery
        console.log('5. Testing OpenID Connect discovery...');
        const discoveryResponse = await axios.get(`${AUTH_SERVER_URL}/.well-known/openid_configuration`);
        console.log('✅ OpenID Connect discovery successful');
        console.log('Issuer:', discoveryResponse.data.issuer);
        console.log('Authorization endpoint:', discoveryResponse.data.authorization_endpoint);
        console.log('Token endpoint:', discoveryResponse.data.token_endpoint, '\n');

        // 6. Test JWKS endpoint
        console.log('6. Testing JWKS endpoint...');
        const jwksResponse = await axios.get(`${AUTH_SERVER_URL}/.well-known/jwks.json`);
        console.log('✅ JWKS endpoint working');
        console.log('Keys available:', jwksResponse.data.keys.length, '\n');

        console.log('🎉 All tests passed! Authorization server is working correctly.');

    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
        
        if (error.response?.status === 401) {
            console.log('\n💡 Make sure the authorization server is running on port 9000');
        }
    }
}

// Run tests
testAuthServer();