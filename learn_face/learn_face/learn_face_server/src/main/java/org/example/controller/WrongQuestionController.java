package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.TaskMapper;
import org.example.mapper.WrongQuestionMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Task;
import org.example.pojo.po.WrongQuestion;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wrong")
public class WrongQuestionController {

    private static final String STATUS_PENDING_MANUAL = "pending_manual";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_MASTERED = "mastered";

    private static final String CN_SINGLE = "\u5355\u9009";
    private static final String CN_MULTI = "\u591a\u9009";
    private static final String CN_JUDGE = "\u5224\u65ad";
    private static final String DIFF_EASY = "easy";
    private static final String DIFF_MEDIUM = "medium";
    private static final String DIFF_HARD = "hard";
    private static final String PAPER_SOURCE_TEACHER = "teacher";
    private static final String PAPER_SOURCE_STUDENT = "student";
    private static final String GRADING_MODE_SELF = "self";
    private static final DateTimeFormatter PAPER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private WrongQuestionMapper wrongQuestionMapper;

    @Resource
    private TaskMapper taskMapper;

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

    @Logs("WrongQuestion Weakness Profile")
    @PostMapping("/weaknessProfile")
    public Result<JSONObject> weaknessProfile(@RequestBody(required = false) JSONObject req) {
        Long userId = getCurrentUserIdSafe();
        JSONObject resp = new JSONObject();
        if (userId == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_login");
            return Result.success(resp);
        }

        List<WrongQuestion> wrongQuestions = loadUserWrongQuestions(userId, false);
        resp.putOpt("success", true);
        resp.putOpt("summary", buildWeaknessSummary(wrongQuestions));
        return Result.success(resp);
    }

    @Logs("WrongQuestion Personalized Paper")
    @PostMapping("/generatePaper")
    public Result<JSONObject> generatePaper(@RequestBody(required = false) JSONObject req) {
        Long userId = getCurrentUserIdSafe();
        JSONObject resp = new JSONObject();
        if (userId == null) {
            resp.putOpt("success", false);
            resp.putOpt("message", "not_login");
            return Result.success(resp);
        }

        JSONObject safeReq = req == null ? new JSONObject() : req;
        int questionCount = safeIntRange(safeReq.getInt("questionCount"), 15, 5, 50);
        int durationMinutes = safeIntRange(safeReq.getInt("durationMinutes"), 45, 15, 180);
        boolean includeMastered = Boolean.TRUE.equals(safeReq.getBool("includeMastered"));

        List<WrongQuestion> wrongQuestions = loadUserWrongQuestions(userId, includeMastered);
        if (wrongQuestions.isEmpty()) {
            resp.putOpt("success", false);
            resp.putOpt("message", "no_wrong_question");
            resp.putOpt("summary", buildWeaknessSummary(Collections.emptyList()));
            return Result.success(resp);
        }

        List<Task> allTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .isNotNull(Task::getQuestion)
                .and(wrapper -> wrapper.eq(Task::getPaperSource, PAPER_SOURCE_TEACHER).or().isNull(Task::getPaperSource))
                .orderByDesc(Task::getCreateTime));
        if (allTasks.isEmpty()) {
            resp.putOpt("success", false);
            resp.putOpt("message", "no_task_source");
            return Result.success(resp);
        }

        WeaknessSummary summary = summarizeWeakness(wrongQuestions);
        double easyRatio = safeRatio(safeReq.getDouble("easyRatio"), 0.4D);
        double mediumRatio = safeRatio(safeReq.getDouble("mediumRatio"), 0.4D);
        double hardRatio = safeRatio(safeReq.getDouble("hardRatio"), 0.2D);
        double ratioSum = easyRatio + mediumRatio + hardRatio;
        if (ratioSum <= 0) {
            easyRatio = 0.4D;
            mediumRatio = 0.4D;
            hardRatio = 0.2D;
            ratioSum = 1.0D;
        }
        easyRatio = easyRatio / ratioSum;
        mediumRatio = mediumRatio / ratioSum;
        hardRatio = hardRatio / ratioSum;

        List<Task> selected = selectPersonalizedTasks(allTasks, summary, questionCount, easyRatio, mediumRatio, hardRatio);
        if (selected.isEmpty()) {
            resp.putOpt("success", false);
            resp.putOpt("message", "no_task_selected");
            return Result.success(resp);
        }

        String inputName = safeReq.getStr("paperName");
        String paperName = StrUtil.isNotBlank(inputName)
                ? inputName.trim()
                : "个性化组卷_" + LocalDateTime.now().format(PAPER_TIME_FORMATTER);
        Integer totalScore = selected.stream().mapToInt(task -> safeInt(task.getScore())).sum();

        for (Task source : selected) {
            Task newTask = new Task();
            BeanUtil.copyProperties(source, newTask);
            newTask.setId(null);
            newTask.setName(paperName);
            newTask.setTotalScore(totalScore);
            newTask.setPaperSource(PAPER_SOURCE_STUDENT);
            newTask.setOwnerUserId(userId);
            newTask.setGradingMode(GRADING_MODE_SELF);
            taskMapper.insert(newTask);
        }

        resp.putOpt("success", true);
        resp.putOpt("paperName", paperName);
        resp.putOpt("questionCount", selected.size());
        resp.putOpt("totalScore", totalScore);
        resp.putOpt("durationMinutes", durationMinutes);
        resp.putOpt("summary", buildWeaknessSummary(wrongQuestions));
        resp.putOpt("difficulty", new JSONObject()
                .set(DIFF_EASY, countDifficulty(selected, DIFF_EASY))
                .set(DIFF_MEDIUM, countDifficulty(selected, DIFF_MEDIUM))
                .set(DIFF_HARD, countDifficulty(selected, DIFF_HARD)));
        return Result.success(resp);
    }

    private List<WrongQuestion> loadUserWrongQuestions(Long userId, boolean includeMastered) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime)
                .orderByDesc(WrongQuestion::getCreateTime);
        if (!includeMastered) {
            wrapper.ne(WrongQuestion::getStatus, STATUS_MASTERED);
        }
        return wrongQuestionMapper.selectList(wrapper);
    }

    private JSONObject buildWeaknessSummary(List<WrongQuestion> wrongQuestions) {
        WeaknessSummary summary = summarizeWeakness(wrongQuestions);
        JSONObject json = new JSONObject();
        json.putOpt("totalWrongQuestions", wrongQuestions.size());
        json.putOpt("topKnowledgePoints", summary.topKnowledgePoints);
        json.putOpt("topCategories", summary.topCategories);
        json.putOpt("recommend", new JSONObject()
                .set("questionCount", wrongQuestions.size() >= 20 ? 20 : 12)
                .set("durationMinutes", wrongQuestions.size() >= 20 ? 45 : 30)
                .set("easyRatio", 0.4)
                .set("mediumRatio", 0.4)
                .set("hardRatio", 0.2));
        return json;
    }

    private WeaknessSummary summarizeWeakness(List<WrongQuestion> wrongQuestions) {
        Map<String, StatBucket> knowledgeStats = new HashMap<>();
        Map<String, StatBucket> categoryStats = new HashMap<>();
        for (WrongQuestion item : wrongQuestions) {
            double weight = computeWrongWeight(item);
            String knowledgePoint = normalizeKnowledgePoint(item.getKnowledgePoint());
            String category = normalizeCategoryLabel(item.getCategory());
            updateStat(knowledgeStats, knowledgePoint, weight, safeInt(item.getWrongCount()));
            updateStat(categoryStats, category, weight, safeInt(item.getWrongCount()));
        }

        List<JSONObject> topKnowledge = toTopStatList(knowledgeStats, "knowledgePoint");
        List<JSONObject> topCategory = toTopStatList(categoryStats, "category");
        return new WeaknessSummary(knowledgeStats, categoryStats, topKnowledge, topCategory);
    }

    private void updateStat(Map<String, StatBucket> stats, String key, double weight, int wrongCount) {
        StatBucket bucket = stats.computeIfAbsent(key, k -> new StatBucket());
        bucket.count += 1;
        bucket.wrongCount += Math.max(1, wrongCount);
        bucket.weight += weight;
    }

    private List<JSONObject> toTopStatList(Map<String, StatBucket> stats, String fieldName) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().weight, a.getValue().weight))
                .limit(8)
                .map(entry -> new JSONObject()
                        .set(fieldName, entry.getKey())
                        .set("weight", round2(entry.getValue().weight))
                        .set("count", entry.getValue().count)
                        .set("wrongCount", entry.getValue().wrongCount))
                .collect(Collectors.toList());
    }

    private List<Task> selectPersonalizedTasks(List<Task> sourceTasks,
                                               WeaknessSummary summary,
                                               int targetCount,
                                               double easyRatio,
                                               double mediumRatio,
                                               double hardRatio) {
        Map<String, Task> deduplicated = new HashMap<>();
        for (Task task : sourceTasks) {
            if (task == null || StrUtil.isBlank(task.getQuestion())) {
                continue;
            }
            String key = normalizeQuestionKey(task);
            deduplicated.putIfAbsent(key, task);
        }
        List<TaskScore> scored = deduplicated.values().stream()
                .map(task -> new TaskScore(task, computeTaskScore(task, summary)))
                .sorted(Comparator.comparingDouble(TaskScore::getScore).reversed())
                .collect(Collectors.toList());

        int easyTarget = (int) Math.round(targetCount * easyRatio);
        int mediumTarget = (int) Math.round(targetCount * mediumRatio);
        int hardTarget = Math.max(0, targetCount - easyTarget - mediumTarget);

        List<Task> selected = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();
        pickByDifficulty(scored, selected, selectedIds, DIFF_EASY, easyTarget);
        pickByDifficulty(scored, selected, selectedIds, DIFF_MEDIUM, mediumTarget);
        pickByDifficulty(scored, selected, selectedIds, DIFF_HARD, hardTarget);

        if (selected.size() < targetCount) {
            for (TaskScore score : scored) {
                Task task = score.task;
                if (task.getId() == null || selectedIds.contains(task.getId())) {
                    continue;
                }
                selected.add(task);
                selectedIds.add(task.getId());
                if (selected.size() >= targetCount) {
                    break;
                }
            }
        }
        return selected;
    }

    private void pickByDifficulty(List<TaskScore> scored,
                                  List<Task> selected,
                                  Set<Long> selectedIds,
                                  String difficulty,
                                  int limit) {
        if (limit <= 0) {
            return;
        }
        for (TaskScore item : scored) {
            Task task = item.task;
            if (task.getId() == null || selectedIds.contains(task.getId())) {
                continue;
            }
            if (!Objects.equals(normalizeDifficulty(task.getType()), difficulty)) {
                continue;
            }
            selected.add(task);
            selectedIds.add(task.getId());
            if (countDifficulty(selected, difficulty) >= limit) {
                break;
            }
        }
    }

    private int countDifficulty(List<Task> tasks, String difficulty) {
        int count = 0;
        for (Task task : tasks) {
            if (Objects.equals(normalizeDifficulty(task.getType()), difficulty)) {
                count++;
            }
        }
        return count;
    }

    private double computeTaskScore(Task task, WeaknessSummary summary) {
        double score = 0;
        String knowledgePoint = normalizeKnowledgePoint(task.getKnowledgePoint());
        String category = normalizeCategoryLabel(task.getCategory());

        StatBucket kp = summary.knowledgeStats.get(knowledgePoint);
        if (kp != null) {
            score += kp.weight * 2.0D;
        }
        StatBucket cate = summary.categoryStats.get(category);
        if (cate != null) {
            score += cate.weight;
        }

        String difficulty = normalizeDifficulty(task.getType());
        if (DIFF_MEDIUM.equals(difficulty)) {
            score += 0.8D;
        } else if (DIFF_HARD.equals(difficulty)) {
            score += 0.5D;
        } else {
            score += 0.6D;
        }

        Random random = new Random(task.getId() == null ? 0L : task.getId());
        score += random.nextDouble() * 0.2D;
        return score;
    }

    private double computeWrongWeight(WrongQuestion item) {
        int wrongCount = Math.max(1, safeInt(item.getWrongCount()));
        int streak = Math.max(0, safeInt(item.getCorrectStreak()));
        double statusBoost;
        if (STATUS_ACTIVE.equals(item.getStatus())) {
            statusBoost = 1.25D;
        } else if (STATUS_PENDING_MANUAL.equals(item.getStatus())) {
            statusBoost = 1.10D;
        } else {
            statusBoost = 0.5D;
        }
        double streakPenalty = Math.max(0, 2 - streak) * 0.6D;
        return wrongCount * statusBoost + streakPenalty;
    }

    private String normalizeQuestionKey(Task task) {
        return normalizeText(task.getQuestion()) + "|" + normalizeKnowledgePoint(task.getKnowledgePoint()) + "|" + normalizeCategoryLabel(task.getCategory());
    }

    private String normalizeCategoryLabel(String category) {
        return StrUtil.blankToDefault(category, "未分类").trim();
    }

    private String normalizeKnowledgePoint(String knowledgePoint) {
        return StrUtil.blankToDefault(knowledgePoint, "未标注知识点").trim();
    }

    private String normalizeDifficulty(String type) {
        String value = StrUtil.blankToDefault(type, "").trim().toLowerCase(Locale.ROOT);
        if (value.contains("easy") || value.contains("简单") || value.contains("易")) {
            return DIFF_EASY;
        }
        if (value.contains("hard") || value.contains("困难") || value.contains("难")) {
            return DIFF_HARD;
        }
        return DIFF_MEDIUM;
    }

    private String normalizeText(String text) {
        return StrUtil.blankToDefault(text, "").replaceAll("\\s+", "");
    }

    private int safeIntRange(Integer value, int defaultValue, int min, int max) {
        int base = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, base));
    }

    private double safeRatio(Double value, double defaultValue) {
        if (value == null || value < 0) {
            return defaultValue;
        }
        return value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
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

    private static final class StatBucket {
        private double weight;
        private int count;
        private int wrongCount;
    }

    private static final class TaskScore {
        private final Task task;
        private final double score;

        private TaskScore(Task task, double score) {
            this.task = task;
            this.score = score;
        }

        private double getScore() {
            return score;
        }
    }

    private static final class WeaknessSummary {
        private final Map<String, StatBucket> knowledgeStats;
        private final Map<String, StatBucket> categoryStats;
        private final List<JSONObject> topKnowledgePoints;
        private final List<JSONObject> topCategories;

        private WeaknessSummary(Map<String, StatBucket> knowledgeStats,
                                Map<String, StatBucket> categoryStats,
                                List<JSONObject> topKnowledgePoints,
                                List<JSONObject> topCategories) {
            this.knowledgeStats = knowledgeStats;
            this.categoryStats = categoryStats;
            this.topKnowledgePoints = topKnowledgePoints;
            this.topCategories = topCategories;
        }
    }
}
