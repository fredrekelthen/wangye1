package com.edu.backend.agent;

import com.edu.backend.service.GlmApiService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * 电磁场仿真代码生成专用智能体（EM_CODE）。
 *
 * 职责：
 *  1. 当上游 TaskDecomposer 检测到"明确的编程意图"时，被自动纳入协作链路（自动激活）。
 *  2. 严格领域限制：仅生成与电磁场模拟相关的 Python 代码；非电磁领域需求直接拒识（零 LLM 成本）。
 *  3. 代码生成策略：聚焦"天线辐射特性"与"电磁波传播"两类仿真，要求向量化（高效）、
 *     命名清晰且带中文注释（可读）、含参数配置区/仿真逻辑区/可视化区（结构化完整代码），
 *     并避免无关解释性文字。
 *
 * 领域闸门使用 ProgrammingIntentDetector.isEMFieldDomain 做关键词快速判定，
 * 仅当命中电磁领域时才调用大模型生成代码，从而兼顾"自动激活"与"严格领域约束"。
 */
@Service
public class EMCodeAgent implements BaseAgent {

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是一名计算电磁学（Computational Electromagnetics）专家级 Python 代码生成器，专注于电磁场数值仿真。",
            "",
            "【领域约束】你生成的代码必须且只能与\"电磁场模拟\"相关，重点覆盖两类任务：",
            "1) 天线辐射特性：远场/近场辐射方向图、方向性系数、增益、半功率波束宽度、阵列因子、偶极子/贴片/喇叭等天线的场分布。",
            "2) 电磁波传播：平面波传播、反射/折射、自由空间/双径/多径路径损耗、菲涅尔区、极化、波导与谐振腔模式。",
            "",
            "【代码质量要求】",
            "- 语言：仅 Python 3，使用 numpy 做向量化计算、scipy 做数值求解、matplotlib 做可视化，必要时用 scipy.constants 取 c、μ0、ε0。",
            "- 效率：必须用向量化（np.meshgrid、广播、矢量化公式）替代显式 for 循环；对角度/空间网格批量计算；避免 O(n^2) 的 Python 级循环。",
            "- 可读性：变量命名语义清晰（如 wavelength、k、theta、E_theta），关键物理步骤附中文注释，常量集中定义为配置区。",
            "- 结构：代码顶部为\"参数配置区\"（频率/波长、介质参数、网格分辨率、天线几何等可调整参数），中部为\"仿真逻辑区\"，尾部为\"结果可视化/输出区\"。",
            "- 物理正确性：使用正确的电磁公式与单位（GHz→Hz、cm→m 的统一换算），波数 k=2π/λ，远场近似 r≫λ。",
            "",
            "【输出格式】只输出一个被 ```python 和 ``` 包裹的完整、可直接运行的代码块。不要输出任何额外的解释性文字、不要输出 Markdown 列表或项目说明。代码块内部如需说明，请使用 Python 注释。"
    );

    private final GlmApiService glmApiService;
    private final AgentRegistry agentRegistry;
    private final ProgrammingIntentDetector intentDetector;
    private AgentState currentState = AgentState.IDLE;

    public EMCodeAgent(GlmApiService glmApiService, AgentRegistry agentRegistry,
                       ProgrammingIntentDetector intentDetector) {
        this.glmApiService = glmApiService;
        this.agentRegistry = agentRegistry;
        this.intentDetector = intentDetector;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.EM_CODE, this);
    }

    @Override
    public AgentType getAgentType() { return AgentType.EM_CODE; }

    @Override
    public List<String> getCapabilities() {
        return List.of("电磁场仿真代码生成智能体：依据自然语言需求自动生成面向电磁场模拟（天线辐射特性、电磁波传播）的 Python 代码，"
                + "严格限定领域，输出含参数配置与仿真逻辑的结构化代码。");
    }

    @Override
    public AgentState getCurrentState() { return currentState; }

    @Override
    public void updateState(AgentState state) { this.currentState = state; }

    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        long startTime = System.currentTimeMillis();
        try {
            String content = task.getContent() != null ? task.getContent() : "";
            if (content.isBlank()) {
                return AgentResult.failure("需求描述为空，无法生成代码");
            }

            // 严格领域限制：零成本领域闸门，未命中电磁领域直接拒识，避免无效推理
            if (!intentDetector.isEMFieldDomain(content)) {
                Map<String, Object> refusal = new LinkedHashMap<>();
                refusal.put("generated", false);
                refusal.put("domain", "electromagnetic_simulation");
                refusal.put("message", "本智能体仅生成与电磁场模拟（天线辐射特性、电磁波传播等）相关的 Python 代码。"
                        + "当前需求未识别为电磁场仿真任务，已拒绝生成。");
                AgentResult r = AgentResult.success(refusal);
                r.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                return r;
            }

            String code = generateEMCode(content);
            if (code == null || code.isBlank()) {
                return AgentResult.failure("未能从模型响应中提取有效 Python 代码");
            }

            List<String> deps = extractDependencies(code);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("generated", true);
            data.put("language", "python");
            data.put("domain", "electromagnetic_simulation");
            data.put("description", content);
            data.put("dependencies", deps);
            data.put("scriptName", "em_simulation.py");
            data.put("code", code);
            if (!deps.isEmpty()) {
                data.put("executionHint", "pip install " + String.join(" ", deps) + " && python em_simulation.py");
            }

            AgentResult r = AgentResult.success(data);
            r.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return r;

        } catch (Exception e) {
            return AgentResult.failure("电磁仿真代码智能体执行失败: " + e.getMessage());
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    private String generateEMCode(String requirement) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", requirement + "\n\n请基于上述需求生成完整、可直接运行的电磁场仿真 Python 代码。");
        messages.add(user);

        // 代码生成属长任务：推理模型的思考与完整代码共享额度，
        // 2000 容易被思考耗尽导致返回空，故提高到 8000
        String response = glmApiService.chatWithHistory(messages, 0.3, 8000);
        if (response == null
                || response.contains("GLM API")
                || response.contains("通信时发生错误")
                || response.contains("响应中无 content")) {
            return null;
        }
        return extractCode(response);
    }

    /** 从模型响应中提取被 ```python ... ``` 或 ``` ... ``` 包裹的代码块 */
    private String extractCode(String response) {
        Matcher m = Pattern.compile("```(?:python)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE).matcher(response);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /** 从 import / from ... import 语句中提取第三方依赖（去重、保留首段包名） */
    private List<String> extractDependencies(String code) {
        Set<String> deps = new LinkedHashSet<>();
        for (String line : code.split("\n")) {
            line = line.trim();
            Matcher m1 = Pattern.compile("^import\\s+([a-zA-Z0-9_\\.]+)").matcher(line);
            if (m1.find()) { deps.add(m1.group(1).split("\\.")[0]); continue; }
            Matcher m2 = Pattern.compile("^from\\s+([a-zA-Z0-9_\\.]+)\\s+import").matcher(line);
            if (m2.find()) { deps.add(m2.group(1).split("\\.")[0]); }
        }
        return new ArrayList<>(deps);
    }

    public String getAgentName() { return "电磁仿真代码智能体"; }
    public String getAgentDescription() { return "依据自然语言需求生成电磁场仿真 Python 代码"; }
}
