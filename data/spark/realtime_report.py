from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, LongType, TimestampType

# 1. Spark 세션 생성 (Kafka 커넥터 포함)
spark = SparkSession.builder \
    .appName("AkkubangkkuRealtimeReport") \
    .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.1") \
    .getOrCreate()

# 2. Kafka 메시지 구조(Schema) 정의
schema = StructType([
    StructField("event_type", StringType()),
    StructField("data", StructType([
        StructField("id", StringType()),
        StructField("user_id", IntegerType()),
        StructField("account_id", StringType()),
        StructField("merchant_id", IntegerType()),
        StructField("amount", LongType()),
        StructField("transaction_type", StringType()),
        StructField("sub_category_name", StringType()),
        StructField("merchant_name", StringType()),
        StructField("created_at", StringType())  # 파싱 후 Timestamp로 변환 예정
    ]))
])

# 3. Kafka 스트림 읽기
df = spark.readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "akkubangkku_kafka:29092") \
    .option("subscribe", "transaction") \
    .option("startingOffsets", "latest") \
    .load()

# 4. 바이너리 데이터를 JSON으로 변환
parsed_df = df.selectExpr("CAST(value AS STRING)") \
    .select(from_json(col("value"), schema).alias("data")) \
    .select("data.data.*")

# 5. 콘솔로 데이터 출력 (테스트용)
query = parsed_df.writeStream \
    .outputMode("append") \
    .format("console") \
    .start()

query.awaitTermination()