package com.edu.backend.agent;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 编程意图检测器：用于多智能体协作中"是否需要激活代码生成智能体"的决策。
 *
 * 设计要点：
 *  - hasProgrammingIntent：通过关键词识别用户是否具备"明确的编程/代码生成意图"。
 *  - isEMFieldDomain：识别需求是否属于电磁场仿真领域，作为严格领域限制的前置闸门。
 *  - detect：综合两者给出是否应当激活 EM_CODE 智能体的决策结果。
 *
 * 注意：isEMFieldDomain 仅做轻量关键词匹配（无 LLM 调用），因此 EMCodeAgent
 * 在 processTask 中可先调用它做零成本领域闸门，未命中时直接返回拒识结果，避免无效推理。
 */
@Service
public class ProgrammingIntentDetector {

    /** 明确的"代码生成"意图关键词（命中其一即视为有编程意图） */
    private static final Set<String> CODE_INTENT = Set.of(
            "生成代码", "写代码", "编写代码", "代码", "编程", "脚本",
            "python", "程序", "code", "script", "实现代码", "仿真代码", "代码实现",
            "可视化代码", "仿真程序"
    );

    /** 电磁场仿真领域关键词（命中其一即视为电磁领域需求） */
    private static final Set<String> EM_DOMAIN = Set.of(
            "电磁场", "电磁波", "电磁", "天线", "辐射", "传播", "麦克斯韦", "maxwell",
            "微波", "射频", "雷达", "antenna", "electromagnetic", "radiation", "em wave",
            "dipole", "偶极子", "波导", "谐振", "远场", "近场", "极化", "增益", "方向图",
            "场强", "s参数", "反射", "折射"
    );

    public boolean hasProgrammingIntent(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String kw : CODE_INTENT) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    public boolean isEMFieldDomain(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String kw : EM_DOMAIN) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    public IntentDecision detect(String text) {
        boolean intent = hasProgrammingIntent(text);
        boolean em = isEMFieldDomain(text);
        return new IntentDecision(intent, em, intent && em);
    }

    /** 意图决策结果 */
    public static class IntentDecision {
        private final boolean programmingIntent;
        private final boolean emDomain;
        private final boolean activateCodeAgent;

        public IntentDecision(boolean programmingIntent, boolean emDomain, boolean activateCodeAgent) {
            this.programmingIntent = programmingIntent;
            this.emDomain = emDomain;
            this.activateCodeAgent = activateCodeAgent;
        }

        public boolean isProgrammingIntent() { return programmingIntent; }
        public boolean isEmDomain() { return emDomain; }
        public boolean isActivateCodeAgent() { return activateCodeAgent; }
    }
}
