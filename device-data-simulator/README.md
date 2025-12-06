# Device Data Simulator

The **Device Data Simulator** is a Python-based application that generates realistic energy consumption data for smart metering devices. It simulates sensor readings at configurable intervals (default 10 minutes), applying time-of-day patterns to create realistic consumption profiles. Data is published to RabbitMQ for distributed processing across multiple monitoring service replicas.

### ⚙️ Technology Stack

* **Language**: Python 3.9
* **Messaging**: RabbitMQ (pika library)
* **Data Format**: CSV, JSON
* **Containerization**: Docker
* **Timezone Handling**: pytz library

### 📁 Project Structure

```
Device Simulator
├── simulator.py         # Main application logic
├── config.py            # Configuration management
├── requirements.txt     # Python dependencies
├── Dockerfile           # Container definition
└── sensor_data/         # CSV output directory (created at runtime)
```
### 🚀 Architecture & Flow

1. **Multi-Device Simulation**: Runs concurrent threads for each configured device ID, allowing independent simulation of multiple devices simultaneously.
2. **Realistic Data Generation**: Applies time-of-day multipliers to base consumption rates:
    - Night hours (00:00-06:00): 0.5x base rate (reduced consumption)
    - Morning/Evening (06:00-09:00, 18:00-22:00): 1.5x base rate (peak usage)
    - Daytime (09:00-18:00): 1.0x base rate (normal consumption)
    - Late night (22:00-00:00): 0.8x base rate (winding down)
3. **Data Publishing**: Sends measurements to RabbitMQ's `device.data.exchange` with routing key `device.data`, consumed by the **Load Balancing Service**.
4. **Local Storage**: Saves all measurements to CSV files (`sensor_data/<device_id>.csv`) for backup and analysis.

### 📊 Data Format

**Published Message (JSON)**:
```json
{
  "timestamp": "2025-11-22T14:32:26",
  "device_id": 1,
  "measured_value": 0.5545
}
```

### Energy Consumption Patterns

The simulator generates realistic consumption based on time of day:

| Time Period | Hours      | Load Factor | Typical Usage         |
|-------------|------------|-------------|-----------------------|
| Night       | 00:00-06:00| 0.5 - 0.7   | Minimal (sleep)       |
| Morning     | 06:00-09:00| 0.8 - 1.1   | Moderate (wake up)    |
| Day         | 09:00-17:00| 0.7 - 0.9   | Low-Moderate (work)   |
| Evening     | 17:00-24:00| 1.0 - 1.4   | High (peak usage)     |

Each device has a unique `base_load` (0.3-0.8 kWh) with random fluctuations (±0.05 kWh).

### 🔧 Configuration

**Environment Variables** (configured in `config.py`):

| Variable          | Description                          | Default               |
|-------------------|--------------------------------------|-----------------------|
| `RABBITMQ_HOST`   | RabbitMQ server hostname            | `localhost`           |
| `RABBITMQ_PORT`   | RabbitMQ server port                | `5673`                |
| `RABBITMQ_USER`   | RabbitMQ username                   | `rabbitmq_user`       |
| `RABBITMQ_PASS`   | RabbitMQ password                   | `rabbitmq_pass`       |
| `DEVICE_IDS`      | Comma-separated device IDs          | `1,2,6,7,8,9,10,11`   |
| `DATA_FOLDER_PATH`| CSV output directory                | `sensor_data`         |

**RabbitMQ Configuration**:

- **Exchange**: `device.data.exchange` (topic, durable)
- **Queue**: `device.data.queue` (durable)
- **Routing Key**: `device.data`
- **Message Format**: JSON with persistent delivery
- **Broker**: Data Collection Broker (port 5673)

### 🚀 Running the Simulator

**Local Development** (Python environment):
```bash
# Navigate to simulator directory
cd device-data-simulator

# Install dependencies
pip install -r requirements.txt

# Run with default 10-minute interval
python simulator.py

# Run with custom interval (e.g., 1 minute for testing)
python simulator.py 1
```
**Expected Output**:
```
====================================================
Starting Device Simulator
Timezone: Europe/Bucharest
Devices: ['1', '2', '6', '7', '8', '9', '10', '11']
Target: localhost:5673
Saving CSVs to: sensor_data/sensor_data_<id>.csv
====================================================
✓ [Device 1] Connected to RabbitMQ
✓ [Device 2] Connected to RabbitMQ
✓ [Device 1] Sent 0.7234 kWh at 2024-12-06T14:30:00
✓ [Device 2] Sent 0.8156 kWh at 2024-12-06T14:30:00
```

### 🔗 Integration with Load Balancing Pipeline

The Device Data Simulator is the entry point of the data processing pipeline:

```
Device Simulator
    ↓ (publishes to device.data.queue)
Load Balancing Service
    ↓ (distributes based on strategy)
    ├─→ ingest.queue.1 → Monitoring Service Replica 1
    ├─→ ingest.queue.2 → Monitoring Service Replica 2
    └─→ ingest.queue.3 → Monitoring Service Replica 3
```

**Data Flow**:

1. **Simulator**: Publishes raw measurements every N minutes to `device.data.exchange`
2. **Load Balancing Service**: Consumes from `device.data.queue` and routes to replica-specific ingest queues
3. **Monitoring Replicas**: Each replica processes messages from its dedicated queue
4. **Aggregation**: Hourly consumption records stored in `monitoring_db`
5. **API Access**: Historical data accessible via `/api/monitoring/devices/{deviceId}/consumption/daily`
