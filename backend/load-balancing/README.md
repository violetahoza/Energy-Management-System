# Load Balancing Service

The **Load Balancing Service** distributes incoming device measurement data across multiple Monitoring Service replicas to enable horizontal scaling and parallel processing. It consumes device readings from a central queue and intelligently routes each message to one of several monitoring instances based on a configurable load balancing strategy.

### ⚙️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.x, Spring AMQP
* **Messaging**: RabbitMQ (data collection broker)
* **Strategies**: Consistent Hashing, Round Robin (Batched)

### 🚀 Architecture & Flow

1.  **Message Consumption**: Consumes `DeviceDataMessage` events from the `device.data.queue` on the data collection broker. This queue receives all raw device measurements from the simulator.
2.  **Strategy Selection**: Applies a configurable load balancing strategy to determine which monitoring replica should process each message:
    *   **Round Robin (Default)**: Distributes devices in batches. First N devices go to replica 1, next N to replica 2, cycling through all replicas.
    *   **Consistent Hashing**: Uses MD5 hashing on device IDs to ensure the same device always routes to the same replica, providing data locality.
3.  **Message Routing**: Publishes the message to a replica-specific ingest queue (`ingest.queue.{1,2,3}`) via the `ingest.exchange`. Each monitoring replica consumes from its dedicated queue.
4.  **Queue Management**: Automatically creates and binds ingest queues based on the configured number of replicas (`MONITORING_REPLICAS`).

### 📊 Load Balancing Strategies

**Round Robin (Batched)**
```
replica = ((deviceId - 1) / devicesPerReplica) % totalReplicas + 1
```
*   Devices 1-3 → Replica 1
*   Devices 4-6 → Replica 2
*   Devices 7-9 → Replica 3

**Consistent Hashing**
```
hash = MD5(deviceId)
replica = (hash % totalReplicas) + 1
```
*   Same device always routes to same replica
*   Better for cache locality and device-specific processing

### ⚙️ Configuration

| Environment Variable | Description | Default |
| :--- | :--- | :--- |
| `MONITORING_REPLICAS` | Number of monitoring service instances | `3` |
| `LOAD_BALANCING_STRATEGY` | Strategy to use (`round-robin` or `consistent-hashing`) | `round-robin` |
| `DEVICES_PER_REPLICA` | Batch size for round-robin distribution | `3` |

### 📡 RabbitMQ Queues

**Input Queue**
*   **Queue**: `device.data.queue`
*   **Exchange**: `device.data.exchange` (topic)
*   **Routing Key**: `device.data`

**Output Queues** (dynamically created)
*   **Queues**: `ingest.queue.1`, `ingest.queue.2`, `ingest.queue.3`
*   **Exchange**: `ingest.exchange` (topic)
*   **Routing Keys**: `ingest.data.1`, `ingest.data.2`, `ingest.data.3`


### 🔄 Scaling

To change the number of monitoring replicas:

1.  Update `MONITORING_REPLICAS` in `compose.yaml`
2.  Add/remove monitoring service instances
3.  Rebuild and restart:
```bash
docker-compose up --build -d
```

The service automatically creates additional ingest queues and adjusts routing accordingly.