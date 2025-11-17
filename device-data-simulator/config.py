class Config:
    """Simulator configuration"""

    # RabbitMQ Connection
    RABBITMQ_HOST = 'localhost'
    RABBITMQ_PORT = 5672
    RABBITMQ_USER = 'rabbitmq_user'
    RABBITMQ_PASS = 'rabbitmq_pass'

    # RabbitMQ Exchange and Routing
    EXCHANGE = 'device.data.exchange'
    ROUTING_KEY = 'device.data'