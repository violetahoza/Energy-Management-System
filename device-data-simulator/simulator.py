import json
import time
import random
import sys
import csv
import threading
import os
from datetime import datetime
import pika
import pytz
from config import Config

# Lock for thread-safe file operations
file_lock = threading.Lock()

class DeviceSimulator:
    """Simulates smart meter energy consumption readings"""

    def __init__(self, device_id):
        self.device_id = str(device_id).strip()
        self.config = Config()
        self.base_load = random.uniform(0.3, 0.8)
        self.connection = None
        self.channel = None
        self.tz = pytz.timezone(self.config.TIMEZONE)

    def get_current_time_str(self):
        now = datetime.now(pytz.utc).astimezone(self.tz)
        return now.strftime("%Y-%m-%dT%H:%M:%S")

    def get_current_hour(self):
        now = datetime.now(pytz.utc).astimezone(self.tz)
        return now.hour

    def connect(self):
        """Establish RabbitMQ connection"""
        while True:
            try:
                credentials = pika.PlainCredentials(
                    self.config.RABBITMQ_USER,
                    self.config.RABBITMQ_PASS
                )
                parameters = pika.ConnectionParameters(
                    host=self.config.RABBITMQ_HOST,
                    port=self.config.RABBITMQ_PORT,
                    credentials=credentials,
                    heartbeat=600
                )

                self.connection = pika.BlockingConnection(parameters)
                self.channel = self.connection.channel()

                self.channel.exchange_declare(
                    exchange=self.config.EXCHANGE,
                    exchange_type='topic',
                    durable=True
                )

                self.channel.queue_declare(
                    queue='device.data.queue',
                    durable=True
                )

                self.channel.queue_bind(
                    queue='device.data.queue',
                    exchange=self.config.EXCHANGE,
                    routing_key=self.config.ROUTING_KEY
                )

                print(f"✓ [Device {self.device_id}] Connected to RabbitMQ")
                return True

            except Exception as e:
                print(f"✗ [Device {self.device_id}] Connection failed. Retrying in 5s...")
                time.sleep(5)

    def generate_measurement(self):
        """Generate realistic energy consumption"""
        hour = self.get_current_hour()

        if 0 <= hour < 6: factor = random.uniform(0.5, 0.7)
        elif 6 <= hour < 9: factor = random.uniform(0.8, 1.1)
        elif 9 <= hour < 17: factor = random.uniform(0.7, 0.9)
        else: factor = random.uniform(1.0, 1.4)

        fluctuation = random.uniform(-0.05, 0.05)
        measurement = self.base_load * factor + fluctuation
        return max(0, round(measurement, 4))

    def log_to_csv(self, timestamp, measurement):
        """Save measurement to a device-specific CSV file"""
        filename = f"sensor_data_{self.device_id}.csv"
        file_path = os.path.join(self.config.DATA_FOLDER, filename)
        file_exists = os.path.isfile(file_path)

        try:
            # Use lock to ensure multiple threads don't try to create/write headers at exact same time
            with file_lock:
                with open(file_path, mode='a', newline='') as file:
                    writer = csv.writer(file)
                    if not file_exists:
                        writer.writerow(['timestamp', 'device_id', 'measured_value'])
                    writer.writerow([timestamp, self.device_id, measurement])
        except Exception as e:
            print(f"✗ [Device {self.device_id}] CSV Error: {e}")

    def send_measurement(self, measurement):
        timestamp = self.get_current_time_str()
        message = {
            "timestamp": timestamp,
            "device_id": int(self.device_id),
            "measured_value": measurement
        }
        #self.log_to_csv(timestamp, measurement)

        try:
            self.channel.basic_publish(
                exchange=self.config.EXCHANGE,
                routing_key=self.config.ROUTING_KEY,
                body=json.dumps(message),
                properties=pika.BasicProperties(
                    delivery_mode=2,
                    content_type='application/json'
                )
            )
            print(f"✓ [Device {self.device_id}] Sent {measurement} kWh at {timestamp}")
            return True
        except Exception as e:
            print(f"✗ [Device {self.device_id}] Send Error: {e}")
            # If send fails, force a reconnect check in the next loop
            if self.connection and self.connection.is_open:
                try:
                    self.connection.close()
                except:
                    pass
            self.connect()
            return False

    def run(self, interval_minutes=10):
        self.connect()
        print(f"✓ [Device {self.device_id}] Simulator Running (Interval: {interval_minutes}m)")

        try:
            while True:
                measurement = self.generate_measurement()
                self.send_measurement(measurement)
                time.sleep(interval_minutes * 60)
        except KeyboardInterrupt:
            pass
        finally:
            if self.connection:
                try:
                    self.connection.close()
                except:
                    pass

def run_device(device_id, interval):
    sim = DeviceSimulator(device_id)
    sim.run(interval)

def main():
    config = Config()
    ids = config.DEVICE_IDS

    if not os.path.exists(config.DATA_FOLDER):
        try:
            os.makedirs(config.DATA_FOLDER)
        except OSError:
            pass

    interval = int(sys.argv[1]) if len(sys.argv) > 1 else 10

    print("=" * 60)
    print(f"Starting Device Simulator")
    print(f"Timezone: {config.TIMEZONE}")
    print(f"Devices: {ids}")
    print(f"Target: {config.RABBITMQ_HOST}:{config.RABBITMQ_PORT}")
    print(f"Saving CSVs to: {config.DATA_FOLDER}/sensor_data_<id>.csv")
    print("=" * 60)

    threads = []
    for dev_id in ids:
        t = threading.Thread(target=run_device, args=(dev_id, interval))
        t.daemon = True
        t.start()
        threads.append(t)

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping simulator...")

if __name__ == '__main__':
    main()