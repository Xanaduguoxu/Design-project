package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.ExamMapper;
import org.example.mapper.TaskMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.WrongQuestionMapper;
import org.example.pojo.base.Base;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Exam;
import org.example.pojo.po.Task;
import org.example.pojo.po.User;
import org.example.pojo.po.WrongQuestion;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/exam")
public class ExamController {

    private static final String RESULT_CORRECT = "\u6b63\u786e";
    private static final String RESULT_WRONG = "\u9519\u8bef";
    private static final String RESULT_PARTIAL = "\u90e8\u5206\u6b63\u786e";
    private static final String RESULT_PENDING = "\u5f85\u6279\u6539";

    private static final String CN_SINGLE = "\u5355\u9009";
    private static final String CN_MULTI = "\u591a\u9009";
    private static final String CN_JUDGE = "\u5224\u65ad";
    private static final String CN_ESSAY = "\u7b80\u7b54";

    private static final String WRONG_STATUS_PENDING_MANUAL = "pending_manual";
    private static final String WRONG_STATUS_ACTIVE = "active";
    private static final String WRONG_STATUS_MASTERED = "mastered";
    private static final String PAPER_SOURCE_STUDENT = "student";
    private static final String GRADING_MODE_SELF = "self";

    @Resource
    private ExamMapper examMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private WrongQuestionMapper wrongQuestionMapper;

    @Logs("Exam Add")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Exam exam = new Exam();
        BeanUtil.copyProperties(req, exam);

        List<JSONObject> submittedAnswers = parseAnswerArray(req);
        List<JSONObject> gradedAnswers = new ArrayList<>();
        int autoScore = 0;
        boolean hasManualQuestion = false;

        for (JSONObject submittedAnswer : submittedAnswers) {
            JSONObject graded = buildSubmitAnswer(submittedAnswer);
            gradedAnswers.add(graded);
            autoScore += safeInt(graded.get("obtainedScore"));
            if (Boolean.TRUE.equals(graded.getBool("manualRequired"))) {
                hasManualQuestion = true;
            }
        }

        exam.setAnswer(gradedAnswers);
        if (exam.getTotalScore() == null || exam.getTotalScore() <= 0) {
            exam.setTotalScore(gradedAnswers.stream().mapToInt(item -> safeInt(item.get("score"))).sum());
        }
        exam.setFinScore(hasManualQuestion ? null : autoScore);

        // Defensive fill for cases where auto-fill does not run.
        try {
            String username = SecurityUtils.getUsername();
            if (StrUtil.isBlank(exam.getCreateBy())) {
                exam.setCreateBy(username);
            }
            if (StrUtil.isBlank(exam.getUpdateBy())) {
                exam.setUpdateBy(username);
            }
        } catch (Exception ignore) {
        }

        boolean saved = examMapper.insert(exam) > 0;
        if (saved) {
            syncWrongBookOnSubmit(exam, gradedAnswers);
        }
        return Result.success(saved);
    }

    @Logs("Exam Delete")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(examMapper.deleteById(id) > 0);
    }

    @Logs("Exam Update")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Long examId = req.getLong("id");
        if (examId == null) {
            return Result.fail(false);
        }

        Exam dbExam = examMapper.selectById(examId);
        if (dbExam == null) {
            return Result.fail(false);
        }

        List<JSONObject> incomingAnswers = parseAnswerArray(req);
        List<JSONObject> mergedAnswers = new ArrayList<>();
        int finalScore = 0;
        boolean hasUngradedManual = false;

        for (JSONObject storedAnswer : safeAnswerList(dbExam.getAnswer())) {
            JSONObject incoming = findIncomingById(incomingAnswers, storedAnswer.get("id"));
            JSONObject merged = mergeTeacherScore(storedAnswer, incoming);
            mergedAnswers.add(merged);

            finalScore += safeInt(merged.get("obtainedScore"));
            if (Boolean.TRUE.equals(merged.getBool("manualRequired")) && !isManualQuestionGraded(merged)) {
                hasUngradedManual = true;
            }
        }

        dbExam.setAnswer(mergedAnswers);
        dbExam.setFinScore(hasUngradedManual ? null : finalScore);

        boolean saved = examMapper.updateById(dbExam) > 0;
        if (saved) {
            syncWrongBookOnManualReviewed(dbExam, mergedAnswers);
        }
        return Result.success(saved);
    }

    @Logs("Exam List")
    @Exclude(type = "page", value = {"createTime", "updateBy", "updateTime", "del"})
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Integer reqCurrentPage = ReqUtils.getCurrentPage(req);
        Integer reqPageSize = ReqUtils.getPageSize(req);
        int currentPage = reqCurrentPage == null || reqCurrentPage <= 0 ? 1 : reqCurrentPage;
        int pageSize = reqPageSize == null || reqPageSize <= 0 ? 10 : reqPageSize;

        Page<Exam> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(Exam::getName, keyword));
        }
        queryWrapper.orderByDesc(Exam::getCreateTime);

        String role = null;
        String username = null;
        try {
            role = SecurityUtils.getRole();
            username = SecurityUtils.getUsername();
        } catch (Exception ignore) {
        }

        boolean isRoot = "root".equals(role);
        if (!isRoot) {
            Set<String> createByCandidates = resolveCreateByCandidates(username);
            if (createByCandidates.isEmpty()) {
                queryWrapper.eq(Base::getCreateBy, "__NO_AUTH_USER__");
            } else {
                queryWrapper.in(Base::getCreateBy, createByCandidates);
            }
        }

        examMapper.selectPage(page, queryWrapper);

        if (!isRoot) {
            for (Exam exam : page.getRecords()) {
                boolean published = isPublished(exam);
                if (!published) {
                    exam.setFinScore(null);
                }
                sanitizeAnswerForStudent(exam, published);
            }
        }

        PageVo answer = new PageVo(currentPage, pageSize);
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    private JSONObject buildSubmitAnswer(JSONObject submittedAnswer) {
        JSONObject graded = new JSONObject();

        Long taskId = parseLong(submittedAnswer.get("id"));
        Task task = taskId == null ? null : taskMapper.selectById(taskId);

        String category = task != null && StrUtil.isNotBlank(task.getCategory())
                ? task.getCategory()
                : submittedAnswer.getStr("category");
        int score = safeInt(submittedAnswer.get("score"));
        if (score <= 0 && task != null && task.getScore() != null) {
            score = task.getScore();
        }

        String userAnswer = normalizeAnswer(submittedAnswer.getStr("answer"), category);
        String correctAnswer = normalizeAnswer(task == null ? "" : task.getAnswer(), category);

        graded.putOpt("id", submittedAnswer.get("id"));
        graded.putOpt("question", task != null && StrUtil.isNotBlank(task.getQuestion())
                ? task.getQuestion()
                : submittedAnswer.getStr("question"));
        graded.putOpt("category", category);
        graded.putOpt("score", score);
        graded.putOpt("answer", userAnswer);
        graded.putOpt("correctAnswer", correctAnswer);

        boolean objectiveQuestion = isObjectiveCategory(category);
        boolean selfGrading = isSelfGradingTask(task);
        graded.putOpt("manualRequired", !objectiveQuestion && !selfGrading);
        graded.putOpt("paperSource", task == null ? null : task.getPaperSource());
        graded.putOpt("gradingMode", task == null ? null : task.getGradingMode());

        if (objectiveQuestion) {
            boolean correct = isCorrectAnswer(category, userAnswer, correctAnswer);
            graded.putOpt("result", correct ? RESULT_CORRECT : RESULT_WRONG);
            graded.putOpt("obtainedScore", correct ? score : 0);
            graded.putOpt("autoGraded", true);
            graded.putOpt("manualGraded", true);
            graded.putOpt("teacherScore", null);
        } else if (selfGrading) {
            Integer selfScore = resolveSelfScore(submittedAnswer, score);
            int finalScore = selfScore == null ? 0 : selfScore;
            graded.putOpt("result", resolveSubjectiveResult(finalScore, score));
            graded.putOpt("obtainedScore", finalScore);
            graded.putOpt("autoGraded", false);
            graded.putOpt("manualGraded", true);
            graded.putOpt("teacherScore", null);
            graded.putOpt("selfScore", finalScore);
        } else {
            graded.putOpt("result", RESULT_PENDING);
            graded.putOpt("obtainedScore", 0);
            graded.putOpt("autoGraded", false);
            graded.putOpt("manualGraded", false);
            graded.putOpt("teacherScore", null);
        }

        return graded;
    }

    private JSONObject mergeTeacherScore(JSONObject storedAnswer, JSONObject incomingAnswer) {
        JSONObject merged = new JSONObject(storedAnswer);

        String category = merged.getStr("category");
        int fullScore = safeInt(merged.get("score"));
        Task task = resolveTaskByAnswerId(merged);
        if (fullScore <= 0 && task != null && task.getScore() != null) {
            fullScore = task.getScore();
            merged.putOpt("score", fullScore);
        }

        String correctAnswer = normalizeAnswer(merged.getStr("correctAnswer"), category);
        if (StrUtil.isBlank(correctAnswer) && task != null) {
            correctAnswer = normalizeAnswer(task.getAnswer(), category);
            merged.putOpt("correctAnswer", correctAnswer);
        }

        boolean manualRequired = getBoolDefault(merged, "manualRequired", !isObjectiveCategory(category));
        merged.putOpt("manualRequired", manualRequired);

        if (!manualRequired) {
            String userAnswer = normalizeAnswer(merged.getStr("answer"), category);
            int objectiveScore;
            String result;

            if (StrUtil.isBlank(correctAnswer)) {
                objectiveScore = safeInt(merged.get("obtainedScore"));
                result = normalizeResultText(merged.getStr("result"));
                if (RESULT_CORRECT.equals(result) && objectiveScore <= 0) {
                    objectiveScore = fullScore;
                }
                if (StrUtil.isBlank(result)) {
                    result = objectiveScore > 0 ? RESULT_CORRECT : RESULT_WRONG;
                }
            } else {
                boolean correct = isCorrectAnswer(category, userAnswer, correctAnswer);
                objectiveScore = correct ? fullScore : 0;
                result = correct ? RESULT_CORRECT : RESULT_WRONG;
            }

            merged.putOpt("result", result);
            merged.putOpt("obtainedScore", objectiveScore);
            merged.putOpt("manualGraded", true);
            merged.putOpt("autoGraded", true);
            merged.putOpt("teacherScore", null);
            return merged;
        }

        Integer manualScore = resolveTeacherScore(incomingAnswer, fullScore);
        if (manualScore == null) {
            // Preserve existing graded score if frontend did not resend it.
            manualScore = resolveTeacherScore(merged, fullScore);
        }

        if (manualScore == null) {
            merged.putOpt("manualGraded", false);
            merged.putOpt("result", RESULT_PENDING);
            merged.putOpt("obtainedScore", 0);
            merged.putOpt("teacherScore", null);
            return merged;
        }

        merged.putOpt("teacherScore", manualScore);
        merged.putOpt("obtainedScore", manualScore);
        merged.putOpt("manualGraded", true);
        merged.putOpt("autoGraded", false);
        merged.putOpt("result", resolveSubjectiveResult(manualScore, fullScore));
        return merged;
    }

    private void sanitizeAnswerForStudent(Exam exam, boolean published) {
        List<JSONObject> answers = safeAnswerList(exam.getAnswer());
        for (JSONObject item : answers) {
            boolean manualRequired = getBoolDefault(item, "manualRequired", !isObjectiveCategory(item.getStr("category")));
            item.putOpt("manualRequired", manualRequired);

            if (manualRequired && !isManualQuestionGraded(item)) {
                item.putOpt("result", RESULT_PENDING);
                item.putOpt("obtainedScore", 0);
            }

            if (!published) {
                item.remove("correctAnswer");
            }
        }
        exam.setAnswer(answers);
    }

    private boolean isPublished(Exam exam) {
        List<JSONObject> answers = safeAnswerList(exam.getAnswer());
        boolean hasManualQuestion = false;

        for (JSONObject item : answers) {
            boolean manualRequired = getBoolDefault(item, "manualRequired", !isObjectiveCategory(item.getStr("category")));
            if (manualRequired) {
                hasManualQuestion = true;
                if (!isManualQuestionGraded(item)) {
                    return false;
                }
            }
        }

        if (!hasManualQuestion) {
            return exam.getFinScore() != null;
        }
        return exam.getFinScore() != null;
    }

    private boolean isManualQuestionGraded(JSONObject answer) {
        Boolean manualGraded = answer.getBool("manualGraded");
        if (manualGraded != null) {
            return manualGraded;
        }

        if (parseNullableInt(answer.get("teacherScore")) != null) {
            return true;
        }

        String result = normalizeResultText(answer.getStr("result"));
        return StrUtil.isNotBlank(result) && !RESULT_PENDING.equals(result);
    }

    private Integer resolveTeacherScore(JSONObject answer, int fullScore) {
        if (answer == null) {
            return null;
        }
        Object scoreValue = answer.get("teacherScore");
        if (scoreValue == null) {
            scoreValue = answer.get("manualScore");
        }
        if (scoreValue == null) {
            scoreValue = answer.get("obtainedScore");
        }

        Integer parsed = parseNullableInt(scoreValue);
        if (parsed == null) {
            return null;
        }

        int max = Math.max(fullScore, 0);
        return Math.max(0, Math.min(max, parsed));
    }

    private Integer resolveSelfScore(JSONObject answer, int fullScore) {
        if (answer == null) {
            return null;
        }
        Object scoreValue = answer.get("selfScore");
        if (scoreValue == null) {
            scoreValue = answer.get("manualScore");
        }
        if (scoreValue == null) {
            scoreValue = answer.get("obtainedScore");
        }

        Integer parsed = parseNullableInt(scoreValue);
        if (parsed == null) {
            return null;
        }
        int max = Math.max(fullScore, 0);
        return Math.max(0, Math.min(max, parsed));
    }

    private boolean isSelfGradingTask(Task task) {
        if (task == null) {
            return false;
        }
        String mode = StrUtil.blankToDefault(task.getGradingMode(), "").trim().toLowerCase(Locale.ROOT);
        if (GRADING_MODE_SELF.equals(mode)) {
            return true;
        }
        String source = StrUtil.blankToDefault(task.getPaperSource(), "").trim().toLowerCase(Locale.ROOT);
        return PAPER_SOURCE_STUDENT.equals(source);
    }

    private String resolveSubjectiveResult(int score, int fullScore) {
        if (score >= fullScore) {
            return RESULT_CORRECT;
        }
        if (score <= 0) {
            return RESULT_WRONG;
        }
        return RESULT_PARTIAL;
    }

    private JSONObject findIncomingById(List<JSONObject> incomingAnswers, Object idValue) {
        String targetId = normalizeId(idValue);
        for (JSONObject incoming : incomingAnswers) {
            if (Objects.equals(targetId, normalizeId(incoming.get("id")))) {
                return incoming;
            }
        }
        return null;
    }

    private String normalizeId(Object idValue) {
        return idValue == null ? "" : String.valueOf(idValue);
    }

    private List<JSONObject> parseAnswerArray(JSONObject req) {
        JSONArray answerArray = req.getJSONArray("answer");
        List<JSONObject> answers = new ArrayList<>();
        if (answerArray == null) {
            return answers;
        }

        for (Object item : answerArray) {
            if (item instanceof JSONObject) {
                answers.add((JSONObject) item);
            } else {
                answers.add(new JSONObject(item));
            }
        }
        return answers;
    }

    private List<JSONObject> safeAnswerList(List<JSONObject> rawAnswers) {
        if (rawAnswers == null) {
            return new ArrayList<>();
        }

        List<JSONObject> list = new ArrayList<>();
        for (Object item : rawAnswers) {
            if (item instanceof JSONObject) {
                list.add((JSONObject) item);
            } else {
                list.add(new JSONObject(item));
            }
        }
        return list;
    }

    private Task resolveTaskByAnswerId(JSONObject answer) {
        Long taskId = parseLong(answer.get("id"));
        if (taskId == null) {
            return null;
        }
        return taskMapper.selectById(taskId);
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

    private String normalizeResultText(String result) {
        if (StrUtil.isBlank(result)) {
            return "";
        }
        String text = result.trim();
        if (text.contains("\u5f85") || text.toLowerCase(Locale.ROOT).contains("pending")) {
            return RESULT_PENDING;
        }
        if (text.contains("\u6b63\u786e") || text.toLowerCase(Locale.ROOT).contains("correct")) {
            return RESULT_CORRECT;
        }
        if (text.contains("\u90e8\u5206")) {
            return RESULT_PARTIAL;
        }
        if (text.contains("\u9519\u8bef") || text.toLowerCase(Locale.ROOT).contains("wrong")) {
            return RESULT_WRONG;
        }
        return text;
    }

    private boolean isObjectiveCategory(String category) {
        String normalized = normalizeCategory(category);
        return "single".equals(normalized) || "multiple".equals(normalized) || "judge".equals(normalized);
    }

    private boolean isMultiChoiceCategory(String category) {
        return "multiple".equals(normalizeCategory(category));
    }

    private String normalizeCategory(String category) {
        String raw = StrUtil.blankToDefault(category, "").trim();
        String value = raw.toLowerCase(Locale.ROOT);
        if (containsAny(raw, value, "single", CN_SINGLE, "\u5355\u9009\u9898", "\u5355\u9009", "鍗曢€夐", "鍗曢€") ) {
            return "single";
        }
        if (containsAny(raw, value, "multiple", CN_MULTI, "\u591a\u9009\u9898", "\u591a\u9009", "澶氶€夐", "澶氶€")) {
            return "multiple";
        }
        if (containsAny(raw, value, "judge", CN_JUDGE, "\u5224\u65ad\u9898", "\u5224\u65ad", "鍒ゆ柇棰", "鍒ゆ柇")) {
            return "judge";
        }
        if (containsAny(raw, value, "essay", CN_ESSAY, "\u7b80\u7b54\u9898", "\u7b80\u7b54", "绠€绛旈", "绠€绛")) {
            return "essay";
        }
        return value;
    }

    private boolean containsAny(String raw, String lower, String... keys) {
        for (String key : keys) {
            if (StrUtil.isBlank(key)) {
                continue;
            }
            if (lower.contains(key.toLowerCase(Locale.ROOT)) || raw.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> resolveCreateByCandidates(String usernameFromToken) {
        Set<String> candidates = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(usernameFromToken)) {
            candidates.add(usernameFromToken.trim());
        }

        try {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                candidates.add(String.valueOf(userId));
                User user = userMapper.selectById(userId);
                if (user != null) {
                    if (StrUtil.isNotBlank(user.getUsername())) {
                        candidates.add(user.getUsername().trim());
                    }
                    if (StrUtil.isNotBlank(user.getEmail())) {
                        candidates.add(user.getEmail().trim());
                    }
                }
            }
        } catch (Exception ignore) {
        }

        return candidates;
    }

    private void syncWrongBookOnSubmit(Exam exam, List<JSONObject> gradedAnswers) {
        Long userId = getCurrentUserIdSafe();
        if (userId == null || gradedAnswers == null || gradedAnswers.isEmpty()) {
            return;
        }

        for (JSONObject answer : gradedAnswers) {
            if (answer == null) {
                continue;
            }
            boolean manualRequired = getBoolDefault(answer, "manualRequired", !isObjectiveCategory(answer.getStr("category")));
            Task task = resolveTaskByAnswerId(answer);
            if (manualRequired) {
                upsertManualPendingWrongQuestion(userId, exam, task, answer);
                continue;
            }

            int score = safeInt(answer.get("score"));
            int obtained = safeInt(answer.get("obtainedScore"));
            if (obtained < score) {
                upsertObjectiveWrongQuestion(userId, exam, task, answer);
            }
        }
    }

    private void syncWrongBookOnManualReviewed(Exam exam, List<JSONObject> mergedAnswers) {
        Long userId = getCurrentUserIdSafe();
        if (userId == null || mergedAnswers == null || mergedAnswers.isEmpty()) {
            return;
        }

        for (JSONObject answer : mergedAnswers) {
            if (answer == null) {
                continue;
            }

            boolean manualRequired = getBoolDefault(answer, "manualRequired", !isObjectiveCategory(answer.getStr("category")));
            if (!manualRequired || !isManualQuestionGraded(answer)) {
                continue;
            }

            Task task = resolveTaskByAnswerId(answer);
            int fullScore = safeInt(answer.get("score"));
            int obtained = safeInt(answer.get("obtainedScore"));
            if (obtained < fullScore) {
                upsertReviewedManualWrongQuestion(userId, exam, task, answer);
            } else {
                handleReviewedManualCorrect(userId, exam, task, answer);
            }
        }
    }

    private void upsertObjectiveWrongQuestion(Long userId, Exam exam, Task task, JSONObject answer) {
        Long questionId = parseLong(answer.get("id"));
        if (questionId == null) {
            return;
        }

        WrongQuestion wrongQuestion = findWrongQuestion(userId, questionId);
        Date now = new Date();
        if (wrongQuestion == null) {
            wrongQuestion = new WrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setWrongCount(1);
            wrongQuestion.setCorrectStreak(0);
            wrongQuestion.setStatus(WRONG_STATUS_ACTIVE);
            wrongQuestion.setFirstWrongTime(now);
            wrongQuestion.setLastWrongTime(now);
        } else {
            wrongQuestion.setWrongCount(safeInt(wrongQuestion.getWrongCount()) + 1);
            wrongQuestion.setCorrectStreak(0);
            wrongQuestion.setStatus(WRONG_STATUS_ACTIVE);
            wrongQuestion.setLastWrongTime(now);
        }

        fillWrongQuestionSnapshot(wrongQuestion, exam, task, answer);
        wrongQuestion.setLastUserAnswer(StrUtil.blankToDefault(answer.getStr("answer"), ""));
        saveWrongQuestion(wrongQuestion);
    }

    private void upsertManualPendingWrongQuestion(Long userId, Exam exam, Task task, JSONObject answer) {
        Long questionId = parseLong(answer.get("id"));
        if (questionId == null) {
            return;
        }

        WrongQuestion wrongQuestion = findWrongQuestion(userId, questionId);
        Date now = new Date();
        if (wrongQuestion == null) {
            wrongQuestion = new WrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setWrongCount(0);
            wrongQuestion.setCorrectStreak(0);
            wrongQuestion.setStatus(WRONG_STATUS_PENDING_MANUAL);
            wrongQuestion.setFirstWrongTime(now);
            wrongQuestion.setLastWrongTime(now);
        }

        fillWrongQuestionSnapshot(wrongQuestion, exam, task, answer);
        wrongQuestion.setLastUserAnswer(StrUtil.blankToDefault(answer.getStr("answer"), ""));
        saveWrongQuestion(wrongQuestion);
    }

    private void upsertReviewedManualWrongQuestion(Long userId, Exam exam, Task task, JSONObject answer) {
        Long questionId = parseLong(answer.get("id"));
        if (questionId == null) {
            return;
        }

        WrongQuestion wrongQuestion = findWrongQuestion(userId, questionId);
        Date now = new Date();
        if (wrongQuestion == null) {
            wrongQuestion = new WrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setWrongCount(1);
            wrongQuestion.setCorrectStreak(0);
            wrongQuestion.setStatus(WRONG_STATUS_ACTIVE);
            wrongQuestion.setFirstWrongTime(now);
            wrongQuestion.setLastWrongTime(now);
        } else {
            int currentWrongCount = safeInt(wrongQuestion.getWrongCount());
            if (WRONG_STATUS_PENDING_MANUAL.equals(wrongQuestion.getStatus()) && currentWrongCount <= 0) {
                wrongQuestion.setWrongCount(1);
            } else {
                wrongQuestion.setWrongCount(currentWrongCount + 1);
            }
            wrongQuestion.setCorrectStreak(0);
            wrongQuestion.setStatus(WRONG_STATUS_ACTIVE);
            wrongQuestion.setLastWrongTime(now);
        }

        fillWrongQuestionSnapshot(wrongQuestion, exam, task, answer);
        wrongQuestion.setLastUserAnswer(StrUtil.blankToDefault(answer.getStr("answer"), ""));
        saveWrongQuestion(wrongQuestion);
    }

    private void handleReviewedManualCorrect(Long userId, Exam exam, Task task, JSONObject answer) {
        Long questionId = parseLong(answer.get("id"));
        if (questionId == null) {
            return;
        }

        WrongQuestion wrongQuestion = findWrongQuestion(userId, questionId);
        if (wrongQuestion == null) {
            return;
        }

        int currentWrongCount = safeInt(wrongQuestion.getWrongCount());
        if (WRONG_STATUS_PENDING_MANUAL.equals(wrongQuestion.getStatus()) && currentWrongCount <= 0 && wrongQuestion.getId() != null) {
            wrongQuestionMapper.deleteById(wrongQuestion.getId());
            return;
        }

        if (WRONG_STATUS_PENDING_MANUAL.equals(wrongQuestion.getStatus())) {
            wrongQuestion.setStatus(currentWrongCount > 0 ? WRONG_STATUS_ACTIVE : WRONG_STATUS_MASTERED);
        }
        fillWrongQuestionSnapshot(wrongQuestion, exam, task, answer);
        wrongQuestion.setLastUserAnswer(StrUtil.blankToDefault(answer.getStr("answer"), ""));
        saveWrongQuestion(wrongQuestion);
    }

    private WrongQuestion findWrongQuestion(Long userId, Long questionId) {
        if (userId == null || questionId == null) {
            return null;
        }
        return wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId)
                .last("limit 1"));
    }

    private void saveWrongQuestion(WrongQuestion wrongQuestion) {
        if (wrongQuestion == null) {
            return;
        }
        if (wrongQuestion.getId() == null) {
            wrongQuestionMapper.insert(wrongQuestion);
        } else {
            wrongQuestionMapper.updateById(wrongQuestion);
        }
    }

    private void fillWrongQuestionSnapshot(WrongQuestion wrongQuestion, Exam exam, Task task, JSONObject answer) {
        wrongQuestion.setExamId(exam == null ? null : exam.getId());
        wrongQuestion.setPaperName(exam == null ? null : exam.getName());

        String category = StrUtil.blankToDefault(answer.getStr("category"), task == null ? "" : task.getCategory());
        wrongQuestion.setQuestion(StrUtil.blankToDefault(answer.getStr("question"), task == null ? "" : task.getQuestion()));
        wrongQuestion.setCategory(category);
        wrongQuestion.setScore(safeInt(answer.get("score")));
        wrongQuestion.setCorrectAnswer(StrUtil.blankToDefault(answer.getStr("correctAnswer"), task == null ? "" : task.getAnswer()));

        if (task != null) {
            wrongQuestion.setChoice(parseChoiceList(task.getChoice()));
            wrongQuestion.setParse(task.getParse());
            wrongQuestion.setType(task.getType());
            wrongQuestion.setKnowledgePoint(task.getKnowledgePoint());
            if (wrongQuestion.getScore() == null || wrongQuestion.getScore() <= 0) {
                wrongQuestion.setScore(task.getScore());
            }
        }
    }

    private List<String> parseChoiceList(String rawChoice) {
        if (StrUtil.isBlank(rawChoice)) {
            return new ArrayList<>();
        }

        String text = rawChoice.trim();
        if (text.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JSONArray jsonArray = JSONUtil.parseArray(text);
            List<String> answer = new ArrayList<>();
            for (Object item : jsonArray) {
                if (item != null) {
                    answer.add(String.valueOf(item));
                }
            }
            if (!answer.isEmpty()) {
                return answer;
            }
        } catch (Exception ignore) {
        }

        return parseLegacyChoice(text);
    }

    private List<String> parseLegacyChoice(String text) {
        String normalized = text;
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        String[] lines = normalized.split("\\r?\\n");
        if (lines.length > 1) {
            for (String line : lines) {
                String item = line.trim();
                if (StrUtil.isNotBlank(item)) {
                    result.add(item);
                }
            }
            return result;
        }

        Matcher matcher = Pattern.compile("(?i)[A-H][\\.\\)]\\s*").matcher(normalized);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.size() > 1) {
            for (int i = 0; i < starts.size(); i++) {
                int start = starts.get(i);
                int end = (i + 1 < starts.size()) ? starts.get(i + 1) : normalized.length();
                String item = normalized.substring(start, end).trim();
                if (StrUtil.isNotBlank(item)) {
                    result.add(item);
                }
            }
            return result;
        }

        String[] parts = normalized.split("\\s*[,;]\\s*");
        for (String part : parts) {
            String item = part.trim();
            if (StrUtil.isNotBlank(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private Long getCurrentUserIdSafe() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean getBoolDefault(JSONObject json, String key, boolean defaultValue) {
        Boolean value = json.getBool(key);
        return value == null ? defaultValue : value;
    }

    private int safeInt(Object value) {
        Integer parsed = parseNullableInt(value);
        return parsed == null ? 0 : parsed;
    }

    private Integer parseNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignore) {
            try {
                return (int) Math.round(Double.parseDouble(text));
            } catch (NumberFormatException ignore2) {
                return null;
            }
        }
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
