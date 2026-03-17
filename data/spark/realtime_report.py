from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, LongType, TimestampType
from pyspark.sql.functions import from_json, col, to_timestamp, date_format, date_sub, next_day

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

# 4. 바이너리 데이터를 JSON으로 변환 (기존 코드)
raw_df = df.selectExpr("CAST(value AS STRING)") \
    .select(from_json(col("value"), schema).alias("data")) \
    .select("data.data.*")

# 5. 날짜 가공 로직
transformed_df = raw_df.withColumn(
    "ts", to_timestamp(col("created_at"), "yyyy-MM-dd HH:mm:ss")
).withColumn(
    # 요일 추출 (소문자 3글자: mon, tue, wed...) - Redis 필드명과 일치시키기 위함
    "day_of_week", date_format(col("ts"), "E").cast("string").substr(1, 3)
).withColumn(
    # 주간 시작일(월요일) 계산
    # 'next_day(date_sub(ts, 7), "Monday")'는 해당 날짜가 포함된 주의 월요일을 구하는 공식입니다.
    "start_day", next_day(date_sub(col("ts"), 7), "Monday")
)

# 6. 콘솔 출력 (가공된 컬럼 확인)
query = transformed_df.select(
    "user_id", "amount", "sub_category_name", "day_of_week", "start_day"
).writeStream \
    .outputMode("append") \
    .format("console") \
    .start()

query.awaitTermination()