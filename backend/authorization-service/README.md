# Authorization Service

The **Authorization Service** is responsible for user authentication, JSON Web Token (JWT) management, and acting as the central authority for request validation via Traefik's ForwardAuth middleware. It is synchronized with the User Service via RabbitMQ events to manage user credentials.

### ⚙️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.x, Spring Security
* **Database**: MySQL (`credentials_db`)
* **Messaging**: RabbitMQ (consumer for user synchronization)
* **Security**: JWT (JSON Web Tokens), BCrypt Password Encoding
* **Documentation**: Swagger/OpenAPI

### 🚀 Architecture & Flow

1.  **Authentication**: Handles user login and generates a signed JWT token containing `userId`, `username`, and `role`.
2.  **Token Validation**: Exposes a `/api/auth/validate` endpoint used by the Traefik proxy. If the token is valid, it passes user identity (`X-User-Id`, `X-Username`, `X-User-Role`) via HTTP headers to downstream services.
3.  **Synchronization**: Consumes `UserSyncEvent` messages from the `user.sync.queue.auth` queue to keep the local `credentials_db` database in sync with the User Service's user list (handles `CREATED`, `UPDATED`, `DELETED` events).

### 🔑 API Endpoints

The service exposes endpoints for user authentication and token management, including the critical /validate endpoint used by the Traefik API Gateway.

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- |:-------|
| `POST` | `/api/auth/login` | Authenticates a user and returns a JWT token. | Public |
| `POST` | `/api/auth/logout` | Invalidates the provided JWT token by adding it to a blacklist. | Protected |
| `GET` | `/api/auth/user` | Retrieves current user information from the JWT token. | Protected |
| `GET` | `/api/auth/validate` | Used internally by the API Gateway to validate a JWT token and pass user headers. | Public |

- **API Documentation (Swagger)**: http://localhost:8083/swagger-ui/index.html
