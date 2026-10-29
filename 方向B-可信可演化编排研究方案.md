# 方向 B 优化研究方案：可信·可演化·依赖感知的多智能体编排（TED-O）

> 本文是对 [`javahouduan论文发表学术规划.md`](javahouduan论文发表学术规划.md) **方向 B** 的升级版。
> 定位：保持方向 B 原目标——**教学导学场景下多智能体编排的"效率与质量"**——不动，用两份参考论文（Puppeteer 的可演化编排、SentinelNet 的信用制门控）把"工程技巧"抬升为"方法研究"。
> 简称：**TED-O**（**T**rust-aware, **E**volvable, **D**ependency-aware **O**rchestration，可信·可演化·依赖感知编排框架）。

---

## 0. 一页结论

原方向 B 的实质是"把依赖分层并行做成一个静态调度算法 + 实证对比"。问题在于：**静态 DAG 调度是 AutoGen/LangGraph/MacNet 都覆盖的红海，单一"效率"主张撑不起方法贡献，也经不起"质量如何保证"这一追问。**

升级思路只有一句话：**把编排从"静态的调度器"变成"可演化的决策者"，并把"质量"从"是否抛异常"变成"可门控的可信度"，让效率与可信共享同一份轨迹、被同一套指标度量。**

- 保留原目标与原有资产：`computeLevels()` 分层、`buildAgentTask()` 上下文注入、`runWithRetry()` 容错、共享线程池、状态机、链路追踪——全部保留，作为"依赖骨架"与"基线"。
- 新增三块（全部可由两份论文论证）：①依赖约束下的可演化编排；②信用制上下文门控；③轨迹即证据的双目标度量。

一句话卖点（写给审稿人）：**"在依赖受约束的教育多智能体系统中，用可演化编排决定'叫谁、叫多深多宽'，用信用门控决定'信谁的产出'，并让两者在单一轨迹上协同优化效率与可信度。"**

## 0.5 项目实况核验（全项目走查后的方案校正）

> 本次按 `start.bat` → `houduan/backend` → `qianduan` → `embedding-service` → 材料目录全链路走查，核验了方案里每处对项目现状的假设。下表分「确认可用 / 必须修正 / 边界澄清」三栏，后续章节已据此改写。

| 实况（代码/文件证据） | 对方案的影响 |
|---|---|
| ✅ **本地免费 Embedding**：`houduan/embedding-service`（`BAAI/bge-small-zh-v1.5`，512 维，fastembed+ONNX，**零 API 成本、无 GPU**），`EmbeddingClient` 已接入 | 创新 2 的"embedding-linear 信用模型"**完全离线可做**；`r_reference` 的"事实对齐"项可用同一模型向量化，成本≈0 |
| ✅ **Python 运行时可用**：embedding 服务即 Python（numpy/fastembed/flask） | 信用模型训练、故障注入、实验脚本都在 Python 侧离线完成，**Java 侧只需加载线性权重做点积**，绕开"pom 无 ML 库"的限制 |
| ✅ **9 章教材原文在库**：`盛东亮…电磁场与电磁波（第4版）` 9 个 `.docx`（第一章~第九章+附件，约 20MB） | 是 RAG 语料 + 任务集知识点来源 + `r_reference` 的**主锚定**；任务集生成器应从这里取知识点，而非仅 2 章的 `DataInitializer` 代码种子 |
| ✅ **图谱可视化可复用**：前端 `@antv/g6` + `KnowledgeGraph.vue`（9 章 772 行概念树） | 创新 3 的"Agent 交互图"可视化直接复用 G6；且可顺势讲清"概念图 ≠ Agent 图" |
| ✅ **编排后端自包含**：`MultiAgentController` 暴露 `/task` `/teach` `/assess` `/qa` `/plan` `/em-code` `/intent/detect` | 实验驱动走 HTTP 或直接 `CollaborationManager` 调用均可；前端只接了 `/em-code` 与 `/intent/detect`，**编排 `/task` 未被 UI 消费 → 改造零 UI 风险** |
| ⚠️ **仿真参照是外部依赖**：`qigger/python-emulation`（端口 8081）**不在本仓库**，只有客户端 `PythonEmulationService` | "数值参照解"降级为**可选锚定**；主锚定改为教材原文 + 题库答案，仿真解仅在外部服务可用时补测 |
| ⚠️ **"电路分析基础"是图不是文**：目录内是 8 个 `.vsdx`（Visio 架构图），非文本语料 | "跨课程泛化"不能靠电路课程文本；改为**课程内 9 章域多样性**（矢量分析→时变电磁场），跨课程留作 future work |
| ⚠️ **Java 侧无 ML 依赖**：`pom.xml` 仅 pdfbox/poi，无矩阵/优化库 | 信用模型训练放 Python，Java 侧只做点积评分；或仿 embedding-service 加一个轻量 HTTP scorer |
| ⚠️ **无 CI**：`.github` 仅 java-upgrade 钩子脚本 | 可复现性靠 README + 手动脚本（`ExperimentRunner` + 数据 schema），不依赖 CI |
| ℹ️ **两套 Agent 需区分**：`agent/` 包（7 类协作 Agent + `CollaborationManager`）是方向 B 对象；`service/AgentService` 是"项目结构/组件列表"元数据服务；`learning-agent` 是独立"项目学习助手" | 论文实验系统边界只画 `agent/` 包 + `MultiAgentController`，另两个子系统排除在外 |

---

## 1. 研究问题（Research Questions）

| 编号 | 研究问题 | 对应论文锚点 | 对应原方向 B |
|---|---|---|---|
| **RQ1（效率）** | 在"教学依赖规划、评估依赖教学"这类**领域先验依赖约束**下，如何让系统按任务难度与预算，动态决定"激活哪些 Agent、串行多深、并行多宽"，既保证依赖正确性，又避免静态全量激活的冗余？ | Puppeteer §2.1 动态编排 / §3.4 深宽约束 | 原增量点 1（调度形式化） |
| **RQ2（质量/可信）** | 教育开放题**缺乏唯一标准答案**，无法用 `r∈{0,1}` 判定正确性；如何量化并门控各 Agent 贡献的可信度，阻止低质/故障产出沿依赖链传播？ | SentinelNet §4.2 相对信用 / §4.3 bottom-k | 原增量点 2（上下文注入策略） |
| **RQ3（演化）** | 系统能否借任务反馈（质量 − λ·成本）从"静态 DAG"演化出更紧凑、更高质量的协作结构，且该过程**可观测、可解释**？ | Puppeteer §2.2 RL / §3.3 压缩与环化 | 原增量点 4（与框架对比） |
| **RQ4（代价与泛化）** | 信用门控在**无故障**时的误伤代价是多少？在**训练未见过的故障类型**上泛化如何？ | SentinelNet §5.2.3 无攻击基线 / §5.1.4 OOD | （原方案缺失，本次新增） |

> RQ2 与 RQ4 是本次升级补齐的"质量与可信"半场，也是原方向 B 被审稿人最容易攻破的两处空白。

---

## 2. 核心创新点

### 创新 1：依赖约束下的可演化编排（Dependency-Aware Evolving Orchestration）

**要解决什么**：Puppeteer 的编排是"无依赖假设"的——它假设任意 Agent 可被任意顺序激活。但教育任务有**硬先验**（教学必须依赖规划、评估必须依赖教学）。直接照搬会破坏领域语义；保留静态 DAG 又退回原方向 B 的平庸。

**做法**：不"二选一"，而是**外层策略 + 内层骨架**两级结构。

- **内层**：保留 `computeLevels()` 的拓扑分层并行作为"依赖骨架"（保证依赖正确性、保留并行收益）。
- **外层**：引入一个**依赖约束的策略空间**，每一步只从"依赖已满足、尚未激活"的 Agent 集合里选下一个激活者。动作空间不再是全枚举，而是**由依赖图动态裁剪**的合法动作集——这是把 Puppeteer 的序列化编排"限定"在领域 DAG 上。

形式化（可直接进论文）：

```
状态  S_t = (任务 τ, 已产出的贡献摘要, 剩余依赖图 G_rem, 剩余预算 b_t)
动作  a_t ∈ A_legal(S_t) = { 依赖已满足 且 未激活 的 Agent 类型 }   // 依赖裁剪的动作空间
策略  π(a_t | S_t, τ) ：(a) 规则查表 → (b) 上下文 Bandit(UCB/Thompson) → (c) 打分函数
奖励  R = 最终质量 Q − λ · Σ_t C_t ，  C_t = tokens_t · log(1 + t/φ)   // 对齐 Puppeteer Eq.6
```

**相对 Puppeteer 的差异**：动作空间受领域依赖约束（不是自由图）；演化目标不是"任意结构"，而是"在合法子图内找更紧凑的结构"。

### 创新 2：信用制上下文门控（Credit-gated Context Injection）

**要解决什么**：原方向 B 的"质量"只有 `AgentResult.success`（是否抛异常），`ResultAggregator` 也只统计成功数。上游一个"看起来没报错"的错误产出，会顺着 `buildAgentTask()` 注入下游并污染全链。

**做法**：把 SentinelNet 的"相对信用 + bottom-k 隔离"下沉到**上下文注入**这一具体机制上，并做**无 GPU 化**。

- **信用模型**：`R_θ(r,c) = w · [Emb(r); Emb(c); Emb(r)−Emb(c)]`，用 Bradley–Terry 成对逻辑回归训练（等价于 SentinelNet 的 `L_chosen-rej + α·L_align` 在线性化形式下的闭式解）。**落地关键**：Emb 用项目已有的本地 `bge-small-zh-v1.5`（512 维，零 API 成本）；训练在 Python 侧离线完成（numpy 即可），Java 侧只加载线性权重 `w` 做一次点积——全程无 GPU、无额外依赖。
- **三源参照锚定**（按可得性排序）：①**教材原文**（9 章 docx 已在库，`MaterialChunk`，主锚定）②**题库标准答案**（`AssessorAgent.answer`，客观题自动判分）③**数值仿真解**（`PythonEmulationService`，属外部服务 `qigger/python-emulation`，**可选**）——前两者已足够，且比 SentinelNet 只有 benchmark 答案更富。
- **门控机制**：每层对同层产出打分 → 升序 → 取 bottom-k 进**累积黑名单**（跨任务累积，落 Mongo）→ `buildAgentTask()` 注入时跳过黑名单 Agent 的产出。注意是"过滤消息"而非"下线 Agent"（对齐 SentinelNet §4.3.2 保留多样性的设计）。

### 创新 3：轨迹即证据（Trajectory-as-Evidence）

**要解决什么**：效率与可信通常被当作两个独立工作各自评测；本项目资源有限，必须"一次采集、双目标产出"。

**做法**：把已有 `trace`（`List<String>`）升级为**结构化事件日志**（对齐架构整改 G2 的 `SessionEvent` 事件溯源），每次协作落一条 JSONL，重建为"隐式图"，**同一份数据同时产出**：

- 效率侧：token、墙钟、活跃 Agent 数、链长、图密度、环长分布（Puppeteer §3.3 结构指标）。
- 可信侧：每层信用分、DetAcc / FPR / FNR / Recovery@层、门控开销（SentinelNet §5.1.6 指标）。

**贡献表述**：提出"单一协作轨迹的双目标度量协议"，让效率与可信在同一坐标系下可比较、可权衡（`质量−λ·成本` 与 `信用门控` 共用同一份轨迹与同一套埋点）。

**可视化落地**：前端已用 `@antv/g6` 渲染 9 章 772 行的**概念知识树**，可直接复用 G6 渲染 **Agent 交互图**（节点=Agent、边=注入/依赖），并借此在论文里明确区分"概念图（领域知识）"与"Agent 图（协作结构）"——后者是本方案新增的观测对象。

### 创新 4（评测资产）：教育场景的故障注入与 OOD 泛化协议

把 SentinelNet 的 6 类通用攻击翻译为 6 类教育故障算子（幻觉公式 / 丢字段 / 上下文污染 ‖ 资料投毒 / 过度自信 / 角色冒充），按 **3 类训练 + 3 类测试（OOD）** 切分，形成可复现、可公开的评测资产。详见 §5。

---

## 3. 技术路线关键环节（分阶段）

| 阶段 | 关键环节 | 核心产出 | 涉及项目文件（新增/改造） | 人日 |
|---|---|---|---|---|
| **S0 可观测化** | ①token 计量 seam（`LlmCallResult` 的用量目前被 `LlmService.chatStrict()` 丢弃）②轨迹 JSONL + `GraphMetrics` ③依赖扇入开关（让 QA/EM_CODE/RECOMMENDER 产出可被下游消费，否则故障无法传播） | 数据基建 | `llm/MeteredLlmProvider.java`(新)、`LlmService`、`CollaborationManager.recordOutcome()`、`TaskDecomposer.buildTaskDependencies()`、`agent/GraphMetrics.java`(新) | 3 |
| **S1 编排层** | ①调度形式化（§2 创新1 的状态/动作/奖励）②策略空间实现 ③离线奖励 `Q−λ·C` ④三档演化：查表→Bandit→离线策略评估 | 可演化编排器 + 基线（静态 DAG=当前实现） | `agent/OrchestratorPolicy.java`(新)、`agent/DependencyAwareActionSpace.java`(新)、`experiment/ExperimentRunner.java`(新) | 6 |
| **S2 信用层** | ①6 类故障算子注入器 ②四元组样本生成 ③embedding-linear 信用模型训练 ④bottom-k + 累积黑名单 + 注入侧过滤 | 信用门控器 | `agent/CreditModel.java`(新)、`agent/CreditLedger.java`(新)、`CollaborationManager.buildAgentTask()` 加过滤开关 | 7 |
| **S3 闭环** | ①信用反馈编排：低信用 Agent 降低被激活概率 ②"演化+门控"协同的单一轨迹双目标评估 ③课程内跨章泛化（9 章域多样性：矢量分析→时变电磁场） | 协同机制 + 验证 | 在 S1/S2 产物上缝合 | 5 |
| **S4 评测与消融** | ①4 基线对比 ②深宽网格 ③OOD 泛化 ④无故障代价 ⑤开销 ⑥统计口径（误差棒/种子/资源表） | 实验证据链 | `experiment/` 脚本 + 结果表 | 6 |

**与原方向 B 四点增量的对应关系**（证明"保持原目标"）：

| 原增量点 | 去向 |
|---|---|
| 1 调度形式化 + 复杂度分析 | → S1（保留，且升级为"依赖约束策略空间 + 奖励"） |
| 2 上下文注入策略（全量/摘要/选择性） | → S2（升级为"信用门控的选择性注入"） |
| 3 容错与调度耦合（重试/降级 vs 门禁） | → S2 的 bottom-k 门禁 + 原 `runWithRetry` 保留 |
| 4 与编排框架实证对比 | → S4（保留，且基线扩充） |

---

## 4. 预期成果（含可检验的量化目标）

### 4.1 成果物

| 类别 | 成果 |
|---|---|
| 方法 | TED-O 框架：依赖约束策略空间、无 GPU 对比信用模型、信用门控机制、轨迹双目标度量协议 |
| 系统 | 开源原型（`@Profile("experiment")` 的 `ExperimentRunner` + 埋点 + 注入器，README 可复现） |
| 数据 | 6 个数据集（任务集 / 故障轨迹集 / 信用偏好对 / 检索集 / 数值参照集 / 真实提问集），可公开子集 + 采样表 |
| 论文 | 1 篇方法论文（目标刊物分档见 §7） |

### 4.2 量化预期目标（**假设，需实验验证，非承诺**）

| 编号 | 指标 | 预期 | 参照 |
|---|---|---|---|
| T1 | 相对静态 DAG，质量不降(±2%) 前提下 token 降幅 | **20%–40%** | Puppeteer Fig.2 趋势 |
| T2 | 相对静态 DAG，墙钟降幅 | **15%–30%** | 原方向 B 效率实验 |
| T3 | 故障检测 DetAcc | **≥ 85%** | SentinelNet 85.9%–92.1% |
| T4 | 隔离后质量恢复 Recovery@第2层 | **≥ 85%** | SentinelNet ~95%@第2轮（我们保守） |
| T5 | 无故障误伤 FPR | **≤ 12%** | SentinelNet 8%–13% |
| T6 | 信用门控开销 overhead | **≤ 8%** | SentinelNet 4.59%–5.03% |
| T7 | OOD 故障泛化（未见类型） | 较 in-domain 掉点 **≤ 8pp** | SentinelNet 跨域稳定性 6.2pp |

---

## 5. 评估方式

### 5.1 基线矩阵（相对现有工作）

| 基线 | 实现 | 对齐谁 |
|---|---|---|
| Pure 模型 | 单次 `chatStrict` | Puppeteer Pure Models |
| Self-Refine | 生成→自批→改写 | Puppeteer Self-Refine |
| **静态 DAG（=当前 `CollaborationManager`）** | `computeLevels` + 固定依赖 | MacNet（静态 DAG） |
| 全并行朴素 | 同层全并行、无注入 | 原方向 B 基线 |
| TED-O（查表 / Bandit 两档） | 本文方法 | Puppeteer / 本文 |
| 信用门控四档 | 无防御 / 关键词规则 / LLM-as-judge / TED-O 信用模型 | G-SafeGuard / AgentSafe / SentinelNet |

### 5.2 指标与数据来源

| 维度 | 指标 | 数据来源 |
|---|---|---|
| 质量 | rubric 分、客观题自动判分、LLM-as-judge 成对胜率 | `AssessorAgent` + 人工 rubric + 三源参照 |
| 效率 | tokens/task、墙钟、活跃 Agent 数、链长 | `MeteredLlmProvider` + 轨迹 |
| 结构 | 图密度、环长 1–4 分布 | `GraphMetrics` + LOWESS 趋势（Puppeteer Fig.2/6 做法） |
| 可信 | DetAcc / FPR / FNR / Recovery@层 / 开销% | 故障注入清单 vs 黑名单 + 计时 |
| 演化 | initial vs evolved 同题对比、性能/成本比曲线 | 逐样本在线 JSONL |
| 泛化 | in-domain vs OOD 分栏、跨章域下降率 | 6 类故障 + 9 章域重跑 |

### 5.3 统计与报告口径（学 NeurIPS checklist / SentinelNet Appendix D）

- ≥3 次重复报均值 ± **标准差**，正文声明 SD 还是 SEM；
- 资源表：API 调用次数、tokens、墙钟、并发度、模型单价与折算公式；
- 超参全表：λ、γ、k、p、温度、max_tokens、深/宽；
- 失败与重试次数结构化落盘（`retry_count`、`failure_kind`）。

---

## 6. 相较现有工作的差异化价值（答辩/审稿核心）

| 现有工作 | 与本方案的关系 | 本方案的差异化 |
|---|---|---|
| **Puppeteer（NeurIPS'25）** | 同源（可演化编排） | Puppeteer 假设**无依赖**的通用推理图、只优化效率、不保证质量可信；TED-O 在**领域先验依赖约束**下演化，且**统一效率与可信**双目标。P 自陈局限"欺骗性一致"正是本方案信用层要解决的 |
| **MacNet（ICLR'25，静态 DAG）** | 本方案基线 | MacNet 拓扑**静态**；TED-O 的激活子图/深宽**随任务与反馈演化** |
| **SentinelNet（WWW'26）** | 同源（信用制门控） | SentinelNet 面向通用**多轮辩论**、需 8×H100 + LoRA、恶意攻击假设强；TED-O 面向教育**开放题**、无 GPU 的 embedding-linear 信用模型、三源参照锚定、门控落在**上下文注入**而非辩论轮次。S 自陈局限"n² 开销"正由本方案"只对被激活子集打分"缓解 |
| **AgentSafe / G-SafeGuard** | 防御基线 | 前者提示词式、后者图异常检测；均**集中式/事后**，TED-O 是**分布式信用 + 事前门控**（对齐 SentinelNet 的批判） |
| **LangGraph / AutoGen** | 实证对比对象 | 它们是"框架/工具"，不提供"可度量的编排策略演化 + 信用门控"的方法贡献；本方案与其**直接实证对比**（原方向 B 增量点 4） |
| **原方向 B（静态调度）** | 本文前身 | 从"静态调度算法"升级为"可演化 + 可信门控"的完整方法；补齐了质量与泛化两个空白 |

**核心差异化一句话**：现有工作把"编排效率"和"协作可信"当两个独立问题分别在无依赖通用设定下解决；TED-O 在**教育这一有先验依赖、又缺标准答案的领域**，把两者统一到**同一份轨迹**上协同优化——这是 Puppeteer 与 SentinelNet 各自自陈局限的交集，也是它们都没覆盖的空白。

---

## 7. 目标刊物与风险边界

### 7.1 目标刊物（按难度分档）

| 档位 | 刊物/会议 | 要求 | 匹配度 |
|---|---|---|---|
| 高（冲刺） | AAMAS（应用轨）、IEEE TLT、IJAIED | 全量实验 + 用户效果 + 强对比 + 明确方法创新 | 需 6 个月以上 |
| 中（主攻） | IEEE Access、Applied Intelligence、ICAART/SEKE、中文核心（计算机应用） | TED-O 完整实验（基线+消融+OOD+评测集） | 12–14 周可达 |
| 低（保底） | 教育信息化会议/普刊 | 系统 + 应用案例 | 2–4 周 |

### 7.2 风险与边界（诚实留白，写进 Limitation）

1. **无 GPU/小样本**：不做 8×H100 LoRA、不做全量 RL；明确声明"缩比复现 + 小样本设定"，用 bandit 与 embedding-linear 模型替代，并引用 Puppeteer 自身也只有 200 优化样本的事实。
2. **图密度跨设定不可比**：Agent 数不同时密度不可直接比，报告密度必须并列 V。
3. **依赖扇入需先改**：当前只有 PLANNER→TUTOR→ASSESSOR 单链，故障传播实验依赖 S0 的扇入改造。
4. **"质量"的判定**：开放题无 ground truth，只能三源锚定 + 相对偏好，需报告标注者一致性（Kappa）。
5. **恶意假设强度**：教育场景更常见的是"幻觉/劣质"而非"恶意攻击"，论文需把 threat model 表述为"劣质/故障贡献"而非"恶意节点"，更贴场景也更易通过伦理审。
6. **仿真参照依赖外部服务**：`qigger/python-emulation` 不在本仓库；"数值参照解"只能作为可选实验，主实验以教材 + 题库为锚，避免进度被外部服务卡住。
7. **跨课程泛化需真实文本**：`电路分析基础` 目录是 Visio 图（.vsdx）而非文本语料，无法直接做跨课程 RAG/导学；先以课程内 9 章域多样性替代，跨课程留作 future work 或另备语料。
8. **Java 侧无 ML 库**：`pom.xml` 无矩阵/优化依赖；信用模型训练走 Python 侧（离线），Java 只做点积评分，避免引入重依赖。

---

## 附录 A · 与两份论文的引用定位

| 用到的点 | 原文定位 |
|---|---|
| 动态编排 + 序列化 + 隐式图 | Puppeteer §2.1, Eq.1–4 |
| REINFORCE + 奖励 `r−λ·C`，`C=F·log(1+t/φ)` | Puppeteer §2.2, Eq.5–6 |
| 结构指标（密度/环长/活跃数） | Puppeteer §3.3, Fig.6 |
| 深宽约束非单调 | Puppeteer §3.4, Fig.7（默认 W4D2） |
| 局限"欺骗性一致" | Puppeteer §D |
| 相对信用 + 对比损失 | SentinelNet §4.2, Eq.1–3 |
| bottom-k + 累积黑名单 + 选择性屏障 | SentinelNet §4.3, Algorithm 1 |
| 指标定义 | SentinelNet §5.1.6 |
| 无攻击 FPR 代价 | SentinelNet §5.2.3（0.97–0.98 下 FPR 8–13%） |
| OOD 攻击切分 | SentinelNet §5.1.4 |
| 训练超参（缩比参照） | SentinelNet Appendix D |

## 附录 B · 落地清单（一次性可执行）

1. `llm/MeteredLlmProvider.java` + `LlmService` 暴露 token ledger。
2. `CollaborationManager.recordOutcome()` 结构化 + JSONL 落盘。
3. `TaskDecomposer.buildTaskDependencies()` 加扇入开关。
4. `buildAgentTask()` 加信用过滤开关。
5. 新增 `OrchestratorPolicy` / `DependencyAwareActionSpace` / `CreditModel` / `CreditLedger` / `GraphMetrics`。
6. 新增 `experiment/ExperimentRunner`（`@Profile("experiment")`）+ 6 数据集生成脚本。
7. 故障算子注入器（6 类）+ 三源参照回放器（教材原文/题库答案为主，仿真解可选）。
8. Python 侧：信用模型离线训练脚本（numpy + 本地 bge 向量）+ 权重导出；Java 侧 `CreditModel` 只做点积评分。

---

*本文档中"预期量化目标"为待验证假设；"论文原文事实"均标注章节定位；"本项目适配"为基于当前代码的设计建议。*
