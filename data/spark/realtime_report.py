import os
import redis
import psycopg2
from datetime import datetime, timedelta
from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col, to_timestamp, date_format, date_sub, next_day
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, LongType

# 1. Spark 세션 설정
spark = SparkSession.builder \
    .appName("Akkubangkku-Full-Pipeline") \
    .getOrCreate()

# [수정] Kafka 스키마 정의: 샘플 데이터에 맞춰 created_at(d 포함)으로 변경
schema = StructType([
    StructField("event_type", StringType()),
    StructField("data", StructType([
        StructField("id", StringType()),
        StructField("user_id", StringType()), 
        StructField("amount", LongType()),
        StructField("transaction_type", StringType()),
        StructField("sub_category_id", IntegerType()),
        StructField("sub_category_name", StringType()),
        StructField("created_at", StringType()) # create_at -> created_at
    ]))
])

# 2. DB 연결 정보 (환경 변수)
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "port": os.getenv("DB_PORT"),
    "database": os.getenv("DB_NAME"),
    "user": os.getenv("DB_USERNAME"),
    "password": os.getenv("DB_PASSWORD")
}

def get_db_conn():
    return psycopg2.connect(**DB_CONFIG)

# 3. Redis 웜업 함수
def warm_up_redis(r, u_id, s_day, t_type):
    weekly_key = f"report:weekly:{u_id}:{s_day}:{t_type}"
    category_key = f"report:category:{u_id}:{s_day}:{t_type}"
    
    if not r.exists(weekly_key):
        try:
            conn = get_db_conn()
            cur = conn.cursor()
            
            # weekly_report 복구
            cur.execute("""
                SELECT mon, tue, wed, thu, fri, sat, sun, total_amount 
                FROM weekly_report WHERE user_id = %s AND start_day = %s AND type = %s
            """, (u_id, s_day, t_type))
            row = cur.fetchone()
            if row:
                fields = ["mon", "tue", "wed", "thu", "fri", "sat", "sun", "total_amount"]
                r.hset(weekly_key, mapping=dict(zip(fields, map(str, row))))
            
            # weekly_category_ratio 복구 (금액만)
            cur.execute("""
                SELECT sub_category_id, spending_amount FROM weekly_category_ratio 
                WHERE user_id = %s AND start_day = %s
            """, (u_id, s_day))
            rows = cur.fetchall()
            for cat_id, amt in rows:
                r.hset(category_key, str(cat_id), str(amt))
                
            cur.close()
            conn.close()
            # 신규/복구 키에 대해 7일 TTL 설정
            r.expire(weekly_key, 604800)
            r.expire(category_key, 604800)
        except Exception as e:
            print(f"⚠️ Warm-up Error for user {u_id}: {e}")

# 4. Postgres Upsert 함수 (30분 주기)
def backup_to_postgres(r, u_id, s_day, t_type):
    sync_key = f"sync:last:{u_id}:{s_day}:{t_type}"
    last_sync = r.get(sync_key)
    
    # 30분이 지났거나 처음인 경우만 실행 (테스트 시 seconds=10 등으로 수정 가능)
    if not last_sync or (datetime.now() - datetime.fromisoformat(last_sync)) > timedelta(minutes=30):
        weekly_key = f"report:weekly:{u_id}:{s_day}:{t_type}"
        category_key = f"report:category:{u_id}:{s_day}:{t_type}"
        
        weekly_data = r.hgetall(weekly_key)
        cat_data = r.hgetall(category_key)
        
        if not weekly_data: return

        try:
            conn = get_db_conn()
            cur = conn.cursor()
            # 1. Weekly Report Upsert
            cur.execute("""
                INSERT INTO weekly_report (user_id, start_day, type, mon, tue, wed, thu, fri, sat, sun, total_amount, end_day)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (user_id, start_day, type) 
                DO UPDATE SET mon=EXCLUDED.mon, tue=EXCLUDED.tue, wed=EXCLUDED.wed, thu=EXCLUDED.thu, 
                              fri=EXCLUDED.fri, sat=EXCLUDED.sat, sun=EXCLUDED.sun, total_amount=EXCLUDED.total_amount;
            """, (u_id, s_day, t_type, 
                  weekly_data.get('mon',0), weekly_data.get('tue',0), weekly_data.get('wed',0),
                  weekly_data.get('thu',0), weekly_data.get('fri',0), weekly_data.get('sat',0), 
                  weekly_data.get('sun',0), weekly_data.get('total_amount',0),
                  (datetime.strptime(s_day, '%Y-%m-%d') + timedelta(days=6)).date()))

            # 2. Category Ratio Upsert (비율 계산 포함)
            total = int(weekly_data.get('total_amount', 1))
            for cat_id, amt in cat_data.items():
                ratio = float(int(amt) / total) if total > 0 else 0
                cur.execute("""
                    INSERT INTO weekly_category_ratio (user_id, start_day, sub_category_id, spending_amount, ratio)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (user_id, start_day, sub_category_id)
                    DO UPDATE SET spending_amount=EXCLUDED.spending_amount, ratio=EXCLUDED.ratio;
                """, (u_id, s_day, cat_id, amt, ratio))
            
            conn.commit()
            r.set(sync_key, datetime.now().isoformat())
            print(f"✅ DB Backup Success: User {u_id}")
            cur.close()
            conn.close()
        except Exception as e:
            print(f"❌ DB Sync Error: {e}")

# 5. Main Batch Processor
def process_batch(batch_df, batch_id):
    # 환경변수에서 Redis 정보 로드
    redis_host = os.getenv('REDIS_HOST', 'akkubangkku_redis')
    r = redis.Redis(host=redis_host, port=6379, db=0, decode_responses=True)
    
    rows = batch_df.collect()
    if not rows: return
    
    for row in rows:
        # [방어 로직] 데이터가 None인 경우 건너뜀 (AttributeError 방지)
        if row['day_of_week'] is None or row['user_id'] is None:
            print(f"⚠️ Skipping invalid row: {row}")
            continue

        u_id = row['user_id']
        s_day = str(row['start_day'])
        # 타입 결정 로직
        t_type = "SPEND" if row['transaction_type'] in ['TRANSFER', 'SPEND'] else "INCOME"
        cat_id = row['sub_category_id']
        amt = row['amount']
        dow = row['day_of_week'].lower()

        # Step 1: Warm-up (DB -> Redis)
        warm_up_redis(r, u_id, s_day, t_type)
        
        # Step 2: Update Redis (Real-time)
        pipe = r.pipeline()
        pipe.hincrby(f"report:weekly:{u_id}:{s_day}:{t_type}", dow, amt)
        pipe.hincrby(f"report:weekly:{u_id}:{s_day}:{t_type}", "total_amount", amt)
        pipe.hincrby(f"report:category:{u_id}:{s_day}:{t_type}", str(cat_id), amt)
        pipe.execute()
        
        # Step 3: Periodic DB Backup (30min)
        backup_to_postgres(r, u_id, s_day, t_type)

# 6. Kafka Stream 설정 및 가공
df = spark.readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "akkubangkku_kafka:29092") \
    .option("subscribe", "transaction") \
    .option("failOnDataLoss", "false") \
    .load()

# [수정] created_at 필드명 반영 및 날짜 포맷 (T 제외) 적용
transformed_df = df.select(from_json(col("value").cast("string"), schema).alias("parsed")) \
    .select("parsed.data.*") \
    .withColumn("ts", to_timestamp(col("created_at"), "yyyy-MM-dd HH:mm:ss")) \
    .filter(col("ts").isNotNull()) \
    .withColumn("day_of_week", date_format(col("ts"), "E")) \
    .withColumn("start_day", next_day(date_sub(col("ts"), 7), "Monday"))

# 7. Sink 시작
query = transformed_df.writeStream \
    .foreachBatch(process_batch) \
    .option("checkpointLocation", "/opt/spark/work-dir/checkpoints/realtime-report") \
    .start()

query.awaitTermination()