# Device Data Simulator

A Python simulator that generates smart meter readings and sends them to RabbitMQ for the Energy Management System.

## Features

- ✅ Generates measurements every 10 minutes (configurable)
- ✅ Realistic energy consumption patterns (night/day/evening variations)
- ✅ Random base load with small fluctuations
- ✅ Sends JSON messages to RabbitMQ
- ✅ Command-line interface

## Project Structure

```
device-data-simulator/
├── simulator.py         # Main simulator script
├── config.py            # Configuration (RabbitMQ settings)
└── requirements.txt     # Python dependencies
```

## Configuration

Install Dependencies

```bash
cd device-data-simulator
pip install -r requirements.txt
```

## Usage

```bash
python simulator.py <device_id> [interval_minutes]
```

### Examples

```bash
# Device 1 with 10-minute intervals 
python simulator.py 1

# Device 1 with 1-minute intervals 
python simulator.py 1 1

# Device 2 with 5-minute intervals
python simulator.py 2 5
```

### Output

```
============================================================
Device Data Simulator Started
============================================================
Device ID:       1
Base Load:       0.52 kWh per 1 min
Interval:        1 minutes
Press Ctrl+C to stop
============================================================
✓ Connected to RabbitMQ at localhost:5672
✓ [  1] 14:30:00 | Device 1 | 0.4234 kWh
✓ [  2] 14:31:00 | Device 1 | 0.4567 kWh
✓ [  3] 14:32:00 | Device 1 | 0.5123 kWh
```

### Stop

Press `Ctrl+C` to stop gracefully.

## Multiple Devices

Run multiple simulators in separate terminals:

**Terminal 1:**
```bash
python simulator.py 1 1
```

**Terminal 2:**
```bash
python simulator.py 2 1
```

**Terminal 3:**
```bash
python simulator.py 3 1
```

## Message Format

The simulator sends JSON messages in this format:

```json
{
  "timestamp": "2024-11-17T14:30:00",
  "device_id": 1,
  "measurement_value": 0.5234
}
```

## Energy Patterns

The simulator creates realistic daily patterns:

| Time Period | Consumption | Description |
|-------------|-------------|-------------|
| **Night** (0-6) | 50-70% of base | Lower consumption |
| **Morning** (6-9) | 80-110% of base | Morning activities |
| **Day** (9-17) | 70-90% of base | Steady consumption |
| **Evening** (17-23) | 100-140% of base | Peak consumption |

- Base load is randomly generated (0.3-0.8 kWh per 10 minutes)
- Small random fluctuations added to each measurement
- All values are non-negative

## Integration with the system

### 1. Start the services

```bash
docker-compose up
```

### 2. Create a device

Use the API or frontend to create a device (as ADMIN).

### 3. Run Simulator

```bash
cd device-data-simulator
python simulator.py 1 1
```

### 4. Verify in RabbitMQ

- Open: http://localhost:15672
- Login: `rabbitmq_user` / `rabbitmq_pass`
- Go to **Queues** → `device.data.queue`
- You should see messages arriving

### 5. Check Monitoring Service

```bash
docker-compose logs -f monitoring-service | grep "Received device data"
```

### 6. Query API

After 6+ measurements (for 1 hour of data):

```bash
curl "http://localhost/api/monitoring/devices/1/consumption/daily?date=2024-11-17" \
  -H "Authorization: Bearer YOUR_TOKEN"
```



## Testing Workflow

```bash
# 1. Start services
docker-compose up

# 2. Create device (via API/frontend)
# Device ID: 1

# 3. Run simulator (testing mode - 1 min intervals)
cd device-data-simulator
python simulator.py 1 1

# 4. Let it run for 6+ minutes

# 5. Query API
curl "http://localhost/api/monitoring/devices/1/consumption/daily?date=$(date +%Y-%m-%d)" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Code Structure

### Class: `DeviceSimulator`

**Methods:**
- `__init__(device_id)` - Initialize simulator
- `connect()` - Connect to RabbitMQ
- `generate_measurement()` - Create realistic measurement
- `send_measurement(value)` - Send to RabbitMQ
- `run(interval)` - Main simulation loop

### Flow

```
1. Initialize with device ID
2. Load configuration from config.py
3. Generate random base load
4. Connect to RabbitMQ
5. Loop:
   - Generate measurement (time-based)
   - Send to RabbitMQ
   - Wait for interval
   - Repeat
```

