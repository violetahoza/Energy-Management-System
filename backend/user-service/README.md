# User Service

The **User Service** is the source for user profile data (first name, email, address, role). It handles CRUD operations and publishes synchronization events to RabbitMQ whenever a user is created, updated, or deleted, ensuring other services maintain a consistent view of the user base.

### ⚙️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.x, Spring Security
* **Database**: MySQL (`users_db`)
* **Messaging**: RabbitMQ (for user synchronization events)
* **Documentation**: Swagger/OpenAPI

### Architecture & Flow

1.  **CRUD Operations**: Provides the core interface for managing user data.
2.  **Authorization**: Access control is enforced using Spring Security's `@PreAuthorize` based on headers injected by the Authorization Service (via Traefik).
3.  **Event Publishing**: Publishes `UserSyncEvent` messages to the `user.sync.exchange` for other services to consume (e.g., Authorization and Device Services).
    * Events include: `CREATED`, `UPDATED`, `DELETED`.

### 🔑 API Endpoints

| Method | Endpoint              | Description                                                                       | Access |
| :--- |:----------------------|:----------------------------------------------------------------------------------| :--- |
| `GET` | `/api/users/`         | List all users.                                                                   | **ADMIN only** |
| `GET` | `/api/users/{userId}` | Get user by ID.                                                                   | ADMIN or CLIENT (self) |
| `POST` | `/api/users/`         | Create a new user (publishes `CREATED` event).                                    | **ADMIN only** |
| `PATCH` | `/api/users/{userId}` | Update user details (triggers a sync event for credential/role/username changes). | ADMIN or CLIENT (self) |
| `DELETE` | `/api/users/{userId}`           | Delete a user (publishes `DELETED` event).                                        | **ADMIN only** |

- **API Documentation (Swagger)**: http://localhost:8081/swagger-ui/index.html
