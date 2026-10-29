"""本地中文 Embedding 服务（替代 GLM embedding-3）。

背景：对话模型切换到 DeepSeek 后，DeepSeek 官方 API 不提供 embeddings 接口
（实测 POST /embeddings 返回 404），因此向量化改为本地模型，零 API 成本。

模型：BAAI/bge-small-zh-v1.5（512 维，中文语义，ONNX 运行时，无需 GPU / PyTorch）

启动::

    python embedding_server.py                 # 默认监听 0.0.0.0:8000
    python embedding_server.py --port 8001

接口::

    GET  /health   → {"status":"UP","model":"...","dim":512,"loaded":true}
    POST /embed    → 请求 {"texts":["...","..."]}
                     响应 {"vectors":[[...],[...]],"dim":512,"model":"..."}
"""
from __future__ import annotations

import argparse
import logging
import os

# 国内镜像 + 禁用 Xet 存储后端（镜像站不支持 xet，会返回 401）
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
os.environ.setdefault("HF_HUB_DISABLE_XET", "1")
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")
os.environ.setdefault("HF_HUB_DISABLE_TELEMETRY", "1")

import numpy as np                                  # noqa: E402
from fastembed import TextEmbedding                 # noqa: E402
from flask import Flask, jsonify, request           # noqa: E402

MODEL_NAME = os.environ.get("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
MAX_BATCH = int(os.environ.get("EMBEDDING_MAX_BATCH", "256"))

app = Flask(__name__)
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s - %(message)s")
log = logging.getLogger("embedding")

_model: TextEmbedding | None = None
_dim: int | None = None


def get_model() -> TextEmbedding:
    global _model, _dim
    if _model is None:
        log.info("加载 embedding 模型：%s ...", MODEL_NAME)
        _model = TextEmbedding(model_name=MODEL_NAME)
        _dim = int(len(np.array(list(_model.embed(["维度探测"]))[0])))
        log.info("模型加载完成，维度 = %d", _dim)
    assert _dim is not None
    return _model


def embed_texts(texts: list[str]) -> list[list[float]]:
    model = get_model()
    vectors: list[list[float]] = []
    for start in range(0, len(texts), MAX_BATCH):
        batch = texts[start:start + MAX_BATCH]
        vectors.extend(
            [round(float(x), 8) for x in vec]
            for vec in model.embed(batch)
        )
    return vectors


@app.get("/health")
def health():
    return jsonify({
        "status": "UP",
        "model": MODEL_NAME,
        "dim": _dim,
        "loaded": _model is not None,
    })


@app.get("/info")
def info():
    get_model()
    return jsonify({"model": MODEL_NAME, "dim": _dim, "maxBatch": MAX_BATCH})


@app.post("/embed")
def embed():
    payload = request.get_json(silent=True) or {}
    texts = payload.get("texts")

    if isinstance(texts, str):
        texts = [texts]
    if not texts or not isinstance(texts, list):
        return jsonify({"error": "texts 不能为空，且必须是字符串数组"}), 400

    texts = [("" if t is None else str(t)) for t in texts]
    try:
        vectors = embed_texts(texts)
    except Exception as exc:                        # noqa: BLE001
        log.exception("向量化失败")
        return jsonify({"error": f"向量化失败：{exc}"}), 500

    log.info("embed %d 条文本，维度 %d", len(texts), len(vectors[0]) if vectors else 0)
    return jsonify({"vectors": vectors, "dim": len(vectors[0]) if vectors else 0,
                    "model": MODEL_NAME})


def main() -> None:
    parser = argparse.ArgumentParser(description="本地中文 Embedding 服务")
    parser.add_argument("--host", default="0.0.0.0", help="监听地址，默认 0.0.0.0")
    parser.add_argument("--port", type=int, default=8000, help="监听端口，默认 8000")
    parser.add_argument("--no-warmup", action="store_true", help="启动时不预加载模型")
    args = parser.parse_args()

    if not args.no_warmup:
        get_model()

    log.info("Embedding 服务启动：http://%s:%s", args.host, args.port)
    app.run(host=args.host, port=args.port, threaded=True)


if __name__ == "__main__":
    main()
