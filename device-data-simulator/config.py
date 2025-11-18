import os

class Config:
    """Simulator configuration"""

    # RabbitMQ Connection
    RABBITMQ_HOST = os.getenv('RABBITMQ_HOST', 'localhost')
    RABBITMQ_PORT = int(os.getenv('RABBITMQ_PORT', 5672))
    RABBITMQ_USER = os.getenv('RABBITMQ_USER', 'rabbitmq_user')
    RABBITMQ_PASS = os.getenv('RABBITMQ_PASS', 'rabbitmq_pass')

    # RabbitMQ Exchange and Routing
    EXCHANGE = 'device.data.exchange'
    ROUTING_KEY = 'device.data'

    # Simulator Settings
    # List of Device IDs to simulate
    DEVICE_IDS = os.getenv('DEVICE_IDS', '1,2,6,7,8,9,10,11').split(',')

    # Folder where CSV files will be saved
    DATA_FOLDER = os.getenv('DATA_FOLDER_PATH', 'sensor_data')

    # Timezone Configuration
    TIMEZONE = 'Europe/Bucharest'