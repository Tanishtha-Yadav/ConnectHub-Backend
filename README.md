# ConnectHub Backend - Auth Service

Welcome! This is the backend server that powers user login, registration, and profile management for ConnectHub.

## 🎯 What Does This Do?

This is a **Spring Boot backend service** that handles:
- ✅ User registration (sign up)
- ✅ User login (sign in with email/password)
- ✅ User profiles (view & edit user info)
- ✅ Password changes
- ✅ User search
- ✅ Online status tracking
- ✅ Google & GitHub login support

**Think of it as:** The server that keeps track of all users and handles their accounts.

---

## 📋 Before You Start

Make sure you have these installed on your computer:

1. **Java 17 or newer**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Check: Open terminal and type `java -version`

2. **MySQL Database**
   - Download from: https://www.mysql.com/downloads/
   - Or use Docker: `docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=password mysql:8.0`

3. **Maven** (usually comes with Java)
   - Check: Type `mvn -version` in terminal

---

## 🚀 Getting Started (Step by Step)

### Step 1: Open the Project

```bash
# Open terminal/command prompt and navigate to the backend folder
cd auth-service

# You should see a file called "pom.xml" here
# If not, you're in the wrong folder!
```

### Step 2: Configure Database Connection

Open the file: `src/main/resources/application.properties`

Look for this line and update it with your MySQL info:

```properties
# Change "your_password" to your MySQL password
spring.datasource.url=jdbc:mysql://localhost:3306/connecthub
spring.datasource.username=root
spring.datasource.password=your_password
```

**Don't have MySQL running?** Start it:
```bash
# If you used Docker:
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=password mysql:8.0
```

### Step 3: Create the Database

Open MySQL and run:
```sql
CREATE DATABASE connecthub;
```

**Using MySQL command line:**
```bash
mysql -u root -p
# Then paste the CREATE DATABASE command above
```

### Step 4: Start the Backend Service

```bash
# In the auth-service folder, run:
./mvnw spring-boot:run

# On Windows, use:
mvnw.cmd spring-boot:run
```

**Wait for this message to appear:**
```
2026-04-18 10:00:00 - Started AuthServiceApplication in 5.234 seconds
```

✅ Your backend is now running on `http://localhost:8081`

### Step 5: Test It Works

Open a new terminal and run:

```bash
curl http://localhost:8081/api/auth/health
```

You should see:
```json
{
  "status": "Auth Service is running",
  "timestamp": "2026-04-18T10:00:00"
}
```

🎉 **Success!** Your backend is working!

---


---

## 📚 What's Inside?

This project has all the code to handle user accounts. Here's where everything is:

```
auth-service/
├── src/main/java/com/connecthub/auth/
│   ├── AuthServiceApplication.java      ← Main file that starts everything
│   ├── controller/                      ← Receives requests from frontend
│   │   ├── AuthController.java          ← Login, register, profile endpoints
│   │   └── OAuth2Controller.java        ← Google/GitHub login
│   ├── service/                         ← Business logic (the "thinking" part)
│   │   ├── AuthServiceImpl.java          ← Handles login/register logic
│   │   └── OAuth2ServiceImpl.java        ← Handles Google/GitHub login
│   ├── model/                           ← Database tables (how data looks)
│   │   └── User.java                    ← User information
│   ├── repository/                      ← Talk to database
│   │   └── UserRepository.java          ← Save/retrieve users
│   ├── security/                        ← Keep data safe
│   │   └── JwtTokenProvider.java        ← Create security tokens
│   └── exception/                       ← Error handling
│       └── GlobalExceptionHandler.java  ← Handle mistakes
│
├── src/main/resources/
│   └── application.properties           ← Configuration file (EDIT THIS!)
│
├── pom.xml                              ← List of libraries used
└── Dockerfile                           ← For running in Docker
```

**Don't worry about understanding all of this yet!** Just know:
- **controller/** = What the frontend can ask for
- **service/** = How the backend does work
- **model/** = What a user looks like in the database

---

## 🔧 How It Works (Simple Explanation)

### When a User Registers:
```
1. User enters email, username, password on frontend
2. Frontend sends this to backend
3. Backend checks if email is already used
4. Backend encrypts password (so nobody can see it)
5. Backend saves user to database
6. Frontend gets a security token (like a password for API calls)
7. User is logged in! ✅
```

### When a User Logs In:
```
1. User enters email & password
2. Backend finds user in database
3. Backend checks if password matches
4. If correct → give security token
5. If wrong → send error message
6. Frontend stores token and uses it for future requests
```

### How Security Tokens Work:
- When user logs in, they get a **token**
- This token is like a temporary ID card
- It lasts for 24 hours
- Every request includes this token
- Backend checks the token is valid
- If token expires → user needs to login again

---

## 📝 Quick Configuration

### File: `application.properties`

This file tells the backend where the database is and other settings.

```properties
# Database connection (most important!)
spring.datasource.url=jdbc:mysql://localhost:3306/connecthub
spring.datasource.username=root
spring.datasource.password=your_password

# Token settings (in milliseconds)
jwt.expiration=86400000              # Token lasts 24 hours
jwt.refresh.expiration=604800000     # Refresh token lasts 7 days

# Server port
server.port=8081

# Show SQL queries (helpful for debugging)
spring.jpa.show-sql=true
```

**If you change the password:**
- Find: `spring.datasource.password=your_password`
- Replace `your_password` with your MySQL password

---

## 🌐 API Endpoints (What You Can Ask For)

### User Registration
**What it does:** Create a new user account

```bash
# The request
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe"
}

# The response (if successful)
{
  "message": "User registered successfully",
  "user": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "status": "ONLINE",
    "provider": "EMAIL",
    "isActive": true,
    "createdAt": "2026-04-18T10:00:00"
  }
}
```

### User Login
**What it does:** Check password and give security token

```bash
POST http://localhost:8081/api/auth/login

{
  "email": "john@example.com",
  "password": "password123"
}

# Response (if password correct)
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful",
  "user": { ... }
}

# Response (if password wrong)
{
  "error": "InvalidCredentialsException",
  "message": "Invalid email or password",
  "status": 401
}
```

### Get User Profile
**What it does:** Get information about a user

```bash
GET http://localhost:8081/api/auth/profile
Authorization: Bearer {token}

# Response
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "bio": "Software Developer",
  "avatarUrl": "https://example.com/avatar.jpg",
  "status": "ONLINE"
}
```

### Update Profile
**What it does:** Change user information

```bash
PUT http://localhost:8081/api/auth/profile
Authorization: Bearer {token}

{
  "fullName": "John Doe Updated",
  "bio": "Senior Developer",
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```

### Change Password
**What it does:** Update password (need to know old one)

```bash
POST http://localhost:8081/api/auth/change-password
Authorization: Bearer {token}

{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword456",
  "confirmPassword": "newpassword456"
}
```

### Search Users
**What it does:** Find users by username

```bash
GET http://localhost:8081/api/auth/search?username=john
Authorization: Bearer {token}

# Response (list of matching users)
[
  {
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "status": "ONLINE"
  },
  ...
]
```

### Update Status
**What it does:** Tell backend if you're online, away, busy, or invisible

```bash
POST http://localhost:8081/api/auth/status?status=ONLINE
Authorization: Bearer {token}

# Status options:
# ONLINE - actively using app
# AWAY - idle but app is open  
# DND - Do Not Disturb (online but unavailable)
# INVISIBLE - appears offline
```

---

## ✅ Testing (How to Check If It Works)

### Test 1: Is the Backend Running?

```bash
curl http://localhost:8081/api/auth/health

# You should see:
# {"status":"Auth Service is running","timestamp":"2026-04-18T10:00:00"}
```

### Test 2: Register a New User

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "email":"test@example.com",
    "password":"test123",
    "fullName":"Test User"
  }'
```

**Success looks like:** User data returned with `status: "ONLINE"`

**Error looks like:** 
```json
{"error": "UserAlreadyExistsException", "message": "Email already registered", "status": 409}
```

### Test 3: Login with That User

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"test@example.com",
    "password":"test123"
  }'
```

**Success:** You get a `token` back  
**Error:** "Invalid email or password"

### Test 4: Use the Token

```bash
# Copy the token from login response and use it here:
curl http://localhost:8081/api/auth/profile \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Success:** User profile displayed  
**Error (401):** "Token is invalid or expired"

---

## 🐛 Troubleshooting (Common Problems)

### Problem: "Connection refused" when starting backend

```
Error: Cannot connect to database at localhost:3306
```

**Solution:**
1. Is MySQL running?
   - Try: `mysql -u root -p` 
   - If it fails, start MySQL
2. Is the password correct in `application.properties`?
   - Check: `spring.datasource.password=your_password`

### Problem: "Port 8081 already in use"

```
Error: Port 8081 is already in use
```

**Solution:**
Either:
- Kill the other process using port 8081, OR
- Change the port in `application.properties`:
  ```properties
  server.port=8082
  ```

### Problem: "Build fails" or "mvn not found"

```
Error: 'mvn' is not recognized or ./mvnw: permission denied
```

**Solution:**
- Windows: Use `mvnw.cmd` instead of `mvnw`
- Mac/Linux: Run `chmod +x mvnw` first
- Make sure you're in the `auth-service` folder

### Problem: "401 Unauthorized" when calling API with token

```
Error: Token is invalid or expired
```

**Solution:**
- Make sure token is in Authorization header: `Bearer YOUR_TOKEN`
- Token expires after 24 hours - login again
- Check token wasn't modified

---

## 📖 Learn More

Check these files for more details:
- **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** - Every endpoint explained
- **[IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)** - How it was built

---

## 🚀 What to Do Next

1. ✅ Get backend running (you're doing this now!)
2. ⏳ Start the frontend (see `ConnectHub-Frontend/README.md`)
3. ⏳ Test login/register flow end-to-end
4. ⏳ Start building chat features!

---

## 📚 Technologies Explained

### Spring Boot
Framework that makes it easy to build Java web applications. Think of it as a toolkit that does a lot of work for you.

### Maven
Tool that downloads libraries (like Spring Boot) and builds your project.

### MySQL
Database - where all user information is stored.

### JWT Token
A way to prove you're logged in without sending password every time.

### CORS
Allows frontend (port 4200) to talk to backend (port 8081).

---

## 💡 Tips for Beginners

✅ **Always check the logs** - When something breaks, read the error message carefully

✅ **Use terminal/PowerShell** - Easier to run commands than GUI

✅ **Save after editing** - When you change `application.properties`, restart backend

✅ **Take breaks** - Programming is hard, take a break if frustrated

✅ **Google the error** - Most errors have been solved before

✅ **Ask for help** - Don't spend hours stuck, ask teammates!

---

## ❓ FAQ

**Q: What's the difference between `mvnw` and `mvn`?**
A: `mvnw` is like an auto-installer. `mvn` must be installed. Use `mvnw` for easier setup.

**Q: Can I use a different database instead of MySQL?**
A: Yes! But you'd need to change `application.properties`. MySQL is recommended for this project.

**Q: Do I need to understand all the Java code?**
A: No! Focus on the endpoints (in `controller/`) and configuration (`application.properties`).

**Q: How long should startup take?**
A: Usually 5-10 seconds. If more, check your computer's resources.

**Q: Can I run backend on a different port?**
A: Yes! Change `server.port=8081` to any number like 8082 in `application.properties`.

---

## 📞 Getting Help

1. Check this README
2. Check `API_DOCUMENTATION.md`
3. Read error messages carefully
4. Google the error message
5. Ask your team

---

**Last Updated:** April 18, 2026  
**Status:** Ready for Development! 🎉

Everything is set up and ready to go. Now start the frontend and test it out!

---

## License

Internal Use Only - ConnectHub Platform 2026

