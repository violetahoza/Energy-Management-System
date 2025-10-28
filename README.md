# Energy Management System - Assignment 1

## Project Overview

The Energy Management System is a microservices-based application that allows authenticated users to access, monitor, and manage smart energy metering devices. The system implements role-based access control with two user types: Administrators (full CRUD operations) and Clients (view assigned devices).

## Architecture

### Components
- **Frontend**: React-based application
- **Authorization Service**: Handles authentication, JWT token generation and validation
- **User Management Service**: CRUD operations for user accounts
- **Device Management Service**: CRUD operations for devices and device-user associations
- **Traefik**: Reverse proxy and API Gateway with ForwardAuth middleware
- **MySQL Databases**: Three separate databases for credentials, users, and devices

### Technology Stack
- **Backend**: Java 21, Spring Boot 3.x, Spring Security, JPA/Hibernate
- **Frontend**: React 18, React Router, Axios
- **Reverse Proxy**: Traefik v3.2
- **Database**: MySQL 8
- **Containerization**: Docker & Docker Compose
- **Authentication**: JWT (JSON Web Tokens)

## Prerequisites

Before running the application, ensure you have the following installed:

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Git**: For cloning the repository

Verify installations:
```bash
docker --version
docker-compose --version
git --version
```

## Configuration

### Authorization Service Environment Variables

Create a `.env` file in `backend/authorization-service/` directory with the following content:

```env
# JWT Configuration
JWT_SECRET=your-secret-key-here-minimum-32-characters-long
JWT_EXPIRATION=86400000
```

**Important**: Replace `your-secret-key-here-minimum-32-characters-long` with a secure random string of at least 32 characters.

## Build and Execution

### Build and Run with Docker Compose

1. **Clone the repository**:
```bash
git clone <repository-url>
cd <repository-folder>
```

2. **Create the authorization service .env file** (as described in Configuration section)

3. **Build and start all services**:
```bash
docker-compose up --build
```

This command will:
- Build Docker images for all services
- Start MySQL databases with health checks
- Start the backend microservices
- Start the frontend application
- Configure Traefik reverse proxy

4. **Wait for all services to be healthy** 


### Rebuild Specific Services

If you make changes to a specific service:

```bash
# Rebuild and restart authorization service
docker-compose up --build -d authorization-service

# Rebuild and restart user service
docker-compose up --build -d user-service

# Rebuild and restart device service
docker-compose up --build -d device-service

# Rebuild and restart frontend
docker-compose up --build -d frontend
```


## API Documentation (Swagger)

Each microservice provides API documentation via Swagger UI. Once all services are running, you can access the Swagger UI for each service:

- **Authorization Service**: http://localhost:8083/swagger-ui/index.html
- **User Service**: http://localhost:8081/swagger-ui/index.html
- **Device Service**: http://localhost:8082/swagger-ui/index.html

## Accessing the Application

Once all services are running:

- **Frontend Application**: http://localhost
- **Traefik Dashboard**: http://localhost:8080
- **API Endpoints**: http://localhost/api/...

### Default Routes

- `http://localhost/login` - Login page
- `http://localhost/register` - Registration page
- `http://localhost/admin` - Admin dashboard (requires ADMIN role)
- `http://localhost/client` - Client dashboard (requires CLIENT role)

## API Endpoints

### Public Endpoints (No Authentication Required)

```
POST   http://localhost/api/auth/register       - Register new user
POST   http://localhost/api/auth/login          - Login and get JWT token
```

### Protected Endpoints (Authentication Required)

#### Authorization Service
```
POST   http://localhost/api/auth/logout         - Logout (invalidate token)
GET    http://localhost/api/auth/user           - Get current user info
```

#### User Service
```
GET    http://localhost/api/users                - List all users (ADMIN only)
POST   http://localhost/api/users                - Create user (ADMIN only)
GET    http://localhost/api/users/{id}           - Get user by ID
PATCH  http://localhost/api/users/{id}           - Update user (ADMIN only)
DELETE http://localhost/api/users/{id}           - Delete user (ADMIN only)
```

#### Device Service
```
GET    http://localhost/api/devices               - List devices (ADMIN: all, CLIENT: assigned only)
POST   http://localhost/api/devices               - Create device (ADMIN only)
GET    http://localhost/api/devices/{id}          - Get device by ID
PATCH  http://localhost/api/devices/{id}          - Update device (ADMIN only)
DELETE http://localhost/api/devices/{id}          - Delete device (ADMIN only)
PATCH  http://localhost/api/devices/{id}/assign   - Assign device to user (ADMIN only)
```

## Authentication Flow

1. **Register/Login**: User registers or logs in via frontend
2. **Token Generation**: Authorization service generates JWT token
3. **Token Storage**: Frontend stores token in localStorage
4. **Authenticated Requests**: Frontend includes token in Authorization header
5. **Token Validation**: Traefik intercepts requests and validates token via ForwardAuth
6. **User Headers**: Valid tokens result in user info headers (X-User-Id, X-Username, X-User-Role)
7. **Service Authorization**: Backend services read headers and enforce permissions


## Stopping the Application

### Stop all services (containers remain):
```bash
docker-compose stop
```

### Stop and remove all containers:
```bash
docker-compose down
```

### Stop, remove containers, and delete volumes (database data):
```bash
docker-compose down -v
```

## Testing the Application

### 1. Register an Admin User

```bash
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "email": "admin@example.com",
    "fullName": "Admin User",
    "address": "123 Admin Street",
    "role": "ADMIN"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "username": "admin",
  "role": "ADMIN",
  "message": "Registration successful"
}
```

### 2. Register a Client User

```bash
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "client1",
    "password": "client123",
    "email": "client1@example.com",
    "fullName": "John Doe",
    "address": "456 Client Avenue",
    "role": "CLIENT"
  }'
```

### 3. Login and Get Token

```bash
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Save the token from the response for subsequent requests.**

### 4. Get Current User Info

```bash
curl -X GET http://localhost/api/auth/user \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Create a User (Admin Only)

```bash
curl -X POST http://localhost/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "client123",
    "email": "newuser@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "address": "456 Client Avenue",
    "role": "CLIENT"
  }'
```

### 6. List All Users (Admin Only)

```bash
curl -X GET http://localhost/api/users \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Get Specific User

```bash
# Replace {id} with actual user ID
curl -X GET http://localhost/api/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 8. Update User (Admin Only)

```bash
curl -X PATCH http://localhost/api/users/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name"
  }'
```

### 9. Create a Device (Admin Only)

```bash
curl -X POST http://localhost/api/devices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smart Meter 001",
    "description": "Living room energy meter",
    "address": "123 Main Street",
    "maxConsumption": 1000.0
  }'
```

### 10. List All Devices

```bash
# Admin sees all devices
# Client sees only assigned devices
curl -X GET http://localhost/api/devices \
  -H "Authorization: Bearer $TOKEN"
```

### 11. Get Specific Device

```bash
curl -X GET http://localhost/api/devices/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 12. Update Device (Admin Only)

```bash
curl -X PATCH http://localhost/api/devices/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smart Meter 001 - Updated",
    "maxConsumption": 1500.0
  }'
```

### 13. Assign Device to User (Admin Only)

```bash
# Assign device ID 1 to user ID 2
curl -X PATCH http://localhost/api/devices/1/assign \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2
  }'
```

### 14. Delete Device (Admin Only)

```bash
curl -X DELETE http://localhost/api/devices/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 15. Delete User (Admin Only)

```bash
curl -X DELETE http://localhost/api/users/2 \
  -H "Authorization: Bearer $TOKEN"
```

### 16. Logout

```bash
curl -X POST http://localhost/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

## Authors

- Violeta-Maria Hoza

## License

This project is developed for academic purposes as part of the Distributed Systems course at Technical University of Cluj-Napoca.