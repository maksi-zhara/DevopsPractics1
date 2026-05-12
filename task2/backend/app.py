import os

from flask import Flask, jsonify
from flask_cors import CORS


app = Flask(__name__)
CORS(app)


@app.get("/api/hello")
def hello():
    name = os.getenv("NAME", "World")
    return jsonify(
        {
            "message": f"Hello {name}",
            "name": name,
            "source": "environment variable NAME",
        }
    )


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
