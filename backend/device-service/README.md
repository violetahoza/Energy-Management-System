# Device Service

The **Device Service** is responsible for managing smart energy devices, their metadata, and their assignment to users. It integrates with the User Service to validate user existence and roles, and communicates with the Monitoring Service via RabbitMQ to synchronize device assignments. It maintains a local replica of essential user data (`sync_users`) for quick role checks.

### ⚙️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.x, Spring Security
* **Database**: MySQL (`devices_db`)
* **Messaging**: RabbitMQ (for device and user synchronization events)
* **Documentation**: Swagger/OpenAPI

### 🚀 Architecture & Flow

1.  **User Synchronization**: Consumes `UserSyncEvent` messages on the `user.sync.queue.device` queue to maintain a local table (`sync_users`) for fast user/role lookups (e.g., validating if a user being assigned a device exists and is a `CLIENT`).
2.  **Device Synchronization**: Publishes `DeviceSyncEvent` messages to the `device.sync.exchange` whenever a device is created, deleted, or its user assignment is changed.
3.  **Authorization**: Enforces authorization rules, including an `@deviceSecurityService.isDeviceOwnedByUser` check to ensure clients only access their assigned devices.

### 🔑 API Endpoints

All endpoints are protected by Traefik's ForwardAuth middleware and enforce role-based access.

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/devices/` | List all devices. | **ADMIN only** |
| `GET` | `/api/devices/{deviceId}` | Get device by ID. | ADMIN or Device Owner |
| `GET` | `/api/devices/user/{userId}` | Get all devices assigned to a specific user. | ADMIN or CLIENT (self) |
| `POST` | `/api/devices/` | Create a new device (publishes `CREATED` event). | **ADMIN only** |
| `PATCH`| `/api/devices/{deviceId}`| Update device details. | **ADMIN only** |
| `PATCH`| `/api/devices/{deviceId}/assign/{userId}` | Assign device to a user (publishes `UPDATED` event). | **ADMIN only** |
| `PATCH`| `/api/devices/{deviceId}/unassign` | Remove user assignment (publishes `UPDATED` event). | **ADMIN only** |
| `DELETE`| `/api/devices/{deviceId}`| Delete a device (publishes `DELETED` event). | **ADMIN only** |

- **API Documentation (Swagger)**: http://localhost:8081/swagger-ui/index.html
