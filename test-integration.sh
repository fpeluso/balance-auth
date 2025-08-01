#!/bin/bash

# Test script for OAuth2 Authorization Server
echo "Testing OAuth2 Authorization Server Integration..."

# Base URL
BASE_URL="http://localhost:9000"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
    fi
}

# Test 1: Check if server is running
echo -e "${YELLOW}1. Testing server health...${NC}"
response=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/health)
print_result $([ "$response" -eq 200 ] && echo 0 || echo 1) "Server health check"

# Test 2: Check JWKS endpoint
echo -e "${YELLOW}2. Testing JWKS endpoint...${NC}"
response=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/.well-known/jwks.json)
print_result $([ "$response" -eq 200 ] && echo 0 || echo 1) "JWKS endpoint accessible"

# Test 3: Register a test user
echo -e "${YELLOW}3. Testing user registration...${NC}"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }')
print_result $([ "$response" -eq 200 ] && echo 0 || echo 1) "User registration"

# Test 4: Get client credentials token for bank service
echo -e "${YELLOW}4. Testing client credentials flow (Bank Service)...${NC}"
token_response=$(curl -s -X POST $BASE_URL/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "bank-accounts-service:bank-secret" \
  -d "grant_type=client_credentials&scope=bank:read bank:write")

if echo "$token_response" | grep -q "access_token"; then
    print_result 0 "Bank service client credentials token"
    BANK_TOKEN=$(echo "$token_response" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
    echo "Bank Token: ${BANK_TOKEN:0:20}..."
else
    print_result 1 "Bank service client credentials token"
fi

# Test 5: Get client credentials token for transaction service
echo -e "${YELLOW}5. Testing client credentials flow (Transaction Service)...${NC}"
token_response=$(curl -s -X POST $BASE_URL/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "transactions-service:transaction-secret" \
  -d "grant_type=client_credentials&scope=transaction:read transaction:write")

if echo "$token_response" | grep -q "access_token"; then
    print_result 0 "Transaction service client credentials token"
    TRANSACTION_TOKEN=$(echo "$token_response" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
    echo "Transaction Token: ${TRANSACTION_TOKEN:0:20}..."
else
    print_result 1 "Transaction service client credentials token"
fi

# Test 6: Token introspection
if [ ! -z "$BANK_TOKEN" ]; then
    echo -e "${YELLOW}6. Testing token introspection...${NC}"
    introspect_response=$(curl -s -X POST $BASE_URL/api/auth/token/introspect \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "token=$BANK_TOKEN&token_type_hint=access_token")
    
    if echo "$introspect_response" | grep -q '"active":true'; then
        print_result 0 "Token introspection"
    else
        print_result 1 "Token introspection"
    fi
fi

# Test 7: Authorization endpoint
echo -e "${YELLOW}7. Testing authorization endpoint...${NC}"
auth_url="$BASE_URL/oauth2/authorize?response_type=code&client_id=expense-tracker-frontend&redirect_uri=http://localhost:4200/login/oauth2/code/expense-tracker&scope=openid profile email"
response=$(curl -s -o /dev/null -w "%{http_code}" "$auth_url")
# Should redirect to login page (302) or show login form (200)
print_result $([ "$response" -eq 302 ] || [ "$response" -eq 200 ] && echo 0 || echo 1) "Authorization endpoint"

echo ""
echo -e "${YELLOW}=== Integration Test Summary ===${NC}"
echo "If all tests passed, your OAuth2 Authorization Server is ready!"
echo ""
echo "Next steps:"
echo "1. Integrate with your Fastify service using the provided configuration"
echo "2. Integrate with your NestJS service using the provided configuration"
echo "3. Create a frontend application that uses the authorization code flow"
echo ""
echo "Authorization URLs for manual testing:"
echo "Frontend: $auth_url"
echo ""
echo "Client Credentials for testing:"
echo "Bank Service: bank-accounts-service / bank-secret"
echo "Transaction Service: transactions-service / transaction-secret"
echo "Frontend: expense-tracker-frontend / frontend-secret"