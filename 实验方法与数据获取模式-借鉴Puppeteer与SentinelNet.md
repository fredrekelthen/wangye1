# 两篇参考文献里可迁移到本项目的试验方法与数据获取模式

> 源文档：
> - ① `NeurIPS-2025-multi-agent-collaboration-via-evolving-orchestration-Paper-Conference.pdf`（下称 **P**，Puppeteer / 可演化编排）
> - ② `SentinelNet Safeguarding Multi-Agent Collaboration .pdf`（下称 **S**，信用制威胁检测）
>
> 对象：本项目 `javahouduan`（电磁场课程 LLM 智能学习平台，Spring Boot + 7 个子 Agent + RAG + 会话记忆）
> 目标：只回答一个问题——**这两篇里哪些"做法"和"数据"能搬到本项目里做实验**，并给出落到具体类/方法的改造点、样本量、指标与工期。
> 原则：区分「论文原文事实」与「本项目适配方案」，标注优先级 P0/P1/P2 与人日估算。

---

## 0. 一页结论

### 0.1 两篇各给什么

| | 论文 P（Puppeteer / NeurIPS'25） | 论文 S（SentinelNet / WWW'26） |
|---|---|---|
| 解决什么 | 静态拓扑随任务/Agent 数增长而失效 → 中央编排器按状态逐选 Agent，RL 演化 | 协作链中恶意/劣质 Agent 污染集体结论 → 分布式信用评分 + bottom-k 隔离 |
| 核心机制 | 序列化编排（把图"展开"成序列）→ 隐式推理图；奖励 = 质量 − λ·成本 | 合成对抗轨迹 → 对比学习训练 reward model 打分 → 累积黑名单 + 选择性通信屏障 |
| 实验资产 | 4 数据集 × 2 模型池 × 5 基线；token/Agent 数/图密度/环分布；深度×宽度网格；W4D2 | 6 基准 × 6 攻击（3 in-domain + 3 OOD）；100k+ 合成对；DetAcc/FPR/FNR/Recovery@轮/开销% |
| 自陈局限 | "偶发的**错误协同或欺骗性一致（deceptive agreement）**"（P §D） | "开销随 Agent 数**二次增长**"（S §6） |

### 0.2 为什么这两篇对本项目是"双拼"关系

- P 的自陈局限（欺骗性一致）**正是 S 的靶心**；S 的自陈局限（n² 开销）**正是 P 的靶心**。
- 本项目恰好两件事都缺：**编排是静态的**（关键词分解 + 固定 DAG 分层），**且没有内容质量信号**（`AgentResult.success` 只表示"没抛异常"）。
- 因此本项目最合理的实验骨架是：**用 P 的"轨迹即数据"做效率侧，用 S 的"信用打分 + 隔离"做可信侧，两者共享同一份轨迹日志**。这也天然构成一个差异化选题（见 §8-C）。

### 0.3 最值得抄的三件东西（按性价比排序）

| 排序 | 迁移对象 | 来源 | 为什么值 | 成本 |
|---|---|---|---|---|
| 1 | **轨迹即数据**：每次 `executeTask` 落一条 JSONL，再从轨迹重建成"隐式图"，算密度/环长/活跃 Agent 数/链长 | P §2.2、§3.3、Fig.6 | 不改算法就能产出论文级结构指标；项目已有 `trace` 列表，改造成本最低 | 2~3 人日 |
| 2 | **相对信用评分 + bottom-k 隔离**（不用 GPU：embedding 差值 + Bradley-Terry 逻辑回归，即论文损失的线性化等价形式） | S §4.2、§4.3 | 直接把"好不好"变成"谁比谁好"，规避教育域没有绝对标准答案的死穴；复用现有 `EmbeddingClient` | 6~8 人日 |
| 3 | **合成故障注入 + OOD 切分协议**（训练只见 3 类故障，测试用 3 类未见过故障） | S §4.1、§5.1.4 | 数据可自己造、可公开、可复现；是审稿人最认的"泛化性"证据 | 5~7 人日 |

---

## 1. 本项目"实验就绪度"盘点（决定哪些方法能落）

| 能力 | 代码位置 | 能否支撑实验 | 缺口 |
|---|---|---|---|
| 依赖感知 DAG 分层编排 | `CollaborationManager.computeLevels()` / `depsSatisfied()`（L152–191） | ✅ 静态编排基线现成 | 无"可演化"策略层；层级由关键词分解决定 |
| 子任务分解 | `TaskDecomposer.determineRequiredAgents()`（L68–81，`containsAny` 关键词） | ✅ 可作"朴素策略"基线 | **脆弱**：无法作为论文里的"智能编排" |
| 并行执行 | 固定池 `max(4, cores)`（L43–48） | ✅ 可做"宽度"变量 | 宽度不可按任务动态调 |
| 上游→下游上下文注入 | `buildAgentTask()`（L194–216） | ✅ 是 S 的"选择性通信屏障"天然落点 | **只有 PLANNER→TUTOR→ASSESSOR 一条链**（见下方⚠） |
| 消息通道 | `AgentMessageQueue`（`offer` 投递，无消费方） | ⚠️ 只能当"记录" | 需在注入侧实现过滤，而非队列侧 |
| 共享记忆 | `SharedKnowledgeBase`（内存 Map，`mongoTemplate` 注入未用） | ✅ 可存黑名单/信用分 | 重启即丢，实验需外部落盘 |
| **内容质量信号** | `ResultAggregator.generateSummary()` 只统计 `success` 布尔 | ❌ **最大短板** | 需要 S 的信用评分或 LLM-as-judge |
| **Token 计量** | `LlmCallResult` 有 `promptTokens/completionTokens/totalTokens`；但 `LlmService.chatStrict()` 只返回 `String`（L58–63、L81–89） | ❌ 数据被丢弃 | 需加计量 seam（见 §4.5） |
| 检索打分 | `RAGKnowledgeBaseService`：按字 unigram BM25 + embedding + RRF(k=60)，chunk metadata 写 `bm25Score/rrfScore/score` | ✅ 可算 MRR/Recall@k | 缺"query→gold chunk"标注 |
| **数值参照解** | `PythonEmulationService` 8 个接口（双极子/单极子/GA-PSO-SA-Tabu/入射反射/全折射/全反射/高斯波束/正弦 EH） | ✅ 代码生成正确性的 ground truth | 需接通"生成代码→调用参照"回路 |
| 客观题标准答案 | `AssessorAgent.generateAssessment()` 每题含 `answer` 字段 | ✅ 可自动判分（绝对标签） | 题型覆盖有限（5 题/次） |
| 课程知识点树 | `DataInitializer.createChapter1Sections()` 等（章节/节/子节） | ✅ 任务集生成器 | 代码里只初始化 2 章，需补全 |
| 真实提问分布 | `ConversationService` / `ChatMessage`（Mongo） | ✅ 任务集"真实性"采样源 | 需匿名化 |

> ⚠ **实验前置改造（P0，必做）**：当前 `TaskDecomposer.buildTaskDependencies()` 只声明 `TUTOR←PLANNER`、`ASSESSOR←TUTOR`，其余并行 Agent（QA/RECOMMENDER/MONITOR/EM_CODE）的产出**没有任何下游消费**。这意味着"故障传播"实验只能沿单链做。两种改法：
> - **改法 A（推荐，改动小）**：给 ASSESSOR 增加依赖 `QA`、`EM_CODE`（评估时参考答疑与仿真代码），TUTOR 增加依赖 `RECOMMENDER`（参考推荐资源）。这样依赖图从"单链"变成"2 层扇入"，故障传播与 bottom-k 才有意义。
> - **改法 B**：不改依赖，把实验对象限定在 PLANNER→TUTOR→ASSESSOR 三跳链上，用"3 层 = 3 轮"做轮次实验（见 §3.6）。

---

## 2. 论文 P（Puppeteer）可迁移的试验方法

### 2.1 方法清单与项目映射

| # | 论文做法（定位） | 本项目对应物 | 迁移动作 | 优先级 |
|---|---|---|---|---|
| **M1** | **序列化编排 + 隐式图重建**：每步选 1 个 Agent，把"展开"的序列折回有向图（P §2.1、Fig.4） | `computeLevels()` 产出的是"层"，不是序列；但 `trace` 里有完整执行顺序 | 在 `recordOutcome()` 里为每步记 `(level, agentType, 上游注入来源, 耗时, tokens)`，运行后重建 `V/E`：`E` 取"上下文注入边"+ "依赖声明边" | P0 |
| **M2** | **结构指标**：图密度、环长 1~4 的分布、活跃 Agent 数、链长；初始 1.0829 → 演化后 1.4479（P §3.3、Fig.6） | 图天然存在（7 类 Agent 为节点） | 实现 `GraphMetrics`：`density = E/(V(V-1))`、`cycleCount[len]`、`activeAgents`、`chainLength`。注意：**Agent 数不同不可直接比密度**，须报告 V 与密度并列（P 用同一 8 模型池才可比） | P0 |
| **M3** | **成本感知奖励**：终局 `r − λ·C_T`，中间 `γR_{t+1} − λ·C_t`，`C_t = F·log(1 + t/φ)`（P Eq.6；λ=0.1, γ=0.99） | `tokens`（需先补埋点）+ 层序 `t` + `φ`=最大层数 | 直接实现为"离线打分函数"，先不训练也能用：给定一次协作轨迹，算出 `R`，用于**对比不同编排配置**（这就是表 1 的效费比） | P0 |
| **M4** | **REINFORCE 在线更新策略**（P Eq.5） | 无 GPU、无开源权重、样本量 200 级别 | **不照抄**。三档替代：<br>①（最省）**规则演化**：按历史 `R` 调整"哪种任务激活哪些 Agent"的查表；<br>②**上下文 Bandit**（UCB/Thompson）：动作=Agent 子集，奖励=`R`；<br>③**离线策略重排**：用已采轨迹做 off-policy 评估（IPS）。<br>论文自己也只有 200 个优化样本（P §A.2），所以①②在本项目规模上是**可辩护的等价物** | P1 |
| **M5** | **模型池分层 + Mono/Multi 消融**：Mimas（6 小模型）/Titan（8 大模型）；Mono=全同模型（P §3、Table 1） | 项目已有**两个 provider**：`DeepSeekLlmProvider`（默认 `deepseek-flash`）与 `GlmApiService`→`LlmService` | 定义 `Mono`（全部 `deepseek-flash`）vs `Multi`（部分 Agent 用 `deepseek-v4-pro` / GLM）。这是**几乎零成本**的消融轴，且 P 的结论（异质 Agent 互补更好）可直接检验 | P0 |
| **M6** | **基线矩阵**：Pure / Self-Refine / AFlow / MacNet(静态DAG) / EvoAgent（P §3） | 见 §2.2 映射表 | 至少做 4 条：裸模型、单 Agent 自反思、**当前静态 DAG**、全并行朴素 | P0 |
| **M7** | **深度×宽度网格**：WxD，默认 W4D2，非单调（P §3.4、Fig.7） | 深度=`computeLevels` 层数；宽度=线程池容量/同层 Agent 数 | 网格 `depth∈{2,3,4} × width∈{2,4,6}`，画 accuracy–token 双轴。**注意**：项目只有 3 层自然深度，需靠"人工追加反思层"扩展 | P1 |
| **M8** | **案例可视化 + 跨环境泛化**：链→树→图演化案例（P Fig.5/10）；ALFWorld 具身泛化（P §A.4，链长≤50、1 条探索轨迹、mono GPT-4o-mini） | 可视化：把一次任务的层-边画成图；泛化：仓库内已有 **`电路分析基础/`** 资料 | ①"轨迹图"图例 3~4 张（教育场景的树/扇入/回访）；②**跨课程泛化**：电磁场→电路分析，重跑检索+导学，报告指标掉多少 | P1 |
| **M9** | **资源与可复现报告**：8×A800、28.8–78.4GB、2–6h（P Table 3）+ NeurIPS checklist 要求误差棒口径 | 无 GPU，全部 API | 报告"API 调用次数 / prompt+completion tokens / 墙钟时长 / 并发度 / 单价折算公式"，并**声明误差棒是 SD 还是 SEM**（P checklist 明确要求） | P0 |

### 2.2 基线映射表（论文 → 本项目可执行版本）

| 论文基线 | 在本项目的落地实现 | 实现方式 |
|---|---|---|
| Pure Models | 单次 `llmService.chatStrict(taskContent)` | 新增一个 `BaselineRunner`，不经过 `CollaborationManager` |
| Self-Refine | 同题生成 → 自我批评 → 改写，2 轮 | 复用 `chatStrict` 三次调用 |
| MacNet（静态 DAG） | **即当前 `CollaborationManager`**（`computeLevels` + 固定依赖） | 直接跑，作为"我们的 baseline" |
| （无对应）全并行朴素 | 所有 Agent 同层全并行、无注入 | 加一个 `parallelAll=true` 开关 |
| AFlow / EvoAgent | 可选：用 3 次随机拓扑 + 选最好（随机搜索基线） | 轻量实现，用于说明"演化"确实优于随机 |

### 2.3 由 P 派生的实验设计

| ID | 实验 | 变量 | 样本 | 指标 | 工期 |
|---|---|---|---|---|---|
| E1 | 编排方式对比 | 4 种：裸模型 / 静态DAG / 全并行 / (可选)bandit 演化 | 50 题 × 4 = 200 episode | 质量分、tokens/task、活跃 Agent 数、墙钟 | 4 人日 |
| E2 | Mono vs Multi 模型池 | 2 种 | 30 题 × 2 × 2 重复 | 同上 + 质量方差 | 2 人日 |
| E3 | 深度×宽度网格 | 3×3=9 组 | 20 题 × 9（小样本即可） | accuracy–token 散点、非单调性 | 3 人日 |
| E4 | 结构演化分析 | 配置演化前后（或 bandit 迭代 0/50/100 轮） | E1 产出的轨迹复用 | 密度、环长分布、链长、活跃 Agent 数（配 LOWESS 趋势线，P Fig.2 做法） | 2 人日 |
| E5 | 跨课程泛化 | 电磁场 / 电路分析 | 各 25 题 | 各指标 + 相对下降率 | 3 人日 |

> 数据复用提示：E1 的轨迹可直接喂给 E4（结构指标），E1 的 tokens 可直接喂给 E3（成本轴）。**一次采集，三次分析**，这是 P 最值得学的数据组织方式。

---

## 3. 论文 S（SentinelNet）可迁移的试验方法

### 3.1 故障算子目录（把 S 的 6 种攻击翻译到教育协作场景）

S 的 6 种攻击（S §5.1.4：3 类 in-domain + 3 类 OOD）→ 本项目的**故障注入算子**：

| # | S 的原型 | 在本项目的可执行故障 | 注入点 | 切分 |
|---|---|---|---|---|
| F1 | Collaboration Attack（修辞操纵） | **幻觉公式/错误前提**：TUTOR 给出错误的本构关系或边界条件 | TUTOR prompt 后处理替换 | in-domain（训练） |
| F2 | NetSafe Attack（拓扑利用） | **丢字段/截断**：PLANNER 输出的 `prerequisites` 被清空，下游仍照常消费 | `buildAgentTask()` 注入前删键 | in-domain |
| F3 | AITM Attack（通信拦截） | **上下文污染**：在注入 TUTOR 的上游 context 里混入错误知识点 | `buildAgentTask()` 注入时替换 | in-domain |
| F4 | Prompt Injection | **资料投毒**：往 RAG 语料塞一段与课程相悖的 chunk，使其被召回 | `MaterialChunk` 新增"污染 chunk" | OOD（测试） |
| F5 | PsySafe Attack（认知偏置） | **过度自信/锚定**：TUTOR 反复强调"这就是标准答案"而不给推导 | prompt 风格注入 | OOD（测试） |
| F6 | AutoInject Attack（自适应） | **角色冒充**：MONITOR 输出伪装成 ASSESSOR 的评分结论 | 消息内容伪造 + `agentType` 元数据篡改 | OOD（测试） |

> 关键方法论（S §5.1.4 的 in-domain / OOD 划分）：**只在 F1–F3 上训练/调参，在 F4–F6 上测试**。这是"泛化性"证据的来源，比"同分布提升 3 个点"有说服力得多，也几乎不增加采集成本。

### 3.2 四元组样本构造 → 本项目的数据形态

S 的训练实例是 `x = (c, r_chosen, r_rejected, r_reference)`（S §4.2.1），其中 `c` = 任务描述 + 前序对话摘要。本项目可以**逐字段对上**：

| S 的字段 | 本项目来源 | 备注 |
|---|---|---|
| `c`（context） | `task.getContent()` + `task.getContext()`（上游注入）+ `SharedKnowledgeBase.getRecentConversations()` | 抄 S 的"**压缩摘要**而非全量历史"，避免上下文爆炸 |
| `r_chosen` | 正常 Agent 输出（被判为高质量） | 需要质量标签，见 §3.3 |
| `r_rejected` | 注入 F1–F3 后的输出 | 由故障算子批量生成 |
| `r_reference` | **三源参照**：①教材原文段落（`MaterialChunk`）②`PythonEmulationService` 数值解 ③`AssessorAgent` 题目的 `answer` | 这是本项目比 S 更占优的地方——S 只有 benchmark 答案，我们有三类"权威参照" |

### 3.3 无 GPU 复现 S 的对比学习目标（关键可行性论证）

S 用 Qwen2.5-3B + LoRA（r=64, α=128, dropout 0.05；lr 5e-6，cosine+1000 warmup，5 epoch，8×H100）训练 `R_θ: r⊕c → R`，损失为
`L = L_chosen-rej + α·L_align`，两项都是 `−log σ(R(·) − R(·))` 形式的**成对**目标（S Eq.1–3）。

**这两项对线性打分器是闭式可训的**：若令 `R(r,c) = w·φ(r,c)`，则
- `L_chosen-rej` = Bradley–Terry 逻辑损失 → **sklearn `LogisticRegression` 直接求解**；
- `L_align` = 再加一组成对样本 `(r_chosen, r_reference)`。

因此本项目可用：`φ(r,c) = [Emb(r); Emb(c); Emb(r) − Emb(c)]`（`EmbeddingClient` 现成，GLM embedding-3），在 **CPU 上分钟级**训出一个信用模型。它的论文表述是"**GPU-free, embedding-linear instantiation of the contrastive credit model**"，属于合理缩比复现而非降级——因为 S 的两个损失项本质就是成对排序。

同时必须报告一个**弱基线**做对照：zero-shot LLM-as-judge（直接让 `deepseek-flash` 打分）。这既是最强的现实竞争者，也是审稿人一定会问的。

### 3.4 部署机制 → 项目里的三个落点

S 的 `Algorithm 1`：每轮算分 → 升序排序 → 取最低 k 个 → 并入**累积黑名单** → 过滤其消息 → 用过滤后上下文继续。

| S 的步骤 | 本项目落点 | 实现要点 |
|---|---|---|
| 打分 `s_i = R_θ(c_t, r_i)` | `recordOutcome()` 之后，对同层所有 Agent 结果打分 | 同层并行 Agent（PLANNER/QA/RECOMMENDER/MONITOR/EM_CODE）正好是"同一轮" |
| bottom-k 选择 | `k = max(1, ⌊n·p⌋)`，p 取 0.2/0.3 做扫描 | k 是超参，S 未给固定值，需报告敏感性 |
| 累积黑名单 | 新增 `CreditLedger`（放 `SharedKnowledgeBase` 或 Mongo） | 键 = `agentType`，值 = 累计低分次数；**跨任务累积**才叫"累积" |
| 选择性通信屏障 | `buildAgentTask()` 里跳过黑名单 Agent 的 context 注入 | **注意**：这是"过滤消息"而不是"下线 Agent"，S 明确说保留拓扑、保留多样性（S §4.3.2） |
| 上下文压缩摘要 | `buildRagContext` / `task.getContext()` 拼接处加摘要 | 抄 S 的 `c` 构造方式 |

> 与项目现状的契合点：`AgentMessageQueue` 目前"只投不取"，正好可以把它改造成**信用过滤的旁路记录**——投递时附带 `credit`，注入时按 credit 阈值过滤。这样既没浪费既有机制，也复用了 S 的语义。

### 3.5 评测指标（S §5.1.6）→ 本项目可计算版本

| S 的指标 | 本项目定义 | 数据来源 |
|---|---|---|
| Task Accuracy per round | 每个 DAG 层级结束时的"当前最佳答案"质量（rubric 分 / 客观题得分） | `AssessorAgent` 自动判分 + rubric |
| Detection Accuracy | 故障 Agent 被正确标记（进黑名单）的比例 | 故障注入清单 vs 黑名单 |
| FPR | 正常 Agent 被误标比例 | 同上 |
| FNR | 故障 Agent 漏标比例 | 同上 |
| Recovery@round | 第 1/2 轮隔离后质量恢复比例（S：~80% → ~95%） | 逐层质量序列 |
| Detection Time | 单次打分耗时（S：1.23–1.52 s/轮） | 埋点计时 |
| Overhead % | `(加检测墙钟 − 不加检测墙钟)/不加检测`（S：4.59–5.03%） | A/B 两跑 |

### 3.6 "轮次"语义的本地化（重要）

S 是**多轮辩论**；本项目是**单遍 DAG**。三种可辩护映射，建议至少声明一种：

1. **层=轮（推荐）**：`level 1 → level 2 → level 3`，每层结束做一次 bottom-k 隔离，恰好 3 轮，足以展示 S 的"2 轮内接近饱和"结论。
2. **重复执行=轮**：同题跑 N 次，每次带累积黑名单，观察第 N 次的质量。适合验证"累积"机制。
3. **新增反思轮**：在 ASSESSOR 后加一轮"批评→修订"，形成真正的迭代。改动最大，但最接近 S 的语义，也顺带填掉项目"消息未被消费"的留白。

### 3.7 由 S 派生的实验设计

| ID | 实验 | 变量 | 样本 | 指标 | 工期 |
|---|---|---|---|---|---|
| E6 | 故障检测与隔离 | 6 类故障 × {无防御, 关键词规则, LLM-as-judge, 本项目信用模型} | 每类 30 episode | DetAcc / FPR / FNR / Recovery@1,2,3 | 6 人日 |
| E7 | OOD 泛化 | 训练 F1–F3，测试 F4–F6 | 3 类 × 30 | 同上（分 in-domain/OOD 两栏） | 3 人日 |
| E8 | 无故障代价 | 无故障 × {有/无信用门控} | 20 episode | 质量损失、FPR、误隔离好 Agent 数（S 的 8–13% FPR 对照） | 2 人日 |
| E9 | 开销测量 | 加/不加检测 | 复用 E6 | 检测耗时、总墙钟、overhead% | 1 人日 |

---

## 4. 实验数据与数据获取模式（两篇合并）

### 4.1 两篇共同的五条数据哲学

| # | 哲学 | P 的做法 | S 的做法 | 本项目怎么落 |
|---|---|---|---|---|
| 1 | **合成优先**（不等真实数据） | 合成 200 条优化样本逐步演化 | 合成 100,000+ 对抗轨迹 | 用脚本批量跑 `CollaborationManager`，模板化出题 |
| 2 | **逐样本在线曲线**，不只报终值 | Fig.2 token/Agent 数随训练下降，LOWESS 拟合 | 逐轮 accuracy 曲线（Fig.3） | 每个 episode 落一行 JSONL，画 learning curve |
| 3 | **相对优于绝对** | 用"演化前 vs 演化后"同题对比 | 用成对偏好替代绝对打分 | 同一任务多配置对照 = 天然成对样本 |
| 4 | **参照锚定** | 有 ground-truth 的 GSM-Hard/MMLU-Pro 用 `r∈{0,1}` | `r_reference` 锚定事实正确性 | 教材原文 + 仿真数值解 + 题库答案三源锚定 |
| 5 | **报告口径显式化** | 资源表 + 误差棒口径 | 训练超参全表 + 攻击/防御基线定义 | 见 §4.7 |

### 4.2 六个数据集（本项目可产出的数据资产）

| ID | 数据集 | 构造方式 | 规模建议 | 用途 |
|---|---|---|---|---|
| **T1** | 编排任务集 | 从 `DataInitializer` 知识点树 + `ConversationService` 真实提问采样，人工/规则标注"需要哪些 Agent" | 50~80 题（覆盖 2 章 × 难度） | E1/E3/E5 |
| **T2** | 故障注入轨迹集 | T1 × 6 类故障算子，每组合跑 1 次，落完整轨迹 | 50 × 6 ≈ 300 episode | E6/E7 |
| **T3** | 信用偏好对 | 从 T2 派生：同 context 下 {正常输出, 故障输出, 参照} → 2 对/实例 | ~600 对（可扩到 2000） | 训练信用模型 |
| **T4** | 检索评测集 | 教材/资料 → 人工标 `query → gold chunk` | 100~150 对 | RAG 侧 MRR/Recall（与 E1 解耦） |
| **T5** | 数值参照集 | 电磁仿真题 → `PythonEmulationService` 参考结果 + 允许误差 | 30~50 题 | 代码生成正确性（EM_CODE） |
| **T6** | 真实提问集（可选） | Mongo 会话导出 + 匿名化 | 100~200 条 | 分布真实性论证 / 应用型实验 |

> 数据体量对照：S 用 100,000+ 对训练；本项目 T3 只有数百对。**必须在论文里显式声明"小样本设定"**，并引用 S 的"数据稀缺 → 合成"论证来支撑，而不是假装同规模可比。S 自己在 §5.1.2 也按场景报了 1000/800/700/360/200 这种精确到百的样本数——**我们也应逐个数据集报精确数**，这是可复现性的最低要求。

### 4.3 数据获取流水线（5 步）

```
① 出题      T1 生成器：知识点 → 任务文本（模板 + LLM 改写 + 人工抽检 20%）
② 执行      实验驱动：HTTP 调 /api/multi-agent/task（或直接 Java 调用），带 config 标签
③ 采集      埋点：每 agent 一步 → tokens/latency/state；每任务 → trace/图/结果
④ 标注      (a) 客观题自动判分 (b) rubric 打分 (c) 成对偏好（同题两配置）
⑤ 切分      按"故障类别 in/out" + "课程域" 双维度切分，固定随机种子
```

驱动方式建议：**不要**通过前端点，写一个 `ExperimentRunner`（独立 profile，`@Profile("experiment")`），内部 `Map<String,Object>` 直接构造 `Task`，避免 Controller 层序列化开销，也便于并发控制。

### 4.4 记录 schema（JSONL，三类）

**A. 轨迹记录（一个任务一行，喂 E1/E4）**

```json
{"episode_id":"e0001","task_id":"...","variant":"static-dag",
 "task":{"content":"讲讲坡印廷矢量","domain":"电磁场","chapters":["1.3"]},
 "config":{"model_pool":"mono","max_depth":3,"width":4,"lambda":0.1,"faults":[]},
 "steps":[{"level":1,"agent":"PLANNER","latency_ms":2310,"prompt_tokens":980,
           "completion_tokens":610,"success":true,"credit":0.82,"injected_from":[]}],
 "graph":{"nodes":["PLANNER","QA","RECOMMENDER","TUTOR","ASSESSOR"],
          "edges":[["PLANNER","TUTOR"],["TUTOR","ASSESSOR"]],
          "density":0.20,"cycles":{"1":0,"2":0,"3":0,"4":0},
          "active_agents":5,"chain_length":3},
 "outcome":{"quality":0.78,"judge":"rubric","tokens_total":8420,"wall_ms":11300,
            "recovered":null,"fault_set":[]}}
```

**B. 信用训练对（一对偏好一行，喂 T3/E6）**

```json
{"pair_id":"p0001","context_id":"c0007","fault_type":"F1_hallucinated_formula",
 "context_summary":"任务：...；上游 PLANNER 前置知识：...",
 "r_chosen":"...","r_rejected":"...","r_reference":"教材 2.3 节原文：...",
 "source":"textbook|simulation|quiz"}
```

**C. 检索记录（一 query 一行，喂 T4）**

```json
{"qid":"q0001","query":"趋肤深度公式","gold_chunk_ids":["..."],
 "retrieved":[{"id":"...","rank":1,"bm25":4.21,"vector":0.83,"rrf":0.032}],
 "k":5}
```

> 关键：三份 schema 都带 `config` 或 `source`，**同一个 episode 可以被多个实验复用**。这是 P 的"一次采集、多面分析"模式，也是控制 API 成本的核心手段。

### 4.5 埋点改造清单（按文件）

| 文件 | 改动 | 目的 | 优先级 |
|---|---|---|---|
| 新增 `llm/MeteredLlmProvider.java` | 装饰 `LlmProvider`，把 `LlmCallResult` 的 tokens 记入 `ThreadLocal<TokenLedger>` | 不碰任何 Agent 代码就拿到 token | P0 |
| `service/LlmService.java` | 增 `chatStrictResult(...)` 返回 `LlmCallResult`（或暴露 ledger 读取口） | 让 token 可达 | P0 |
| `agent/CollaborationManager.java` | `recordOutcome()` 追加 step 级 JSON；末尾 `trace` 转结构化（现在是 List<String>） | 轨迹数据 | P0 |
| `agent/CollaborationManager.java` | 新增 `buildAgentTask()` 的信用过滤开关（默认关，实验开） | S 的通信屏障 | P0 |
| 新增 `agent/CreditLedger.java` | 累积黑名单 + 信用分；落 Mongo（`mongoTemplate` 已注入未用） | S 的累积机制 | P1 |
| 新增 `agent/GraphMetrics.java` | 密度/环长/链长计算 | P 的结构指标 | P1 |
| 新增 `experiment/ExperimentRunner.java` | 批量驱动 + JSONL 落盘 + 并发限流 | 数据采集 | P0 |
| `agent/TaskDecomposer.java` | 追加 `buildTaskDependencies()` 的扇入版本（开关控制） | 让故障可传播（§1 的 ⚠） | P0 |

**Token 计量为什么必须要**：P 的核心结论是"性能提升不以 token 为代价"（Fig.2），S 的核心结论之一是"开销仅 4.59–5.03%"。**没有 token 与墙钟数据，这两个结论都无法主张**，而项目现在恰恰把 `LlmCallResult` 里的用量丢掉了。这是投入产出比最高的一个改造（约 1 人日）。

### 4.6 规模与成本估算（用于排期与预算）

单任务 token 粗估（7 Agent 全激活，prompt≈1200 / completion≈800 每 Agent）：**≈ 1.4 万 tokens/任务**。

| 实验 | episode 数 | tokens（估） | 备注 |
|---|---|---|---|
| E1 编排对比 | 200 | ≈ 280 万 | 主线，不可省 |
| E2 Mono/Multi | 120 | ≈ 170 万 | 30 题 × 2 × 2 |
| E3 深宽网格 | 180 | ≈ 250 万 | 20 题 × 9 |
| E6+E7 故障实验 | 360 | ≈ 500 万 | 60 题 × 6 类 |
| E8 无故障代价 | 40 | ≈ 56 万 | |
| E9 开销 | 复用 E6 | 0 | 只测墙钟 |
| **合计** | **≈ 900** | **≈ 1250 万** | 不含重试与失败调用 |

**控本四招**（均来自两篇的实践方式）：
1. **缓存去重**：同 `(prompt 模板, 输入)` 命中直接复用（P 的 `F`/`E3` 分析都是离线重算的）。
2. **小样本 + 种子重复**：主表 50 题 ×1 次，稳健性用 20 题 ×3 次，而不是全量 ×3。
3. **分层用模型**：质量判定/评审用 `deepseek-v4-pro`，执行用 `deepseek-flash`（对应 S 的"攻击者用更大模型"设定，见 §3.1 注）。
4. **温度固定 + 记录**：项目各 Agent 温度不同（如 ASSESSOR 0.6、feedback 0.7），**必须把每次调用的 temperature/max_tokens 记进 JSONL**，否则实验不可复现。

### 4.7 统计与报告规范（学 NeurIPS checklist 的硬要求）

- **误差棒**：报告 ≥3 次重复的均值 ± 标准差，并**在正文声明是 SD 还是 SEM**（P 的 checklist 明确点名此坑）。
- **非对称分布**不要画对称误差棒（如成功率接近 1 时）。
- **资源表**：API 调用次数、tokens、墙钟、并发度、模型单价与折算公式（对齐 P 的 Table 3）。
- **超参全表**：λ、γ、k、p、温度、max_tokens、深度/宽度（对齐 S 的 Appendix D）。
- **失败也算数据**：`AgentResult.failure` 与重试次数必须落盘——项目 `runWithRetry()` 已有重试逻辑，但结果被吞进 message 字符串，需结构化（`retry_count`、`failure_kind`）。

---

## 5. 合并实验矩阵（总表）

| ID | 目的 | 主要来源 | 关键变量 | 样本 | 主指标 | 优先级 | 人日 |
|---|---|---|---|---|---|---|---|
| E0 | 埋点与驱动（不含科学结论） | P Fig.2 前提 / S Table1 前提 | — | — | 数据完整性 | **P0** | 3 |
| E1 | 编排方式主对比 | P Table1 范式 | 4 种编排 | 200 ep | 质量、tokens、墙钟 | **P0** | 4 |
| E4 | 结构演化分析 | P Fig.6 | 配置演化档位 | 复用 E1 | 密度、环长、链长 | **P0** | 2 |
| E6 | 故障检测与隔离 | S Fig.3/4 + Table1 | 4 种防御 | 360 ep | DetAcc/FPR/FNR/Recovery | **P0** | 6 |
| E7 | OOD 故障泛化 | S §5.1.4 | in-domain vs OOD | 复用 E6 | 同上（分栏） | **P0** | 3 |
| E8 | 无故障代价 | S §5.2.3 | 门控开/关 | 40 ep | 质量损失、误隔离率 | P1 | 2 |
| E9 | 计算开销 | S Table1 | 检测开/关 | 复用 E6 | 耗时、overhead% | P1 | 1 |
| E2 | 模型池消融 | P Table1 | Mono/Multi | 120 ep | 质量、方差 | P1 | 2 |
| E3 | 深宽网格 | P Fig.7 | 9 组合 | 180 ep | accuracy–token 前沿 | P1 | 3 |
| E5 | 跨课程泛化 | P §A.4 | 电磁场/电路 | 50 题 | 相对下降 | P2 | 3 |
| E10 | 弱化版"演化"（bandit/查表） | P §2.2 | 迭代轮次 | 复用 E1 | 质量–成本比曲线 | P2 | 5 |

**最小可发表包（P0 五项）≈ 18 人日**，加 P1 约 26 人日，与既有 14 周规划的阶段 1（6 周）量级吻合。

---

## 6. 对既有 14 周规划的增量改动

| 阶段 | 原计划 | 建议新增 |
|---|---|---|
| 阶段 0（实验基建） | 埋点、数据集、脚本 | ①`MeteredLlmProvider`（token）②`ExperimentRunner` + JSONL schema ③`TaskDecomposer` 扇入开关 ④`GraphMetrics` |
| 阶段 1（核心实验） | 方向 A/B/C 各自实验 | 把 E1/E4/E6/E7 提到最前（共享轨迹，边际成本最低），E2/E3 用 E1 复用数据 |
| 阶段 2（深度实验） | 用户效果、框架对比 | 增加"OOD 故障泛化"与"无故障代价"两项——这是审稿人最认、而原计划完全没有的实验 |
| 阶段 3（写作） | 方法+实验 | 单独一节 **"数据获取与可复现性"**（对齐 P Table3 / S Appendix D），附 JSONL schema 与采样表 |

---

## 7. 不能直接迁移的部分（诚实边界）

| 论文设定 | 为什么不适用 | 处理建议 |
|---|---|---|
| 8×H100/A800 + LoRA 微调 reward model | 本项目无 GPU 预算、无开源权重 | 用 §3.3 的 embedding 线性信用模型（CPU 分钟级），并明确标注"缩比复现" |
| REINFORCE 在线 RL | 样本量数百，在线梯度更新不现实；且策略动作空间小 | 用 bandit / 离线策略评估替代，并在 Limitation 说明 |
| 多轮辩论（6 轮 / 5 轮） | 项目是单遍 DAG，无辩论循环 | 声明的轮次映射（§3.6），或新增一轮"批评-修订" |
| 100,000+ 训练对 | 规模差 2 个数量级 | 报精确小样本 + 引用 S 的数据稀缺论证 + 人工抽检说明质量 |
| 图密度跨设定比较 | 节点数不同则密度不可比（P 用固定 8 模型池才可比） | 密度必须与 V 并列报告；跨实验只比"同 V"的档位 |
| 绝对 ground truth（`r∈{0,1}`） | 教育域开放题无唯一答案 | 三源锚定（教材/仿真/题库）+ 成对偏好，并报告标注者一致性（Kappa） |
| 恶意 Agent 可被"下线" | 项目 Agent 是服务单例，不能按任务下线 | 采纳 S 的"选择性通信屏障"而非移除，正好符合 |
| 集中式 orchestrator 替换 DAG | 会丢掉项目已有的依赖语义（教学依赖规划） | 不替换，而是"外层选择子图 + 内层保留 DAG"，作为混合方案 |

---

## 8. 三种可选的论文包装（用哪篇当骨架）

| 方案 | 骨架 | 一句话卖点 | 需要的最少实验 | 风险 |
|---|---|---|---|---|
| A. 效率向 | P | "教育导学任务的依赖感知编排：静态 DAG vs 可演化选择的效费比" | E0+E1+E3+E4 | 与 P 太像，创新性被质疑 |
| B. 可信向 | S | "教育多智能体协作中的信用制上下文门控：无 GPU 的对比信用模型" | E0+E6+E7+E8 | 需要故障注入的合理性论证 |
| **C. 合并向（推荐）** | P+S | "**可演化编排 × 信用门控：一份轨迹同时度量协作的效率与可信度**" | E0+E1+E4+E6+E7+E8 | 工作量最大，但正好落在两者的自陈局限交集上 |

方案 C 的论证链（两篇原文可直接引用）：
1. P 说静态拓扑在 Agent 数增长时失效 → 本项目静态 DAG 是同一病灶；
2. P 的局限是"欺骗性一致"，S 的靶心正是它 → 编排解决"叫谁"，信用解决"信谁"；
3. S 的局限是"n² 开销"，P 的靶心正是它 → 信用只对"被编排激活的子集"打分，可把 S 的 n² 降到激活数 m²；
4. 两者共用同一份轨迹日志 → **单一采集成本，双重指标产出**，这正是本项目在资源受限下的最优策略。

---

## 附录 A · 原文定位速查

| 主题 | 位置 |
|---|---|
| P：序列化编排与 Markov 形式化 | P §2.1, Eq.1–4 |
| P：REINFORCE 与奖励设计 | P §2.2, Eq.5–6 |
| P：数据集与基线 | P §3（GSM-Hard/MMLU-Pro/SRDD/CommonGen-Hard；Pure/Self-Refine/AFlow/MacNet/EvoAgent） |
| P：token 与 Agent 数演化 | P §3.2, Fig.2；§A.1, Fig.8 |
| P：压缩与环化（密度/环长） | P §3.3, Fig.6（1.0829→1.4479；环长分布） |
| P：深度×宽度非单调 | P §3.4, Fig.7（默认 W4D2） |
| P：超参与资源 | P §3（λ=0.1, γ=0.99, episode 4, 探索 3）；§C Table3（8×A800, 28.8–78.4GB, 2–6h） |
| P：具身泛化 | P §A.4（ALFWorld，链长≤50，1 条轨迹，GPT-4o-mini） |
| P：局限（欺骗性一致） | P §D |
| S：攻击场景与数据规模 | S §4.1；§5.1.2（100k+ 对；1000/800/700/360/200 分场景） |
| S：四元组与对比损失 | S §4.2.1–4.2.2, Eq.1–3 |
| S：运行时打分与 bottom-k | S §4.3.1–4.3.2, Algorithm 1 |
| S：指标定义 | S §5.1.6 |
| S：主结果（恢复率/FPR/FNR） | S §5.2.1–5.2.4, Fig.3–4, Table1 |
| S：无攻击代价 | S §5.2.3（0.97–0.98 准确率下 FPR 8–13%） |
| S：训练超参 | S Appendix D（LoRA r=64/α=128/dropout 0.05；lr 5e-6；8×H100） |
| S：局限（n² 开销） | S §6 |

## 附录 B · 落地清单（按文件，一次性可执行）

1. `llm/MeteredLlmProvider.java`（新增）+ `LlmService` 暴露 ledger → token 可达。
2. `agent/CollaborationManager.recordOutcome()` → 结构化 step 记录 + JSONL 落盘。
3. `agent/TaskDecomposer.buildTaskDependencies()` → 扇入开关（ASSESSOR←QA/EM_CODE，TUTOR←RECOMMENDER）。
4. `agent/CollaborationManager.buildAgentTask()` → 信用过滤开关。
5. `agent/CreditLedger.java`（新增）+ Mongo 持久化。
6. `agent/GraphMetrics.java`（新增）。
7. `experiment/ExperimentRunner.java`（新增，`@Profile("experiment")`）+ 6 个数据集生成脚本。
8. `python` 侧：故障算子注入器（6 类）+ 参照解回放器（调 `PythonEmulationService`）。

---

*本文档中"论文原文事实"均标注了章节/图表定位；"本项目适配方案"为基于代码现状的设计建议，未经实验验证。*
