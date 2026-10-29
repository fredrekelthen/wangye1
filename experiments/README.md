# experiments/ 实验目录约定（TED-O 第一阶段准备）

> 本目录承载 TED-O 全部实验资产：埋点数据、结果表、注入器、训练脚本、复现说明。
> 约束：**不动生产代码路径**；实验通过 `@Profile("experiment")` 的 `ExperimentRunner` 驱动。

## 1. 目录结构

```
experiments/
├── README.md                 # 本文件：约定与入口
├── TRACKING.md               # 阶段跟踪看板（进度/风险/周报）
├── data/
│   ├── tasks/                # 任务集 T1（50 题 → 最终 120 题）
│   ├── faults/               # 故障轨迹集 T2（F1–F6 注入输出）
│   ├── credit_pairs/         # 信用偏好对 T3
│   └── references/           # 三源参照（教材原文/题库答案/仿真解[可选]）
├── traces/                   # 每次协作的结构化 JSONL（exp_id 命名的子目录）
├── results/                  # 结果表（.json / .csv）与图表
├── scripts/
│   ├── run_experiment.py     # 实验驱动（调用后端 /task 或直接构造 Task）
│   ├── fault_inject.py       # 6 类故障算子注入器
│   ├── train_credit.py       # embedding-linear 信用模型离线训练（numpy + bge）
│   └── compute_metrics.py    # 指标计算（效率/结构/可信）
└── schema/
    ├── trace_event.schema.json
    └── dataset.schema.json
```

## 2. exp_id 规范

```
exp-{stage}-{config}-{dataset}-{seed}-{yyyymmdd}
```

- `stage` ∈ `s0`（可观测）/`s1`（编排）/`s2`（信用）/`s3`（闭环）/`s4`（评测）
- `config` ∈ `pure` / `selfrefine` / `staticdag` / `naivepar` / `tedo-table` / `tedo-bandit` / `credit-none` / `credit-keyword` / `credit-judge` / `credit-tedo`
- `dataset` ∈ `t1`（任务集）/`t2`（故障）/`t3`（信用对）
- `seed` ∈ `1..3`（全量 ≥3 次重复）

示例：`exp-s2-credit-tedo-t2-1-20261110`

## 3. 数据 schema

- `schema/trace_event.schema.json`：单条协作事件（`task_id/level/agent/start/end/status/tokens/wall_ms/injected_from/credit`）。
- `schema/dataset.schema.json`：三类数据集统一字段与版本号。
- 未回传的用量显式记 `null`，不写 `0`。

## 4. 复现要求（Gate 3 验收）

干净环境按 `experiments/README.md` 与后端 `README`，能在 **30 分钟内**跑通 10 题 mini 实验并产出基线表。
