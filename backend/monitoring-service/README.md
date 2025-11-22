# Monitoring Service

The **Monitoring Service** aggregates real-time device readings and stores them as hourly consumption summaries. It consumes raw data from a dedicated data broker and device synchronization events from the main synchronization broker. It provides endpoints for retrieving historical consumption data.

### ⚙️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.x, Spring Security
* **Database**: MySQL (`monitoring_db`)
* **Messaging**: RabbitMQ (two separate brokers for sync and data streams)
* **Documentation**: Swagger/OpenAPI

### 🚀 Architecture & Flow

1.  **Device Synchronization**: Consumes `DeviceSyncEvent` messages on the `device.sync.queue.monitoring` to maintain a list of `monitored_devices` and their assigned users. This is crucial for access control to consumption data.
2.  **Data Aggregation**: Consumes `DeviceDataMessage` events from the `device.data.queue` (connected to a separate broker). It aggregates the raw 1-minute/10-minute measurements into hourly consumption records (`measurements` table).
3.  **Authorization**: Enforces access control using an `@deviceSecurityService.isDeviceOwnedByUser` check, allowing clients to only view data for devices assigned to them.

### 🔑 API Endpoints

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/monitoring/devices/{deviceId}/consumption/daily` | Retrieves aggregated hourly consumption data for a device on a specified date. | ADMIN or Device Owner |

- **API Documentation (Swagger)**: http://localhost:8081/swagger-ui/index.html
