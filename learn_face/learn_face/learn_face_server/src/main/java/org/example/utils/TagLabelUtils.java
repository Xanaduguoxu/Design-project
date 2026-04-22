package org.example.utils;

import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class TagLabelUtils {

    private static final Map<String, String> KNOWLEDGE_KEYWORDS = new LinkedHashMap<>();

    static {
        KNOWLEDGE_KEYWORDS.put("javascript,js,es6,闭包,原型,前端", "JavaScript");
        KNOWLEDGE_KEYWORDS.put("vue,vuex,pinia,vite,组件,响应式", "Vue");
        KNOWLEDGE_KEYWORDS.put("java,jdk,jvm,集合,多线程", "Java");
        KNOWLEDGE_KEYWORDS.put("spring,springboot,spring boot,依赖注入,ioc,aop", "Spring");
        KNOWLEDGE_KEYWORDS.put("mysql,sql,索引,事务,数据库,join", "MySQL");
        KNOWLEDGE_KEYWORDS.put("http,https,rest,api,状态码,请求", "HTTP接口");
        KNOWLEDGE_KEYWORDS.put("redis,缓存,ttl", "Redis");
        KNOWLEDGE_KEYWORDS.put("网络,tcp,udp,协议", "计算机网络");
        KNOWLEDGE_KEYWORDS.put("算法,复杂度,排序,查找,二叉树,链表", "数据结构与算法");
        KNOWLEDGE_KEYWORDS.put("操作系统,进程,线程,调度,内存", "操作系统");
    }

    private TagLabelUtils() {
    }

    public static boolean isValidDifficultyTag(String value) {
        return "简单".equals(value) || "中等".equals(value) || "困难".equals(value);
    }

    public static String normalizeOrInferDifficultyTag(String currentTag,
                                                       Integer score,
                                                       String category,
                                                       String question,
                                                       String parse) {
        if (isValidDifficultyTag(currentTag)) {
            return currentTag;
        }
        return inferDifficultyTag(score, category, question, parse);
    }

    public static String inferDifficultyTag(Integer score,
                                            String category,
                                            String question,
                                            String parse) {
        if (score != null) {
            if (score <= 2) {
                return "简单";
            }
            if (score <= 5) {
                return "中等";
            }
            return "困难";
        }

        String normalizedCategory = StrUtil.blankToDefault(category, "").toLowerCase(Locale.ROOT);
        if (normalizedCategory.contains("简答") || normalizedCategory.contains("essay")) {
            return "困难";
        }
        if (normalizedCategory.contains("判断") || normalizedCategory.contains("judge")) {
            return "简单";
        }

        String text = (StrUtil.blankToDefault(question, "") + " " + StrUtil.blankToDefault(parse, "")).toLowerCase(Locale.ROOT);
        if (containsAny(text, "设计", "优化", "架构", "推导", "实现", "综合")) {
            return "困难";
        }
        if (containsAny(text, "分析", "比较", "应用", "流程", "原因", "场景")) {
            return "中等";
        }
        if (containsAny(text, "定义", "概念", "识别", "判断", "基础", "是什么")) {
            return "简单";
        }
        return "中等";
    }

    public static String inferKnowledgePoint(String question,
                                             String parse,
                                             String answer,
                                             String category) {
        String text = (StrUtil.blankToDefault(question, "") + " "
                + StrUtil.blankToDefault(parse, "") + " "
                + StrUtil.blankToDefault(answer, "")).toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : KNOWLEDGE_KEYWORDS.entrySet()) {
            String[] keys = entry.getKey().split(",");
            for (String key : keys) {
                if (StrUtil.isNotBlank(key) && text.contains(key.toLowerCase(Locale.ROOT))) {
                    return entry.getValue();
                }
            }
        }

        String normalizedCategory = StrUtil.blankToDefault(category, "");
        if ("判断题".equals(normalizedCategory)) {
            return "概念辨析";
        }
        if ("单选题".equals(normalizedCategory) || "多选题".equals(normalizedCategory)) {
            return "基础知识";
        }
        if ("简答题".equals(normalizedCategory)) {
            return "综合应用";
        }
        return "综合知识";
    }

    private static boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
