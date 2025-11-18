FROM python:3.9-slim

WORKDIR /app

RUN apt-get update && apt-get install -y tzdata && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Set python to be unbuffered so logs show up immediately
ENV PYTHONUNBUFFERED=1

# Run the simulator
# Default command runs with 10 minute interval
CMD ["python", "simulator.py", "10"]