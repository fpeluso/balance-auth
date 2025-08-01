#!/bin/bash

# Test script for Balance Auth OAuth2 Server
# Make sure the server is running on localhost:9000

BASE_URL="http://localhost:9000"
AUTH_URL="$BASE_URL/api/auth"
USERS_URL="$BASE_URL/api/users"

echo "🧪 Testing Balance Auth OAuth2 Server"
echo "====================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

# Test 1: Register a new user
echo -e "\n${YELLOW}1. Testing user registration...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$AUTH_URL/register" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "testuser",
        "password": "password123",
        "email": "test@example.com"
    }')

if echo "$REGISTER_RESPONSE" | grep -q "User registered successfully"; then
    print_status 0 "User registration successful"
else
    print_status 1 "User registration failed: $REGISTER_RESPONSE"
fi

# Test 2: Login
echo -e "\n${YELLOW}2. Testing user login...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$AUTH_URL/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "testuser",
        "password": "password123"
    }')

# Extract access token
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ]; then
    print_status 0 "Login successful, access token obtained"
    echo "   Access token: ${ACCESS_TOKEN:0:50}..."
else
    print_status 1 "Login failed: $LOGIN_RESPONSE"
    exit 1
fi

# Test 3: Validate token
echo -e "\n${YELLOW}3. Testing token validation...${NC}"
VALIDATE_RESPONSE=$(curl -s -X GET "$AUTH_URL/validate" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$VALIDATE_RESPONSE" | grep -q "Token is valid"; then
    print_status 0 "Token validation successful"
else
    print_status 1 "Token validation failed: $VALIDATE_RESPONSE"
fi

# Test 4: Get user profile
echo -e "\n${YELLOW}4. Testing user profile endpoint...${NC}"
PROFILE_RESPONSE=$(curl -s -X GET "$USERS_URL/profile" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$PROFILE_RESPONSE" | grep -q "testuser"; then
    print_status 0 "User profile retrieval successful"
else
    print_status 1 "User profile retrieval failed: $PROFILE_RESPONSE"
fi

# Test 5: Test OAuth2 endpoints
echo -e "\n${YELLOW}5. Testing OAuth2 endpoints...${NC}"

# Test JWK endpoint
JWK_RESPONSE=$(curl -s -X GET "$BASE_URL/oauth2/jwks")
if echo "$JWK_RESPONSE" | grep -q "keys"; then
    print_status 0 "JWK endpoint accessible"
else
    print_status 1 "JWK endpoint failed: $JWK_RESPONSE"
fi

# Test authorization endpoint (should redirect to login)
AUTH_ENDPOINT_RESPONSE=$(curl -s -I "$BASE_URL/oauth2/authorize?response_type=code&client_id=oidc-client&redirect_uri=http://localhost:8080/callback&scope=openid&state=test")
if echo "$AUTH_ENDPOINT_RESPONSE" | grep -q "302"; then
    print_status 0 "Authorization endpoint accessible"
else
    print_status 1 "Authorization endpoint failed"
fi

echo -e "\n${GREEN}🎉 All tests completed!${NC}"
echo -e "\n${YELLOW}Next steps:${NC}"
echo "1. Use the access token to authenticate requests to your microservices"
echo "2. Configure your Fastify and Nest apps to validate JWT tokens"
echo "3. Set up proper CORS and security headers for production"
echo -e "\n${YELLOW}Example usage:${NC}"
echo "curl -X GET http://localhost:3000/api/bank-accounts \\"
echo "  -H \"Authorization: Bearer $ACCESS_TOKEN\""