package org.example.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.WrongQuestionMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.WrongQuestion;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/wrong")
public class WrongQuestionController {

    private static final String STATUS_PENDING_MANUAL = "pending_manual";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_MASTERED = "mastered";

    private static final String CN_SINGLE = "\u5355\u9009";
    private static final String CN_MULTI = "\u591a\u9009";
    private static final String CN_JUDGE = "\u5224\u65ad";

    @Resource
    private WrongQuestionMapper wrongQuestionMapper;

    @Logs("WrongQuestion List")
    @Exclude(type = "page", value = {"createBy", "updateBy", "updateTime", "del"})
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Long userId = getCurrentUserIdSafe();
        Integer reqCurrentPage = ReqUtils.getCurrentPage(req);
        Integer reqPageSize = ReqUtils.getPageSize(req);
        int currentPage = reqCurrentPage == null || reqCurrentPage <= 0 ? 1 : reqCurrentPage;
        int pageSize = reqPageSize == null || reqPageSize <= 0 ? 10 : reqPageSize;

        PageVo answer = new PageVo(currentPage, pageSize);
        if (userId == null) {
            answer.setTotal(0L);
            answer.setData(Collections.emptyList());
            return Result.success(answer);
        }

        Page<WrongQuestion> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<WrongQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WrongQuestion::getUserId, userId);

        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(WrongQuestion::getQuestion, keyword)
                    .or()
                    .like(WrongQuestion::getPaperName, keyword)
                    .or()
                    .like(WrongQuestion::getKnowledgePoint, keyword));
        }

        String status = ReqUtils.getStatus(req);
        if (StrUtil.isNotBlank(status)) {
            queryWrapper.eq(WrongQuestion::getStatus, status);
        }

        String category = ReqUtils.getCategory(req);
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq(WrongQuestion::getCategory, category);
        }

        String type = ReqUtils.getType(req);
        if (StrUtil.isNotBlank(type)) {
            queryWrapper.eq(WrongQuestion::getType, type);
        }

        queryWrapper.orderByDesc(WrongQuestion::getLastWrongTime);
        queryWrapper.orderByDesc(WrongQuestion::getCreateTime);

        wrongQuestionMapper.selectPage(page, queryWrapper);

        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    @Logs("WrongQuestion Objective Practice")
    @PostMapping("/practice/objective")
    public Result<JSONObject> practiceObjective(@RequestBody JSONObject req) {
        Long userId = getCurrentUserIdSafe();
        JSONObject resp = new JSONObject();
        if (userId == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_login");
            return Result.success(resp);
        }

        Long id = req.getLong("id");
        String userAnswer = req.getStr("answer");
        if (id == null || StrUtil.isBlank(userAnswer)) {
            resp.putOpt("success", false);
            resp.putOpt("message", "invalid_param");
            return Result.success(resp);
        }

        WrongQuestion wrong = wrongQuestionMapper.selectById(id);
        if (wrong == null || !userId.equals(wrong.getUserId())) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_found");
            return Result.success(resp);
        }

        if (!isObjectiveCategory(wrong.getCategory())) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_objective");
            return Result.success(resp);
        }

        String normalizedAnswer = normalizeAnswer(userAnswer, wrong.getCategory());
        String correctAnswer = normalizeAnswer(wrong.getCorrectAnswer(), wrong.getCategory());
        boolean correct = isCorrectAnswer(wrong.getCategory(), normalizedAnswer, correctAnswer);

        wrong.setLastPracticeTime(new java.util.Date());
        wrong.setLastUserAnswer(normalizedAnswer);

        if (correct) {
            int streak = safeInt(wrong.getCorrectStreak()) + 1;
            wrong.setCorrectStreak(streak);
            wrong.setStatus(streak >= 2 ? STATUS_MASTERED : STATUS_ACTIVE);
        } else {
            wrong.setWrongCount(safeInt(wrong.getWrongCount()) + 1);
            wrong.setCorrectStreak(0);
            wrong.setStatus(STATUS_ACTIVE);
            wrong.setLastWrongTime(new java.util.Date());
        }

        wrongQuestionMapper.updateById(wrong);

        resp.putOpt("success", true);
        resp.putOpt("correct", correct);
        resp.putOpt("correctAnswer", wrong.getCorrectAnswer());
        resp.putOpt("parse", wrong.getParse());
        resp.putOpt("status", wrong.getStatus());
        resp.putOpt("correctStreak", safeInt(wrong.getCorrectStreak()));
        resp.putOpt("wrongCount", safeInt(wrong.getWrongCount()));
        return Result.success(resp);
    }

    @Logs("WrongQuestion Subjective Practice")
    @PostMapping("/practice/subjective")
    public Result<JSONObject> practiceSubjective(@RequestBody JSONObject req) {
        Long userId = getCurrentUserIdSafe();
        JSONObject resp = new JSONObject();
        if (userId == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_login");
            return Result.success(resp);
        }

        Long id = req.getLong("id");
        Boolean mastered = req.getBool("mastered");
        if (id == null || mastered == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "invalid_param");
            return Result.success(resp);
        }

        WrongQuestion wrong = wrongQuestionMapper.selectById(id);
        if (wrong == null || !userId.equals(wrong.getUserId())) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_found");
            return Result.success(resp);
        }

        String previousStatus = wrong.getStatus();
        wrong.setLastPracticeTime(new java.util.Date());

        if (mastered) {
            int streak = safeInt(wrong.getCorrectStreak()) + 1;
            wrong.setCorrectStreak(streak);
            wrong.setStatus(streak >= 2 ? STATUS_MASTERED : STATUS_ACTIVE);
        } else {
            wrong.setWrongCount(safeInt(wrong.getWrongCount()) + 1);
            wrong.setCorrectStreak(0);
            wrong.setStatus(STATUS_ACTIVE);
            wrong.setLastWrongTime(new java.util.Date());
        }

        if (STATUS_PENDING_MANUAL.equals(previousStatus)) {
            wrong.setStatus(mastered ? STATUS_MASTERED : STATUS_ACTIVE);
        }

        wrongQuestionMapper.updateById(wrong);

        resp.putOpt("success", true);
        resp.putOpt("status", wrong.getStatus());
        resp.putOpt("correctStreak", safeInt(wrong.getCorrectStreak()));
        resp.putOpt("wrongCount", safeInt(wrong.getWrongCount()));
        return Result.success(resp);
    }

    @Logs("WrongQuestion Delete Batch")
    @PostMapping("/delBatch")
    public Result<JSONObject> delBatch(@RequestBody JSONObject req) {
        Long userId = getCurrentUserIdSafe();
        JSONObject resp = new JSONObject();
        if (userId == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_login");
            resp.putOpt("removed", 0);
            return Result.success(resp);
        }

        List<Long> ids = parseIdList(req.get("ids"));
        if (ids.isEmpty()) {
            resp.putOpt("success", false);
            resp.putOpt("message", "invalid_param");
            resp.putOpt("removed", 0);
            return Result.success(resp);
        }

        int removed = wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .in(WrongQuestion::getId, ids));

        resp.putOpt("success", true);
        resp.putOpt("removed", removed);
        return Result.success(resp);
    }

    @Logs("WrongQuestion Delete Mastered")
    @PostMapping("/delMastered")
    public Result<JSONObject> delMastered() {
        Long userId = getCurrentUserIdSafe();
        JSONObject resp = new JSONObject();
        if (userId == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_login");
            resp.putOpt("removed", 0);
            return Result.success(resp);
        }

        int removed = wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getStatus, STATUS_MASTERED));

        resp.putOpt("success", true);
        resp.putOpt("removed", removed);
        return Result.success(resp);
    }

    private Long getCurrentUserIdSafe() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isCorrectAnswer(String category, String userAnswer, String correctAnswer) {
        if ("judge".equals(normalizeCategory(category))) {
            return normalizeJudgeAnswer(userAnswer).equals(normalizeJudgeAnswer(correctAnswer));
        }
        if (isMultiChoiceCategory(category)) {
            return normalizeOptionAnswer(userAnswer).equals(normalizeOptionAnswer(correctAnswer));
        }
        return normalizeTextAnswer(userAnswer).equalsIgnoreCase(normalizeTextAnswer(correctAnswer));
    }

    private String normalizeAnswer(String answer, String category) {
        if (isMultiChoiceCategory(category)) {
            return normalizeOptionAnswer(answer);
        }
        return normalizeTextAnswer(answer);
    }

    private String normalizeOptionAnswer(String answer) {
        if (StrUtil.isBlank(answer)) {
            return "";
        }

        String normalized = answer.toUpperCase(Locale.ROOT)
                .replace("\uFF0C", ",")
                .replace("\u3001", ",")
                .replace("\uFF1B", ",")
                .replace(";", ",")
                .replace("|", ",")
                .replace("/", ",")
                .replace("\\", ",")
                .replace(" ", "");

        // Compatible with legacy answers like "AB"/"ACD" (without comma).
        // Also supports "A,B" / "A|B" / "A B" / "A、B" and de-duplicates options.
        Set<String> optionSet = new LinkedHashSet<>();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                optionSet.add(String.valueOf(c));
            }
        }
        if (!optionSet.isEmpty()) {
            List<String> valid = new ArrayList<>(optionSet);
            Collections.sort(valid);
            return String.join(",", valid);
        }

        String[] parts = normalized.split(",");
        List<String> valid = new ArrayList<>();
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                valid.add(part.trim());
            }
        }
        Collections.sort(valid);
        return String.join(",", valid);
    }

    private String normalizeJudgeAnswer(String answer) {
        String text = normalizeTextAnswer(answer).toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "1".equals(text)
                || "\u6b63\u786e".equals(text) || "\u5bf9".equals(text) || "\u662f".equals(text)) {
            return "T";
        }
        if ("false".equals(text) || "0".equals(text)
                || "\u9519\u8bef".equals(text) || "\u9519".equals(text) || "\u5426".equals(text)) {
            return "F";
        }
        return text;
    }

    private String normalizeTextAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().replaceAll("\\s+", "");
    }

    private boolean isObjectiveCategory(String category) {
        String normalized = normalizeCategory(category);
        return "single".equals(normalized) || "multiple".equals(normalized) || "judge".equals(normalized);
    }

    private boolean isMultiChoiceCategory(String category) {
        return "multiple".equals(normalizeCategory(category));
    }

    private String normalizeCategory(String category) {
        String value = StrUtil.blankToDefault(category, "").trim().toLowerCase(Locale.ROOT);
        if (value.contains("single") || value.contains(CN_SINGLE) || value.contains("\u5355\u9009\u9898")) {
            return "single";
        }
        if (value.contains("multiple") || value.contains(CN_MULTI) || value.contains("\u591a\u9009\u9898")) {
            return "multiple";
        }
        if (value.contains("judge") || value.contains(CN_JUDGE) || value.contains("\u5224\u65ad\u9898")) {
            return "judge";
        }
        return value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private List<Long> parseIdList(Object rawIds) {
        if (rawIds == null) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>();
        if (rawIds instanceof JSONArray) {
            JSONArray arr = (JSONArray) rawIds;
            for (Object item : arr) {
                Long id = parseLong(item);
                if (id != null) {
                    result.add(id);
                }
            }
            return result;
        }

        if (rawIds instanceof List) {
            for (Object item : (List<?>) rawIds) {
                Long id = parseLong(item);
                if (id != null) {
                    result.add(id);
                }
            }
            return result;
        }

        Long single = parseLong(rawIds);
        if (single != null) {
            result.add(single);
        }
        return result;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
