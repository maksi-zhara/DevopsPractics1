import os
import random
import time

from flask import Flask, jsonify
from flask_cors import CORS
from psycopg import connect
from psycopg import OperationalError
from psycopg.rows import dict_row


app = Flask(__name__)
CORS(app)
app.config["DB_INITIALIZED"] = False

FIRST_NAMES = ["Lev", "Anna", "Maksim", "Ivan", "Maria", "Olga", "Pavel"]
LAST_NAMES = ["Sidorov", "Smirnov", "Orlov", "Kuznetsov", "Petrov", "Morozov"]
MIDDLE_NAMES = ["Petrovich", "Igorevna", "Denisovich", "Sergeevna", "Olegovich", "Ivanovna"]

def get_connection():
    retries = max(int(os.getenv("POSTGRES_CONNECT_RETRIES", "0")), 0)
    delay = max(float(os.getenv("POSTGRES_CONNECT_DELAY", "0")), 0)
    last_error = None

    for attempt in range(retries + 1):
        try:
            return connect(
                host=os.getenv("POSTGRES_HOST", "localhost"),
                port=os.getenv("POSTGRES_PORT", "5432"),
                dbname=os.getenv("POSTGRES_DB", "task3db"),
                user=os.getenv("POSTGRES_USER", "task3user"),
                password=os.getenv("POSTGRES_PASSWORD", "task3password"),
                connect_timeout=int(os.getenv("POSTGRES_CONNECT_TIMEOUT", "3")),
                row_factory=dict_row,
            )
        except OperationalError as error:
            last_error = error
            if attempt == retries:
                raise
            time.sleep(delay)

    raise last_error


def initialize_database():
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                CREATE TABLE IF NOT EXISTS people (
                    id SERIAL PRIMARY KEY,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    middle_name TEXT NOT NULL,
                    age INTEGER NOT NULL CHECK (age >= 0)
                )
                """
            )
            cur.execute("SELECT COUNT(*) AS total FROM people")
            total = cur.fetchone()["total"]
            if total == 0:
                cur.executemany(
                    """
                    INSERT INTO people (first_name, last_name, middle_name, age)
                    VALUES (%s, %s, %s, %s)
                    """,
                    [
                        ("Lev", "Sidorov", "Petrovich", 23),
                        ("Anna", "Smirnova", "Igorevna", 31),
                        ("Maksim", "Orlov", "Denisovich", 27),
                    ],
                )
        conn.commit()
    app.config["DB_INITIALIZED"] = True


def build_random_person():
    return {
        "first_name": random.choice(FIRST_NAMES),
        "last_name": random.choice(LAST_NAMES),
        "middle_name": random.choice(MIDDLE_NAMES),
        "age": random.randint(18, 65),
    }


def ensure_database_initialized():
    if app.config["DB_INITIALIZED"]:
        return
    initialize_database()


@app.get("/api/people")
def list_people():
    ensure_database_initialized()

    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, first_name, last_name, middle_name, age
                FROM people
                ORDER BY id
                """
            )
            people = cur.fetchall()

    return jsonify(
        {
            "message": "People loaded from PostgreSQL",
            "count": len(people),
            "people": people,
        }
    )


@app.post("/api/people/random")
def create_random_person():
    ensure_database_initialized()
    person = build_random_person()

    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO people (first_name, last_name, middle_name, age)
                VALUES (%s, %s, %s, %s)
                RETURNING id, first_name, last_name, middle_name, age
                """,
                (
                    person["first_name"],
                    person["last_name"],
                    person["middle_name"],
                    person["age"],
                ),
            )
            created_person = cur.fetchone()
        conn.commit()

    return jsonify(
        {
            "message": "Random person added to PostgreSQL",
            "person": created_person,
        }
    ), 201


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


@app.get("/startup")
def startup():
    try:
        ensure_database_initialized()
    except OperationalError as error:
        return jsonify({"status": "error", "database": str(error)}), 503

    return jsonify({"status": "ok", "database": "initialized"})


@app.get("/ready")
def ready():
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                cur.fetchone()
    except OperationalError as error:
        return jsonify({"status": "error", "database": str(error)}), 503

    return jsonify({"status": "ok", "database": "reachable"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
