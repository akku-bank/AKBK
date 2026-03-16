import time
import sys
from kafka.admin import KafkaAdminClient, NewTopic
from kafka.errors import NoBrokersAvailable

KAFKA_BROKER = "akkubangkku_kafka:29092"

REQUIRED_TOPICS = [
    ("transaction", 3, 1),
    ("payment", 3, 1),
    ("transfer", 3, 1),
]

def wait_for_kafka_and_init():
    print(f"[Setup] Kafka({KAFKA_BROKER}) 연결 시도")

    max_retries = 30

    for i in range(max_retries):
        try:
            admin = KafkaAdminClient(bootstrap_servers=KAFKA_BROKER)

            print("[Setup] Kafka 연결 성공")

            existing_topics = admin.list_topics()
            new_topics = []

            for topic, partitions, replication in REQUIRED_TOPICS:
                if topic not in existing_topics:
                    new_topics.append(
                        NewTopic(
                            name=topic,
                            num_partitions=partitions,
                            replication_factor=replication
                        )
                    )

            if new_topics:
                admin.create_topics(new_topics=new_topics)
                print("[Setup] 생성된 토픽:", [t.name for t in new_topics])
            else:
                print("[Setup] 모든 토픽 이미 존재")

            admin.close()
            return True

        except NoBrokersAvailable:
            print(f"[Setup] Kafka 대기중... ({i+1}/{max_retries})")
            time.sleep(2)

        except Exception as e:
            print("[Setup] 오류:", e)
            time.sleep(2)

    print("[Setup] Kafka 연결 실패")
    return False


if __name__ == "__main__":
    if not wait_for_kafka_and_init():
        sys.exit(1)