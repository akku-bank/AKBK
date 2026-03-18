FROM python:3.11-slim

WORKDIR /app

RUN pip install kafka-python

COPY setup_kafka.py .

CMD ["python", "setup_kafka.py"]