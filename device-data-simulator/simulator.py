import json
import time
import random
import sys
from datetime import datetime
import pika
from config import Config


class DeviceSimulator:
    """Simulates smart meter energy consumption readings"""

    def __init__(self, device_id):
        self.device_id = device_id
        self.config = Config()
        self.base_load = random.uniform(0.3, 0.8)  # Random base load (kWh per 10 min)
        self.connection = None
        self.channel = None

    def connect(self):
        """Establish RabbitMQ connection"""
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

            # Declare exchange
            self.channel.exchange_declare(
                exchange=self.config.EXCHANGE,
                exchange_type='topic',
                durable=True
            )

            print(f"✓ Connected to RabbitMQ at {self.config.RABBITMQ_HOST}:{self.config.RABBITMQ_PORT}")
            return True

        except Exception as e:
            print(f"✗ Connection failed: {e}")
            return False

    def generate_measurement(self):
        """
        Generate realistic energy consumption based on time of day.
        Returns energy consumed in kWh for the interval.
        """
        hour = datetime.now().hour

        # Time-based consumption patterns
        if 0 <= hour < 6:      # Night: 50-70% of base load
            factor = random.uniform(0.5, 0.7)
        elif 6 <= hour < 9:    # Morning: 80-110% of base load
            factor = random.uniform(0.8, 1.1)
        elif 9 <= hour < 17:   # Day: 70-90% of base load
            factor = random.uniform(0.7, 0.9)
        else:                  # Evening: 100-140% of base load
            factor = random.uniform(1.0, 1.4)

        # Add small random fluctuations
        fluctuation = random.uniform(-0.05, 0.05)

        # Calculate measurement
        measurement = self.base_load * factor + fluctuation

        # Ensure non-negative
        return max(0, round(measurement, 4))

    def send_measurement(self, measurement):
        """Send measurement to RabbitMQ"""
        message = {
            "timestamp": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "device_id": self.device_id,
            "measurement_value": measurement
        }

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
            return True
        except Exception as e:
            print(f"✗ Failed to send: {e}")
            return False

    def run(self, interval_minutes=10):
        """Run the simulator"""
        if not self.connect():
            return

        print("=" * 60)
        print("Device Data Simulator Started")
        print("=" * 60)
        print(f"Device ID:       {self.device_id}")
        print(f"Base Load:       {self.base_load:.2f} kWh per {interval_minutes} min")
        print(f"Interval:        {interval_minutes} minutes")
        print(f"Press Ctrl+C to stop")
        print("=" * 60)

        count = 0
        try:
            while True:
                # Generate and send measurement
                measurement = self.generate_measurement()

                if self.send_measurement(measurement):
                    count += 1
                    timestamp = datetime.now().strftime("%H:%M:%S")
                    print(f"✓ [{count:3d}] {timestamp} | Device {self.device_id} | {measurement} kWh")

                # Wait for next interval
                time.sleep(interval_minutes * 60)

        except KeyboardInterrupt:
            print("\n" + "=" * 60)
            print(f"Simulator stopped. Total sent: {count} measurements")
            print("=" * 60)
        finally:
            if self.connection:
                self.connection.close()


def main():
    if len(sys.argv) < 2:
        print("Usage: python simulator.py <device_id> [interval_minutes]")
        print("\nExamples:")
        print("  python simulator.py 1          # Device 1, 10-minute intervals")
        print("  python simulator.py 1 1        # Device 1, 1-minute intervals (testing)")
        print("  python simulator.py 2 10       # Device 2, 10-minute intervals")
        sys.exit(1)

    device_id = int(sys.argv[1])
    interval = int(sys.argv[2]) if len(sys.argv) > 2 else 10

    simulator = DeviceSimulator(device_id)
    simulator.run(interval)


if __name__ == '__main__':
    main()