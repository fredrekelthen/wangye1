# 本地中文 Embedding 服务

为 javahouduan 的 RAG 知识库提供向量化能力，**替代原先的 GLM `embedding-3`**。

## 为什么换掉 GLM embedding

对话模型已切换到 DeepSeek（`deepseek-flash`）。DeepSeek 官方 API **不提供 embeddings 接口**
（实测 `POST /embeddings` 返回 404，`GET /models` 只有 `deepseek-flash` 与 `deepseek-v4-pro` 两个模型），
因此向量化改用本地模型，好处是：

- **零 API 成本**，不受额度和限流影响
- **离线可用**，不依赖外部网络
- **语义效果更好**：实测「梯度」与「什么是梯度」余弦相似度 0.924，
  可弥补原先 BM25 中文分词导致的"自然问句召回为 0"问题

## 模型

| 项 | 值 |
|---|---|
| 模型 | `BAAI/bge-small-zh-v1.5` |
| 维度 | **512**（原 GLM embedding-3 为 2048） |
| 运行方式 | ONNX Runtime（`fastembed`），无需 PyTorch、无需 GPU |
| 模型体积 | 约 100 MB，首次启动自动下载（走 `hf-mirror.com` 镜像） |
| 缓存位置 | `%TEMP%\fastembed_cache` |

可通过环境变量覆盖：

```bat
set EMBEDDING_MODEL=BAAI/bge-base-zh-v1.5   rem 768 维，效果更好但更慢
```

## 启动

```bat
start.bat
```

或手动：

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python embedding_server.py --port 8000
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 健康检查，返回模型名与维度 |
| GET | `/info` | 模型信息（模型名、维度、批大小） |
| POST | `/embed` | 向量化，请求 `{"texts":["...","..."]}`，响应 `{"vectors":[[...]],"dim":512}` |

自检：

```bash
curl http://localhost:8000/health
curl -X POST http://localhost:8000/embed -H "Content-Type: application/json" -d "{\"texts\":[\"梯度\"]}"
```

## 与后端的关系

```
javahouduan 后端 (8080)
  └─ EmbeddingClient  ──HTTP──>  本服务 (8000)
                                     └─ bge-small-zh-v1.5 → 512 维向量
                                          └─ 存入 MongoDB material_chunks.embedding
```

配置项（`application.yml`）：

```yaml
embedding:
    local:
        base-url: http://localhost:8000
        model: bge-small-zh-v1.5
        dimension: 512
```

## 注意事项

- **维度变化**：从 GLM 的 2048 维换成 512 维后，历史 chunk 的向量全部失效，
  需要调用后端 `POST /api/materials/reindex` 全量重算（后端会自动跳过维度不匹配的旧向量）。
- **首次启动慢**：要下载模型（约 100MB）；之后启动只需加载本地缓存，约 3~5 秒。
- **内存占用**：模型常驻内存约 200~300 MB。
