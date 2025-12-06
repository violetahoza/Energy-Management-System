# Energy Management System

## Project Overview

The Energy Management System is a microservices-based application that allows authenticated users to access, monitor, and manage smart energy metering devices with consumption tracking. The system implements role-based access control with two user types: Administrators (full CRUD operations) and Clients (view assigned devices and consumption data).

The system features a hybrid **Chat Support System** (Rule-based + AI + Admin Support) and **Real-time Overconsumption Alerts**.

## Architecture

### System Components

#### Backend Microservices
- **Authorization Service** (Port 8083): JWT authentication, token validation, and ForwardAuth endpoint for Traefik
- **User Service** (Port 8081): User profile management and CRUD operations
- **Device Service** (Port 8082): Device management and user-device assignment
- **Monitoring Service** (Port 8084): Energy consumption data aggregation and historical analysis
- **Customer Support Service** (Port 8085): Manages real-time WebSockets for chat and alerts, integrates with Google Gemini AI, and handles rule-based responses.

#### Infrastructure
- **Traefik v3.2**: Reverse proxy and API Gateway with ForwardAuth middleware for centralized authentication
- **RabbitMQ Brokers** (2 instances):
    - **Synchronization Broker** (Ports 5672/15672): Handles user and device synchronization events
    - **Data Collection Broker** (Ports 5673/15673): Processes real-time device measurement data
- **MySQL Databases** (4 instances): Separate databases for each service (credentials_db, users_db, devices_db, monitoring_db)

#### Additional Components
- **Frontend**: React 18-based single-page application with authentication context and role-based routing
- **Device Data Simulator**: Python application generating realistic energy consumption data every 10 minutes

### Technology Stack

**Backend**
- Java 21, Spring Boot 3.x
- Spring Security (method-level authorization with @PreAuthorize)
- Spring Data JPA with Hibernate 
- Spring WebFlux (for AI API integration)
- Spring WebSocket (STOMP over SockJS)
- RabbitMQ (AMQP messaging)
- JWT (JSON Web Tokens)
- Swagger/OpenAPI documentation

**Frontend**
- React 18, React Router v6
- Axios for API communication
- **SockJS & StompJS** for real-time communication
- Context API for authentication and WebSocket state

**AI & Automation**
- **Google Gemini API**: Generative AI for fallback customer support responses.
- **Rule-Based Engine**: Regex pattern matching for instant common query responses.

**Infrastructure**
- Traefik v3.2 (API Gateway)
- MySQL 8 (persistent data storage)
- RabbitMQ 3.13 (message broker)
- Docker & Docker Compose (containerization)

**Data Simulation**
- Python 3.9
- Pika (RabbitMQ client library)
- Pytz (timezone handling)

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
JWT_EXPIRATION=3600000 # 1 hour in milliseconds
```

**Important**: Replace `your-secret-key-here-minimum-32-characters-long` with a secure random string of at least 32 characters.

### Customer Support Service Environment Variables

To enable AI responses, create a `.env` file in `backend/customer-support/` directory with the following content:

```env
GEMINI_API_KEY=your_google_gemini_api_key
```

**Note**: If the API key is not provided, the AI service will return a default fallback message.

## Build and Execution

### Build and Run with Docker Compose

1. **Clone the repository**:
```bash
git clone <repository-url>
cd <repository-folder>
```

2. **Create the authorization service .env file** (as described in the Configuration section)

3. **Build and start all services**:
```bash
docker-compose up --build
```

This command will:
- Build Docker images for all microservices
- Start 4 MySQL databases with health checks
- Start 2 RabbitMQ brokers (synchronization and data collection)
- Launch all backend microservices
- Start the React frontend
- Configure Traefik reverse proxy with routing rules

4. **Wait for all services to be healthy** 

## Accessing the Application

Once all services are running:

- **Frontend Application**: http://localhost
- **Traefik Dashboard**: http://localhost:8080
- **API Endpoints**: http://localhost/api/...
- **Synchronization Broker Management**: http://localhost:15672 (credentials: `rabbitmq_user/rabbitmq_pass`)
- **Data Collection Broker Management**: http://localhost:15673 (credentials: `rabbitmq_user/rabbitmq_pass`)

### Default Routes

- `http://localhost/login` - Login page
- `http://localhost/admin` - Admin dashboard (requires ADMIN role)
- `http://localhost/client` - Client dashboard (requires CLIENT role)

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

### Rebuild Specific Services

If you make changes to a specific service:

```bash
# Rebuild and restart authorization service
docker-compose up --build -d authorization-service

# Rebuild and restart user service
docker-compose up --build -d user-service

# Rebuild and restart device service
docker-compose up --build -d device-service

# Rebuild and restart monitoring service
docker-compose up --build -d monitoring-service

# Rebuild and restart frontend
docker-compose up --build -d frontend
```

## Authentication Flow

1. **Register/Login**: User registers or logs in via frontend
2. **Token Generation**: Authorization service generates JWT token
3. **Token Storage**: Frontend stores token in localStorage
4. **Authenticated Requests**: Frontend includes token in Authorization header
5. **Token Validation**: Traefik intercepts requests and validates token via ForwardAuth
6. **User Headers**: Valid tokens result in user info headers (X-User-Id, X-Username, X-User-Role)
7. **Service Authorization**: Backend services read headers and enforce permissions

## Testing the Application

### Login and Get Token

```bash
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Save the token from the response for subsequent requests.**


### Create a User (Admin Only)

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

### List All Users (Admin Only)

```bash
curl -X GET http://localhost/api/users \
  -H "Authorization: Bearer $TOKEN"
```

### Get Specific User

```bash
# Replace {id} with actual user ID
curl -X GET http://localhost/api/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Update User (Admin Only)

```bash
curl -X PATCH http://localhost/api/users/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name"
  }'
```

### Create a Device (Admin Only)

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

### List All Devices

```bash
# Admin sees all devices
# Client sees only assigned devices
curl -X GET http://localhost/api/devices \
  -H "Authorization: Bearer $TOKEN"
```

### Get Specific Device

```bash
curl -X GET http://localhost/api/devices/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Update Device (Admin Only)

```bash
curl -X PATCH http://localhost/api/devices/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smart Meter 001 - Updated",
    "maxConsumption": 1500.0
  }'
```

### Assign Device to User (Admin Only)

```bash
# Assign device ID 1 to user ID 2
curl -X PATCH http://localhost/api/devices/1/assign \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2
  }'
```

### Delete Device (Admin Only)

```bash
curl -X DELETE http://localhost/api/devices/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Delete User (Admin Only)

```bash
curl -X DELETE http://localhost/api/users/2 \
  -H "Authorization: Bearer $TOKEN"
```

### Logout

```bash
curl -X POST http://localhost/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

## API Documentation (Swagger)

Each microservice provides API documentation via Swagger UI. Once all services are running, you can access the Swagger UI for each service:

- **Authorization Service**: http://localhost:8083/swagger-ui/index.html
- **User Service**: http://localhost:8081/swagger-ui/index.html
- **Device Service**: http://localhost:8082/swagger-ui/index.html
- **Monitoring Service**: http://localhost:8084/swagger-ui/index.html

## Authors

- Violeta-Maria Hoza

## License

This project is developed for academic purposes as part of the Distributed Systems course at Technical University of Cluj-Napoca.