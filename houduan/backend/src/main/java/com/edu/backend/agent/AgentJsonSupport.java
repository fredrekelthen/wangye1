package com.edu.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 结构化输出的解析工具。
 *
 * <p><b>背景</b>：原先各 Agent 通过"一次只问一个小问题、连续调用模型 5~7 次"来组装结果，
 * 时延与成本都很高（每次调用都包含完整的推理开销），且任一次失败都会让整块内容缺失。
 * 改为"单次调用返回 JSON"后，需要稳健地从模型响应中提取 JSON。</p>
 *
 * <p>模型常见的不规范输出（前后夹带说明文字、用 ```json 包裹、单元素不套数组等）
 * 都由本类统一兼容；解析失败时返回空容器，由调用方用默认值兜底，
 * 保证返回结构始终完整。</p>
 */
final class AgentJsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentJsonSupport() {
    }

    /** 从模型响应中提取 JSON 对象；失败返回空 Map。 */
    static Map<String, Object> parseObject(String raw) {
        JsonNode node = locate(raw, '{', '}');
        if (node == null || !node.isObject()) {
            return new LinkedHashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = MAPPER.convertValue(node, Map.class);
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /** 从模型响应中提取 JSON 对象数组；失败返回空 List。 */
    static List<Map<String, Object>> parseObjectArray(String raw) {
        JsonNode node = locate(raw, '[', ']');
        List<Map<String, Object>> items = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return items;
        }
        for (JsonNode child : node) {
            if (child.isObject()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> one = MAPPER.convertValue(child, Map.class);
                    if (one != null) {
                        items.add(one);
                    }
                } catch (Exception ignored) {
                    // 单个元素异常不影响其余元素
                }
            }
        }
        return items;
    }

    /** 取字符串字段，缺失或为空时返回默认值。 */
    static String asString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    /** 取正整数（兼容模型返回 "5" 这类字符串）；非法时返回默认值。 */
    static int asInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            int parsed = number.intValue();
            return parsed > 0 ? parsed : defaultValue;
        }
        if (value != null) {
            String digits = String.valueOf(value).replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(digits);
                    if (parsed > 0) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // 落到默认值
                }
            }
        }
        return defaultValue;
    }

    /**
     * 把 JSON 值规整为字符串列表。
     * 兼容三种形态：数组、单个字符串（按行拆）、带编号的整段文本。
     */
    static List<String> asStringList(Object value) {
        List<String> items = new ArrayList<>();
        if (value == null) {
            return items;
        }
        if (value instanceof List<?> list) {
            for (Object one : list) {
                if (one == null) {
                    continue;
                }
                if (one instanceof Map<?, ?> nested) {
                    // 形如 {"item": "..."} 的对象，取第一个值
                    for (Object inner : nested.values()) {
                        if (inner != null) {
                            items.add(String.valueOf(inner).trim());
                            break;
                        }
                    }
                } else {
                    String text = String.valueOf(one).trim();
                    if (!text.isEmpty()) {
                        items.add(text);
                    }
                }
            }
            return items;
        }
        // 模型有时把数组写成整段换行文本
        for (String line : String.valueOf(value).split("[\\n\\r]+")) {
            String cleaned = line.trim()
                    .replaceAll("^[\\d一二三四五六七八九十]+[\\.、)、]\\s*", "")
                    .replaceAll("^[-*•]\\s*", "")
                    .trim();
            if (!cleaned.isEmpty()) {
                items.add(cleaned);
            }
        }
        return items;
    }

    /**
     * 在响应文本中定位首个成对的 JSON 片段。
     *
     * <p>不使用正则：模型可能输出嵌套结构或代码块标记，按括号配对扫描更可靠。</p>
     */
    private static JsonNode locate(String raw, char open, char close) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.replace("```json", "").replace("```", "");
        int start = text.indexOf(open);
        if (start < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    String candidate = text.substring(start, i + 1);
                    try {
                        return MAPPER.readTree(candidate);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
