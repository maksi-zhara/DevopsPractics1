import json
import os
import time

import pika
import psycopg2
import yaml


def load_config(path: str):
    with open(path, "r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)

    cfg["rabbitmq"]["host"] = os.getenv("RABBITMQ_HOST", cfg["rabbitmq"]["host"])
    cfg["rabbitmq"]["port"] = int(os.getenv("RABBITMQ_PORT", cfg["rabbitmq"]["port"]))
    cfg["rabbitmq"]["username"] = os.getenv("RABBITMQ_USER", cfg["rabbitmq"]["username"])
    cfg["rabbitmq"]["password"] = os.getenv("RABBITMQ_PASSWORD", cfg["rabbitmq"]["password"])
    cfg["rabbitmq"]["queue"] = os.getenv("RABBITMQ_QUEUE", cfg["rabbitmq"]["queue"])

    cfg["postgres"]["host"] = os.getenv("POSTGRES_HOST", cfg["postgres"]["host"])
    cfg["postgres"]["port"] = int(os.getenv("POSTGRES_PORT", cfg["postgres"]["port"]))
    cfg["postgres"]["db"] = os.getenv("POSTGRES_DB", cfg["postgres"]["db"])
    cfg["postgres"]["user"] = os.getenv("POSTGRES_USER", cfg["postgres"]["user"])
    cfg["postgres"]["password"] = os.getenv("POSTGRES_PASSWORD", cfg["postgres"]["password"])
    return cfg


def pg_connection(cfg):
    return psycopg2.connect(
        host=cfg["postgres"]["host"],
        port=cfg["postgres"]["port"],
        dbname=cfg["postgres"]["db"],
        user=cfg["postgres"]["user"],
        password=cfg["postgres"]["password"],
    )


def process_message(cfg, body):
    payload = json.loads(body)
    order_id = payload.get("orderId")
    if order_id is None:
        return

    # Simulate asynchronous processing before reporting final status.
    time.sleep(1)

    with pg_connection(cfg) as conn:
        with conn.cursor() as cur:
            cur.execute("UPDATE orders SET status = %s WHERE id = %s", ("Обработано", order_id))
        conn.commit()


def main():
    cfg_path = os.getenv("WORKER_CONFIG", "config.yml")
    cfg = load_config(cfg_path)

    credentials = pika.PlainCredentials(cfg["rabbitmq"]["username"], cfg["rabbitmq"]["password"])

    while True:
        try:
            params = pika.ConnectionParameters(
                host=cfg["rabbitmq"]["host"],
                port=cfg["rabbitmq"]["port"],
                credentials=credentials,
            )
            connection = pika.BlockingConnection(params)
            channel = connection.channel()
            channel.queue_declare(queue=cfg["rabbitmq"]["queue"], durable=True)
            channel.basic_qos(prefetch_count=cfg["worker"]["prefetch_count"])

            def callback(ch, method, _properties, body):
                try:
                    process_message(cfg, body)
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                except Exception:
                    ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

            channel.basic_consume(queue=cfg["rabbitmq"]["queue"], on_message_callback=callback)
            print("worker started")
            channel.start_consuming()
        except Exception as exc:
            print(f"worker error: {exc}, reconnecting in 5s")
            time.sleep(5)


if __name__ == "__main__":
    main()
