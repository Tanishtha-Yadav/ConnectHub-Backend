#!/bin/bash

# ConnectHub Auth Service - cURL Testing Commands
# This script contains ready-to-use cURL commands for testing all Auth Service endpoints

BASE_URL="http://localhost:8080/api/auth"

echo "========================================="
echo "ConnectHub Auth Service - cURL Test Suite"
echo "========================================="
echo ""

# ===== 1. REGISTRATION TESTS =====
echo "1. REGISTRATION TESTS"
echo "====================="
echo ""

echo "1.1 - Register Valid User"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "1.2 - Register with Duplicate Email (should fail)"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "differentuser",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "fullName": "Another User"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "1.3 - Register with Invalid Email (should fail)"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "janedoe",
    "email": "invalid-email",
    "password": "SecurePass123!",
    "fullName": "Jane Doe"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "1.4 - Register with Weak Password (should fail)"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "weakuser",
    "email": "weak@example.com",
    "password": "short",
    "fullName": "Weak User"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "1.5 - Register with Empty Fields (should fail)"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "email": "",
    "password": "",
    "fullName": ""
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 2. LOGIN TESTS =====
echo "2. LOGIN TESTS"
echo "=============="
echo ""

echo "2.1 - Login with Valid Credentials"
LOGIN_RESPONSE=$(curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s)

echo "$LOGIN_RESPONSE" | jq '.'
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token' | grep -v "Status")

echo ""
echo "2.2 - Login with Wrong Password (should fail)"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "WrongPassword123!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "2.3 - Login with Non-existent User (should fail)"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@example.com",
    "password": "Password123!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "2.4 - Login with Empty Email (should fail)"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "",
    "password": "SecurePass123!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "2.5 - Login with Empty Password (should fail)"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": ""
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 3. TOKEN VALIDATION TESTS =====
echo "3. TOKEN VALIDATION TESTS"
echo "========================="
echo ""

echo "3.1 - Validate Valid Token"
if [ ! -z "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  curl -X GET "$BASE_URL/validate/$TOKEN" \
    -w "\nStatus Code: %{http_code}\n" \
    -s | jq '.'
else
  echo "No valid token available. Run login test first."
fi

echo ""
echo "3.2 - Validate Invalid Token"
curl -X GET "$BASE_URL/validate/invalid_token_here" \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "3.3 - Validate Malformed Token"
curl -X GET "$BASE_URL/validate/not.a.valid.jwt" \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 4. REFRESH TOKEN TESTS =====
echo "4. REFRESH TOKEN TESTS"
echo "======================"
echo ""

echo "4.1 - Refresh Token with Valid Token"
if [ ! -z "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  REFRESH_RESPONSE=$(curl -X POST "$BASE_URL/refresh" \
    -H "Authorization: Bearer $TOKEN" \
    -w "\nStatus Code: %{http_code}\n" \
    -s)
  
  echo "$REFRESH_RESPONSE" | jq '.'
  NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.token' | grep -v "Status")
else
  echo "No valid token available. Run login test first."
fi

echo ""
echo "4.2 - Refresh Token without Authorization Header (should fail)"
curl -X POST "$BASE_URL/refresh" \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 5. PROTECTED ENDPOINT TESTS =====
echo "5. PROTECTED ENDPOINT TESTS"
echo "==========================="
echo ""

echo "5.1 - Get User Profile with Valid Token"
if [ ! -z "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  # Extract user ID from token (assuming you have it stored)
  USER_ID="test-user-id-here"
  curl -X GET "$BASE_URL/profile/$USER_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -w "\nStatus Code: %{http_code}\n" \
    -s | jq '.'
else
  echo "No valid token available. Run login test first."
fi

echo ""
echo "5.2 - Get User Profile without Token (should return 401)"
USER_ID="test-user-id-here"
curl -X GET "$BASE_URL/profile/$USER_ID" \
  -w "\nStatus Code: %{http_code}\n" \
  -s

echo ""
echo "5.3 - Get User Profile with Invalid Token (should return 401)"
curl -X GET "$BASE_URL/profile/$USER_ID" \
  -H "Authorization: Bearer invalid_token" \
  -w "\nStatus Code: %{http_code}\n" \
  -s

echo ""
echo ""

# ===== 6. LOGOUT TESTS =====
echo "6. LOGOUT TESTS"
echo "==============="
echo ""

echo "6.1 - Logout User"
USER_ID="test-user-id-here"
curl -X POST "$BASE_URL/logout/$USER_ID" \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 7. PASSWORD MANAGEMENT TESTS =====
echo "7. PASSWORD MANAGEMENT TESTS"
echo "============================"
echo ""

echo "7.1 - Change Password"
USER_ID="test-user-id-here"
curl -X POST "$BASE_URL/password/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "SecurePass123!",
    "newPassword": "NewSecurePass456!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "7.2 - Change Password with Wrong Old Password (should fail)"
curl -X POST "$BASE_URL/password/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "WrongPassword123!",
    "newPassword": "NewSecurePass456!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 8. SECURITY TESTS =====
echo "8. SECURITY TESTS"
echo "================="
echo ""

echo "8.1 - SQL Injection Test (Email Field) - Should be rejected"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'\'' OR '\''1'\''='\''1",
    "password": "Password123!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "8.2 - SQL Injection Test (Password Field) - Should be rejected"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "'\'' OR '\''1'\''='\''1'\'' --"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "8.3 - XSS Injection Test - Should be rejected"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "xssuser",
    "email": "xss@example.com",
    "password": "SecurePass123!",
    "fullName": "<script>alert(\"XSS\")</script>"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "8.4 - Command Injection Test - Should be rejected"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com; rm -rf /",
    "password": "Password123!"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo ""

# ===== 9. EDGE CASES =====
echo "9. EDGE CASES"
echo "============="
echo ""

echo "9.1 - Register with Duplicate Username"
curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "different@example.com",
    "password": "SecurePass123!",
    "fullName": "Different User"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "9.2 - Login with Special Characters in Password"
curl -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "P@ssw0rd!#$%^&*()"
  }' \
  -w "\nStatus Code: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "========================================="
echo "Testing Complete!"
echo "========================================="
